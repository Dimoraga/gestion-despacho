package cl.duoc.transportista.despacho.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "guia_despacho",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_guia_despacho_numero_guia", columnNames = "numero_guia"))
@Getter
@Setter
@NoArgsConstructor
public class GuiaDespacho {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long numeroGuia;

  private String transportista;
  private LocalDate fecha;
  private String destino;
  private String pedido;
  private String archivoKey;
  private String efsPath;
}
