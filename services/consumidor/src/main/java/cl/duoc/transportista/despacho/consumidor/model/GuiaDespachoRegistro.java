package cl.duoc.transportista.despacho.consumidor.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "guia_despacho_registro",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_registro_numero_guia", columnNames = "numero_guia"))
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
  private String efsPath;
  private String payloadHash;
  private Integer versionEvento;
  private boolean eliminada;

  @Enumerated(EnumType.STRING)
  private EstadoProcesamiento estado;

  private Instant fechaInicioProcesamiento;
  private Instant fechaProcesado;
}
