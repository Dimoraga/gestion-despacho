package cl.duoc.transportista.despacho.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.transportista.despacho.dto.SolicitudResponse;
import cl.duoc.transportista.despacho.security.SecurityConfig;
import cl.duoc.transportista.despacho.security.SecurityRoles;
import cl.duoc.transportista.despacho.service.ConsumerUnavailableException;
import cl.duoc.transportista.despacho.service.SolicitudConsumerClient;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SolicitudController.class)
@Import(SecurityConfig.class)
class SolicitudControllerTest {
  private static final String REQUEST_ID = "a14bb491-06bd-4348-a2e4-6d7a8d3f121e";

  @Autowired MockMvc mockMvc;

  @MockitoBean SolicitudConsumerClient solicitudConsumerClient;

  @Test
  void consultar_observacionDisponible_retornaEstadoDelConsumidor() throws Exception {
    when(solicitudConsumerClient.consultar(REQUEST_ID))
        .thenReturn(Optional.of(new SolicitudResponse(REQUEST_ID, 42L, "COMPLETED", null)));

    mockMvc
        .perform(get("/api/solicitudes/{requestId}", REQUEST_ID).with(gestionGuiasJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestId").value(REQUEST_ID))
        .andExpect(jsonPath("$.numeroGuia").value(42))
        .andExpect(jsonPath("$.estado").value("COMPLETED"));

    verify(solicitudConsumerClient).consultar(eq(REQUEST_ID));
  }

  @Test
  void consultar_sinObservacion_retorna404ConReintento() throws Exception {
    when(solicitudConsumerClient.consultar(REQUEST_ID)).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/solicitudes/{requestId}", REQUEST_ID).with(gestionGuiasJwt()))
        .andExpect(status().isNotFound())
        .andExpect(header().string("Retry-After", "1"));
  }

  @Test
  void consultar_errorDeRedInterno_retorna503Controlado() throws Exception {
    when(solicitudConsumerClient.consultar(REQUEST_ID))
        .thenThrow(new ConsumerUnavailableException(new RuntimeException("Connection refused")));

    mockMvc
        .perform(get("/api/solicitudes/{requestId}", REQUEST_ID).with(gestionGuiasJwt()))
        .andExpect(status().isServiceUnavailable())
        .andExpect(header().string("Retry-After", "1"));
  }

  @Test
  void consultar_requiereRolDeGestion() throws Exception {
    mockMvc
        .perform(get("/api/solicitudes/{requestId}", REQUEST_ID))
        .andExpect(status().isUnauthorized());
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor gestionGuiasJwt() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + SecurityRoles.GESTION_GUIAS));
  }
}
