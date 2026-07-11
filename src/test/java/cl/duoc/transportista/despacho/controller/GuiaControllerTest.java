package cl.duoc.transportista.despacho.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cl.duoc.transportista.despacho.dto.GuiaResponse;
import cl.duoc.transportista.despacho.exception.AccesoDenegadoException;
import cl.duoc.transportista.despacho.security.SecurityConfig;
import cl.duoc.transportista.despacho.security.SecurityRoles;
import cl.duoc.transportista.despacho.service.GuiaDespachoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(GuiaController.class)
@Import(SecurityConfig.class)
class GuiaControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean GuiaDespachoService service;

  @Autowired ObjectMapper objectMapper;

  private static RequestPostProcessor gestion() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + SecurityRoles.GESTION_GUIAS));
  }

  private static RequestPostProcessor descarga() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + SecurityRoles.DESCARGA_GUIAS));
  }

  @Test
  void crearGuia_retorna201ConLocation() throws Exception {
    GuiaResponse resp =
        new GuiaResponse(
            1L, "transportistaX", LocalDate.of(2021, 3, 15), "Santiago", "PED-001", null);
    when(service.crear(any())).thenReturn(resp);

    String body =
        """
        {
          "transportista": "transportistaX",
          "fecha": "2021-03-15",
          "destino": "Santiago",
          "pedido": "PED-001"
        }
        """;

    mockMvc
        .perform(
            post("/api/guias")
                .with(gestion())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"));
  }

  @Test
  void crearGuia_sinAutenticacion_retorna401() throws Exception {
    String body =
        """
        {
          "transportista": "transportistaX",
          "fecha": "2021-03-15",
          "destino": "Santiago",
          "pedido": "PED-001"
        }
        """;

    mockMvc
        .perform(post("/api/guias").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void crearGuia_conRolDescarga_retorna403() throws Exception {
    String body =
        """
        {
          "transportista": "transportistaX",
          "fecha": "2021-03-15",
          "destino": "Santiago",
          "pedido": "PED-001"
        }
        """;

    mockMvc
        .perform(
            post("/api/guias")
                .with(
                    jwt()
                        .jwt(j -> j.claim("preferred_username", "transportistaX"))
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_" + SecurityRoles.DESCARGA_GUIAS)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void descargarDeS3_transportistaAutorizado_retorna200() throws Exception {
    when(service.descargarDeS3(eq(1L), eq("transportistaX"))).thenReturn(new byte[] {1});

    mockMvc
        .perform(
            get("/api/guias/1/s3")
                .with(
                    jwt()
                        .jwt(j -> j.claim("preferred_username", "transportistaX"))
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_" + SecurityRoles.DESCARGA_GUIAS)))
                .header("X-Transportista", "transportistaX"))
        .andExpect(status().isOk());
  }

  @Test
  void descargarDeS3_transportistaNoAutorizado_retorna403() throws Exception {
    when(service.descargarDeS3(eq(1L), eq("otro"))).thenThrow(new AccesoDenegadoException("no"));

    mockMvc
        .perform(
            get("/api/guias/1/s3")
                .with(
                    jwt()
                        .jwt(j -> j.claim("preferred_username", "otro"))
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_" + SecurityRoles.DESCARGA_GUIAS)))
                .header("X-Transportista", "otro"))
        .andExpect(status().isForbidden());
  }

  @Test
  void descargarDeS3_conRolGestion_retorna403() throws Exception {
    mockMvc
        .perform(get("/api/guias/1/s3").with(gestion()).header("X-Transportista", "transportistaX"))
        .andExpect(status().isForbidden());
  }

  @Test
  void actualizarGuia_retorna200() throws Exception {
    GuiaResponse resp =
        new GuiaResponse(
            1L, "transportistaX", LocalDate.of(2021, 3, 15), "Valparaiso", "PED-002", null);
    when(service.actualizar(eq(1L), any())).thenReturn(resp);

    String body =
        """
        {
          "transportista": "transportistaX",
          "fecha": "2021-03-15",
          "destino": "Valparaiso",
          "pedido": "PED-002"
        }
        """;

    mockMvc
        .perform(
            put("/api/guias/1")
                .with(gestion())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk());
  }

  @Test
  void eliminarGuia_retorna204() throws Exception {
    mockMvc.perform(delete("/api/guias/1").with(gestion())).andExpect(status().isNoContent());

    verify(service).eliminar(1L);
  }

  @Test
  void historial_retornaListaConUnElemento() throws Exception {
    GuiaResponse resp =
        new GuiaResponse(
            1L, "transportistaX", LocalDate.of(2021, 3, 15), "Santiago", "PED-001", null);
    when(service.historial("transportistaX", LocalDate.of(2021, 3, 15))).thenReturn(List.of(resp));

    mockMvc
        .perform(
            get("/api/guias")
                .with(gestion())
                .param("transportista", "transportistaX")
                .param("fecha", "2021-03-15"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }
}
