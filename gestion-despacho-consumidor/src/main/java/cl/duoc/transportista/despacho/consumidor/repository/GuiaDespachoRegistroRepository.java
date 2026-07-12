package cl.duoc.transportista.despacho.consumidor.repository;

import cl.duoc.transportista.despacho.consumidor.model.GuiaDespachoRegistro;
import java.util.Optional;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuiaDespachoRegistroRepository extends JpaRepository<GuiaDespachoRegistro, Long> {
  Optional<GuiaDespachoRegistro> findByNumeroGuia(Long numeroGuia);
  List<GuiaDespachoRegistro> findByEstadoAndFechaInicioProcesamientoBefore(
      cl.duoc.transportista.despacho.consumidor.model.EstadoProcesamiento estado, Instant limite);
  List<GuiaDespachoRegistro> findByTransportista(String transportista);
  List<GuiaDespachoRegistro> findByFecha(java.time.LocalDate fecha);
  List<GuiaDespachoRegistro> findByTransportistaAndFecha(String transportista, java.time.LocalDate fecha);
}
