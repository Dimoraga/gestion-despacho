package cl.duoc.transportista.despacho.consumidor.repository;

import cl.duoc.transportista.despacho.consumidor.model.GuiaDespachoRegistro;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuiaDespachoRegistroRepository extends JpaRepository<GuiaDespachoRegistro, Long> {
  Optional<GuiaDespachoRegistro> findByNumeroGuia(Long numeroGuia);
}
