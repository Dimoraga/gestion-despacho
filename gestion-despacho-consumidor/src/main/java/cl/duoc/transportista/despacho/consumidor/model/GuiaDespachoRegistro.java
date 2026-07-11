package cl.duoc.transportista.despacho.consumidor.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "guia_despacho_registro", uniqueConstraints = @UniqueConstraint(name = "uk_registro_numero_guia", columnNames = "numero_guia"))
@Getter
@Setter
@NoArgsConstructor
public class GuiaDespachoRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long numeroGuia;
    private String transportista;
    private LocalDate fecha;
    private String destino;
    private String pedido;
    private String archivoKey;
    private Instant fechaProcesado;
}
