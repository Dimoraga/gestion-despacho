package cl.duoc.transportista.despacho.consumidor.service;
import cl.duoc.transportista.despacho.consumidor.dto.GuiaColaMensaje;
import cl.duoc.transportista.despacho.consumidor.model.GuiaDespachoRegistro;
import cl.duoc.transportista.despacho.consumidor.repository.GuiaDespachoRegistroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.Optional;
@Service public class GuiaRegistroPersistenceService {
 private final GuiaDespachoRegistroRepository repo;
 public GuiaRegistroPersistenceService(GuiaDespachoRegistroRepository repo){this.repo=repo;}
 @Transactional(propagation=Propagation.REQUIRES_NEW) public Optional<GuiaDespachoRegistro> buscar(Long numeroGuia){return repo.findByNumeroGuia(numeroGuia);}
 @Transactional(propagation=Propagation.REQUIRES_NEW) public GuiaDespachoRegistro insertar(GuiaColaMensaje m){GuiaDespachoRegistro r=new GuiaDespachoRegistro();r.setNumeroGuia(m.numeroGuia());r.setTransportista(m.transportista());r.setFecha(m.fecha());r.setDestino(m.destino());r.setPedido(m.pedido());r.setArchivoKey(m.archivoKey());r.setFechaProcesado(Instant.now());return repo.saveAndFlush(r);}
}
