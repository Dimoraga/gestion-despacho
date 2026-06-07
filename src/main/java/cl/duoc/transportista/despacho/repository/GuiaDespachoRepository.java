package cl.duoc.transportista.despacho.repository;

import cl.duoc.transportista.despacho.model.GuiaDespacho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface GuiaDespachoRepository extends JpaRepository<GuiaDespacho, Long> {
    List<GuiaDespacho> findByTransportistaAndFecha(String transportista, LocalDate fecha);
    List<GuiaDespacho> findByTransportista(String transportista);
    List<GuiaDespacho> findByFecha(LocalDate fecha);
    boolean existsByPedido(String pedido);
}
