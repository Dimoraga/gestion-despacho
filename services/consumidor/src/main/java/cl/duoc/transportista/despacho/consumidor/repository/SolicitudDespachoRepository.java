package cl.duoc.transportista.despacho.consumidor.repository;

import cl.duoc.transportista.despacho.consumidor.model.SolicitudDespacho;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudDespachoRepository extends JpaRepository<SolicitudDespacho, Long> {
  Optional<SolicitudDespacho> findByRequestId(String requestId);
}
