package cl.duoc.transportista.despacho.service;

import cl.duoc.transportista.despacho.dto.GuiaColaMensaje;
import cl.duoc.transportista.despacho.dto.GuiaRequest;
import cl.duoc.transportista.despacho.dto.GuiaResponse;
import cl.duoc.transportista.despacho.exception.AccesoDenegadoException;
import cl.duoc.transportista.despacho.exception.RecursoNoEncontradoException;
import cl.duoc.transportista.despacho.model.GuiaDespacho;
import cl.duoc.transportista.despacho.repository.GuiaDespachoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class GuiaDespachoServiceImpl implements GuiaDespachoService {

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final GuiaDespachoRepository repo;
    private final GuiaPdfService pdfService;
    private final S3StorageService s3;
    private final EfsStorageService efs;
    private final GuiaQueuePublisher queuePublisher;

    public GuiaDespachoServiceImpl(GuiaDespachoRepository repo, GuiaPdfService pdfService,
                                   S3StorageService s3, EfsStorageService efs, GuiaQueuePublisher queuePublisher) {
        this.repo = repo;
        this.pdfService = pdfService;
        this.s3 = s3;
        this.efs = efs;
        this.queuePublisher = queuePublisher;
    }

    private String buildKey(GuiaDespacho g) {
        return g.getFecha().format(F) + "/" + g.getTransportista() + "/guia" + g.getNumeroGuia() + ".pdf";
    }

    private GuiaDespacho buscar(Long n) {
        return repo.findById(n).orElseThrow(() -> new RecursoNoEncontradoException("Guia " + n + " no encontrada"));
    }

    private GuiaResponse toResponse(GuiaDespacho g) {
        return new GuiaResponse(g.getNumeroGuia(), g.getTransportista(), g.getFecha(),
                g.getDestino(), g.getPedido(), g.getArchivoKey());
    }

    @Override
    @Transactional
    public GuiaResponse crear(GuiaRequest request) {
        GuiaDespacho g = new GuiaDespacho();
        g.setTransportista(request.transportista());
        g.setFecha(request.fecha());
        g.setDestino(request.destino());
        g.setPedido(request.pedido());
        repo.save(g);
        subirAS3(g.getNumeroGuia());
        GuiaResponse response = toResponse(buscar(g.getNumeroGuia()));
        queuePublisher.publicarGuia(new GuiaColaMensaje(response.numeroGuia(), response.transportista(),
                response.fecha(), response.destino(), response.pedido(), response.archivoKey()));
        return response;
    }

    @Override
    @Transactional
    public String subirAS3(Long n) {
        GuiaDespacho g = buscar(n);
        byte[] pdf = pdfService.generar(g);
        String key = buildKey(g);
        Path p = efs.guardar(key, pdf);
        s3.subir(key, pdf, "application/pdf");
        g.setArchivoKey(key);
        g.setEfsPath(p.toString());
        repo.save(g);
        return key;
    }

    @Override
    @Transactional(readOnly = true)
    public GuiaResponse obtener(Long n) {
        return toResponse(buscar(n));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] descargarDeS3(Long n, String solicitante) {
        GuiaDespacho g = buscar(n);
        if (!g.getTransportista().equalsIgnoreCase(solicitante)) {
            throw new AccesoDenegadoException("Transportista no autorizado");
        }
        String key = g.getArchivoKey() != null ? g.getArchivoKey() : buildKey(g);
        return s3.descargar(key);
    }

    @Override
    @Transactional
    public GuiaResponse actualizar(Long n, GuiaRequest request) {
        GuiaDespacho g = buscar(n);
        g.setTransportista(request.transportista());
        g.setFecha(request.fecha());
        g.setDestino(request.destino());
        g.setPedido(request.pedido());
        byte[] pdf = pdfService.generar(g);
        String key = g.getArchivoKey() != null ? g.getArchivoKey() : buildKey(g);
        efs.guardar(key, pdf);
        s3.subir(key, pdf, "application/pdf");
        g.setArchivoKey(key);
        repo.save(g);
        return toResponse(g);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuiaResponse> historial(String transportista, LocalDate fecha) {
        List<GuiaDespacho> lista;
        if (transportista != null && fecha != null) {
            lista = repo.findByTransportistaAndFecha(transportista, fecha);
        } else if (transportista != null) {
            lista = repo.findByTransportista(transportista);
        } else if (fecha != null) {
            lista = repo.findByFecha(fecha);
        } else {
            lista = repo.findAll();
        }
        return lista.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void eliminar(Long n) {
        GuiaDespacho g = buscar(n);
        String key = g.getArchivoKey() != null ? g.getArchivoKey() : buildKey(g);
        if (g.getArchivoKey() != null) {
            s3.borrar(key);
            efs.borrar(key);
        }
        repo.delete(g);
    }
}
