package cl.duoc.transportista.despacho.consumidor.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "solicitud_despacho", uniqueConstraints = @UniqueConstraint(name = "uk_solicitud_request_id", columnNames = "request_id"))
@Getter
@Setter
@NoArgsConstructor
public class SolicitudDespacho {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "solicitud_despacho_seq")
  @SequenceGenerator(name = "solicitud_despacho_seq", sequenceName = "solicitud_despacho_seq", allocationSize = 1)
  private Long id;

  private String requestId;
  private String fingerprint;
  private Long numeroGuia;
  private Instant recibidaEn;
}
