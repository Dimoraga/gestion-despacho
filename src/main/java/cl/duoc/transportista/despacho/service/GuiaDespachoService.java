package cl.duoc.transportista.despacho.service;

import cl.duoc.transportista.despacho.dto.GuiaRequest;
import cl.duoc.transportista.despacho.dto.GuiaResponse;

import java.time.LocalDate;
import java.util.List;

public interface GuiaDespachoService {

    GuiaResponse crear(GuiaRequest request);

    GuiaResponse obtener(Long numeroGuia);

    String subirAS3(Long numeroGuia);

    byte[] descargarDeS3(Long numeroGuia, String solicitante);

    GuiaResponse actualizar(Long numeroGuia, GuiaRequest request);

    List<GuiaResponse> historial(String transportista, LocalDate fecha);

    void eliminar(Long numeroGuia);
}
