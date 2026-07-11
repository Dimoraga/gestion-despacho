package cl.duoc.transportista.despacho.consumidor.service;

import cl.duoc.transportista.despacho.consumidor.dto.GuiaColaMensaje;
import cl.duoc.transportista.despacho.consumidor.model.GuiaDespachoRegistro;
import cl.duoc.transportista.despacho.consumidor.repository.GuiaDespachoRegistroRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuiaRegistroPersistenceService {
  private final GuiaDespachoRegistroRepository repository;

  public GuiaRegistroPersistenceService(GuiaDespachoRegistroRepository repository) {
    this.repository = repository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Optional<GuiaDespachoRegistro> buscarPorNumeroGuia(Long numeroGuia) {
    return repository.findByNumeroGuia(numeroGuia);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public GuiaDespachoRegistro crearRegistro(GuiaColaMensaje mensaje) {
    GuiaDespachoRegistro registro = new GuiaDespachoRegistro();
    registro.setNumeroGuia(mensaje.numeroGuia());
    registro.setTransportista(mensaje.transportista());
    registro.setFecha(mensaje.fecha());
    registro.setDestino(mensaje.destino());
    registro.setPedido(mensaje.pedido());
    registro.setArchivoKey(mensaje.archivoKey());
    registro.setFechaProcesado(Instant.now());
    return repository.saveAndFlush(registro);
  }
}
