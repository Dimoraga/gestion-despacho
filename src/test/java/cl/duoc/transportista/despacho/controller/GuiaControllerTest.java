package cl.duoc.transportista.despacho.controller;

import cl.duoc.transportista.despacho.dto.GuiaResponse;
import cl.duoc.transportista.despacho.exception.AccesoDenegadoException;
import cl.duoc.transportista.despacho.service.GuiaDespachoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GuiaController.class)
class GuiaControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GuiaDespachoService service;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void crearGuia_retorna201ConLocation() throws Exception {
        GuiaResponse resp = new GuiaResponse(1L, "transportistaX", LocalDate.of(2021, 3, 15), "Santiago", "PED-001", null);
        when(service.crear(any())).thenReturn(resp);

        String body = """
                {
                  "transportista": "transportistaX",
                  "fecha": "2021-03-15",
                  "destino": "Santiago",
                  "pedido": "PED-001"
                }
                """;

        mockMvc.perform(post("/api/guias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    void descargarDeS3_transportistaAutorizado_retorna200() throws Exception {
        when(service.descargarDeS3(1L, "transportistaX")).thenReturn(new byte[]{1});

        mockMvc.perform(get("/api/guias/1/s3")
                        .header("X-Transportista", "transportistaX"))
                .andExpect(status().isOk());
    }

    @Test
    void descargarDeS3_transportistaNoAutorizado_retorna403() throws Exception {
        when(service.descargarDeS3(eq(1L), eq("otro"))).thenThrow(new AccesoDenegadoException("no"));

        mockMvc.perform(get("/api/guias/1/s3")
                        .header("X-Transportista", "otro"))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizarGuia_retorna200() throws Exception {
        GuiaResponse resp = new GuiaResponse(1L, "transportistaX", LocalDate.of(2021, 3, 15), "Valparaiso", "PED-002", null);
        when(service.actualizar(eq(1L), any())).thenReturn(resp);

        String body = """
                {
                  "transportista": "transportistaX",
                  "fecha": "2021-03-15",
                  "destino": "Valparaiso",
                  "pedido": "PED-002"
                }
                """;

        mockMvc.perform(put("/api/guias/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void eliminarGuia_retorna204() throws Exception {
        mockMvc.perform(delete("/api/guias/1"))
                .andExpect(status().isNoContent());

        verify(service).eliminar(1L);
    }

    @Test
    void historial_retornaListaConUnElemento() throws Exception {
        GuiaResponse resp = new GuiaResponse(1L, "transportistaX", LocalDate.of(2021, 3, 15), "Santiago", "PED-001", null);
        when(service.historial("transportistaX", LocalDate.of(2021, 3, 15))).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/guias")
                        .param("transportista", "transportistaX")
                        .param("fecha", "2021-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
