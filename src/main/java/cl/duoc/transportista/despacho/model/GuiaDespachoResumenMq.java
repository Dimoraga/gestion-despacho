package cl.duoc.transportista.despacho.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "guia_despacho_resumen_mq")
@Getter
@Setter
@NoArgsConstructor
public class GuiaDespachoResumenMq {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long numeroGuia;
    private String transportista;
    private LocalDate fecha;
    private String destino;
    private String pedido;
    private String archivoKey;
    private LocalDateTime fechaMensaje;
    private LocalDateTime fechaConsumo;
}
