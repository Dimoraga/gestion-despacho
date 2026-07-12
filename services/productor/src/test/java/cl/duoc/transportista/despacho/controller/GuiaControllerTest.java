package cl.duoc.transportista.despacho.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.transportista.despacho.dto.GuiaResponse;
import cl.duoc.transportista.despacho.security.SecurityConfig;
import cl.duoc.transportista.despacho.security.SecurityRoles;
import cl.duoc.transportista.despacho.service.GuiaDespachoService;
import java.time.LocalDate;
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
  void crearGuia_retorna202ConLocation() throws Exception {
    GuiaResponse response =
        new GuiaResponse(
            1L, "transportistaX", LocalDate.of(2021, 3, 15), "Santiago", "PED-001", null);
    when(service.crear(any(), eq(1L))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/guias")
                .with(
                    jwt()
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_" + SecurityRoles.GESTION_GUIAS)))
                .header("Idempotency-Key", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isAccepted())
        .andExpect(header().string("Location", "/api/guias/1"));

    verify(service).crear(any(), eq(1L));
  }

  @Test
  void crearGuia_conIdempotencyKeyInvalida_retorna400() throws Exception {
    mockMvc
        .perform(
            post("/api/guias")
                .with(
                    jwt()
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_" + SecurityRoles.GESTION_GUIAS)))
                .header("Idempotency-Key", "0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isBadRequest());

    verify(service, never()).crear(any(), any());
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
