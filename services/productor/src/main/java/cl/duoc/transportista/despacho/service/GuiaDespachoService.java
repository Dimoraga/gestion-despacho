package cl.duoc.transportista.despacho.service;

import cl.duoc.transportista.despacho.dto.GuiaRequest;
import cl.duoc.transportista.despacho.dto.GuiaResponse;

public interface GuiaDespachoService {

  GuiaResponse crear(GuiaRequest request);
}
