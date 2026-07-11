package cl.duoc.transportista.despacho.service;

import cl.duoc.transportista.despacho.model.GuiaDespacho;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import org.springframework.stereotype.Service;

@Service
public class GuiaPdfService {

  public byte[] generar(GuiaDespacho guia) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Document doc = new Document();
      PdfWriter.getInstance(doc, out);
      doc.open();
      doc.add(new Paragraph("Guia de Despacho"));
      doc.add(new Paragraph("Numero: " + guia.getNumeroGuia()));
      doc.add(new Paragraph("Transportista: " + guia.getTransportista()));
      doc.add(new Paragraph("Fecha: " + guia.getFecha()));
      doc.add(new Paragraph("Destino: " + guia.getDestino()));
      doc.add(new Paragraph("Pedido: " + guia.getPedido()));
      doc.close();
      return out.toByteArray();
    } catch (DocumentException e) {
      throw new RuntimeException(e);
    }
  }
}
