package cl.duoc.transportista.despacho.consumidor.repository;

import cl.duoc.transportista.despacho.consumidor.model.GuiaDespachoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GuiaDespachoRegistroRepository extends JpaRepository<GuiaDespachoRegistro, Long> {
    Optional<GuiaDespachoRegistro> findByNumeroGuia(Long numeroGuia);
}
