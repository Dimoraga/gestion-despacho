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
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_registro_numero_guia", columnNames = "numero_guia"),
      @UniqueConstraint(name = "uk_registro_payload_hash", columnNames = "payload_hash")
    })
@Getter
@Setter
@NoArgsConstructor
public class GuiaDespachoRegistro {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "guia_despacho_registro_seq")
  @SequenceGenerator(name = "guia_despacho_registro_seq", sequenceName = "guia_despacho_registro_seq", allocationSize = 1)
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
  @Column(nullable = false)
  private boolean eliminada;

  @Enumerated(EnumType.STRING)
  private EstadoProcesamiento estado;

  private Instant fechaInicioProcesamiento;
  private Instant fechaProcesado;

  @Enumerated(EnumType.STRING)
  private FaseProcesamiento fase = FaseProcesamiento.PENDING;

  private String leaseToken;
  private Instant leaseExpiraEn;
  private Long fence = 0L;
  private String checksum;
  @Column(length = 2000)
  private String ultimoError;
  private Integer retryCount = 0;
  private Instant retryAt;
}
