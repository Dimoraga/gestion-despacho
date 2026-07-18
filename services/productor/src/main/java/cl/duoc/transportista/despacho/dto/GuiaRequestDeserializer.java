package cl.duoc.transportista.despacho.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Allows forward-compatible fields but explicitly forbids client-controlled identifiers. */
public class GuiaRequestDeserializer extends StdDeserializer<GuiaRequest> {
  private static final List<String> FORBIDDEN_IDENTIFIERS =
      List.of("numeroGuia", "requestId", "fingerprint");

  public GuiaRequestDeserializer() {
    super(GuiaRequest.class);
  }

  @Override
  public GuiaRequest deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    JsonNode node = parser.getCodec().readTree(parser);
    for (String identifier : FORBIDDEN_IDENTIFIERS) {
      if (node.has(identifier)) {
        throw JsonMappingException.from(parser, "El campo " + identifier + " no está permitido");
      }
    }
    return new GuiaRequest(
        text(node, "transportista"),
        fecha(parser, node.get("fecha")),
        text(node, "destino"),
        text(node, "pedido"));
  }

  private String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value == null || value.isNull() ? null : value.asText();
  }

  private LocalDate fecha(JsonParser parser, JsonNode node) throws JsonMappingException {
    if (node == null || node.isNull()) {
      return null;
    }
    try {
      return LocalDate.parse(node.asText());
    } catch (DateTimeParseException ex) {
      throw JsonMappingException.from(parser, "fecha debe tener formato ISO yyyy-MM-dd", ex);
    }
  }
}
