package cl.duoc.transportista.despacho.consumidor.service;

import cl.duoc.transportista.despacho.consumidor.model.GuiaDespachoRegistro;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import org.springframework.stereotype.Service;

@Service public class GuiaPdfService {
  public byte[] generar(GuiaDespachoRegistro g) { try { var out=new ByteArrayOutputStream(); var doc=new Document(); PdfWriter.getInstance(doc,out); doc.open(); doc.add(new Paragraph("Guia de Despacho")); doc.add(new Paragraph("Numero: " + g.getNumeroGuia())); doc.add(new Paragraph("Transportista: " + g.getTransportista())); doc.add(new Paragraph("Fecha: " + g.getFecha())); doc.add(new Paragraph("Destino: " + g.getDestino())); doc.add(new Paragraph("Pedido: " + g.getPedido())); doc.close(); return out.toByteArray(); } catch(DocumentException e) { throw new IllegalStateException(e); } }
}
