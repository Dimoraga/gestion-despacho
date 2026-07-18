package cl.duoc.transportista.despacho.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.transportista.despacho.dto.GuiaResponse;
import cl.duoc.transportista.despacho.security.SecurityConfig;
import cl.duoc.transportista.despacho.security.SecurityRoles;
import cl.duoc.transportista.despacho.service.GuiaDespachoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GuiaController.class)
@Import(SecurityConfig.class)
class GuiaControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean GuiaDespachoService service;

  private static final String BODY =
      """
      {
        "transportista": "transportistaX",
        "fecha": "2021-03-15",
        "destino": "Santiago",
        "pedido": "PED-001"
      }
      """;

  @Test
  void crearGuia_retorna202ConRequestIdYLocation() throws Exception {
    when(service.crear(any())).thenReturn(new GuiaResponse("dd82d384-9921-4f4f-a1df-6da29c386a18"));

    mockMvc
        .perform(
            post("/api/guias")
                .with(
                    jwt()
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_" + SecurityRoles.GESTION_GUIAS)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isAccepted())
        .andExpect(
            header().string("Location", "/api/solicitudes/dd82d384-9921-4f4f-a1df-6da29c386a18"))
        .andExpect(jsonPath("$.requestId").value("dd82d384-9921-4f4f-a1df-6da29c386a18"))
        .andExpect(jsonPath("$.numeroGuia").doesNotExist());

    verify(service).crear(any());
  }

  @Test
  void crearGuia_postIgualesSinHeader_retornaRequestIdsDistintos() throws Exception {
    when(service.crear(any()))
        .thenReturn(
            new GuiaResponse("dd82d384-9921-4f4f-a1df-6da29c386a18"),
            new GuiaResponse("1d2ca3e1-48c4-4211-b990-e4b6d0d53d5f"));

    mockMvc
        .perform(
            post("/api/guias")
                .with(
                    jwt()
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_" + SecurityRoles.GESTION_GUIAS)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.requestId").value("dd82d384-9921-4f4f-a1df-6da29c386a18"));
    mockMvc
        .perform(
            post("/api/guias")
                .with(
                    jwt()
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_" + SecurityRoles.GESTION_GUIAS)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.requestId").value("1d2ca3e1-48c4-4211-b990-e4b6d0d53d5f"));

    verify(service, org.mockito.Mockito.times(2)).crear(any());
  }

  @Test
  void crearGuia_rechazaIdentificadoresAportadosPorCliente() throws Exception {
    mockMvc
        .perform(
            post("/api/guias")
                .with(
                    jwt()
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_" + SecurityRoles.GESTION_GUIAS)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    BODY.replace(
                        "}",
                        ", \"requestId\": \"cliente\", \"fingerprint\": \"cliente\", \"numeroGuia\": 1}")))
        .andExpect(status().isBadRequest());

    verify(service, never()).crear(any());
  }

  @Test
  void crearGuia_rechazaIdempotencyKeyAportadaPorCliente() throws Exception {
    mockMvc
        .perform(
            post("/api/guias")
                .with(
                    jwt()
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_" + SecurityRoles.GESTION_GUIAS)))
                .header("Idempotency-Key", "cliente")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isBadRequest());

    verify(service, never()).crear(any());
  }

  @Test
  void productorNoExponeRutasDeConsulta() throws Exception {
    mockMvc
        .perform(
            get("/api/guias/1")
                .with(
                    jwt()
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_" + SecurityRoles.GESTION_GUIAS))))
        .andExpect(status().isNotFound());
  }
}
