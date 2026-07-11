package cl.duoc.transportista.despacho.service;

import cl.duoc.transportista.despacho.dto.GuiaDespachoMensaje;
import cl.duoc.transportista.despacho.dto.MqResultadoResponse;
import cl.duoc.transportista.despacho.exception.RecursoNoEncontradoException;
import cl.duoc.transportista.despacho.model.GuiaDespacho;
import cl.duoc.transportista.despacho.model.GuiaDespachoResumenMq;
import cl.duoc.transportista.despacho.repository.GuiaDespachoRepository;
import cl.duoc.transportista.despacho.repository.GuiaDespachoResumenMqRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GuiaDespachoMqService {

    private final GuiaDespachoRepository guiaRepo;
    private final GuiaDespachoResumenMqRepository resumenRepo;
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final String queue;

    public GuiaDespachoMqService(GuiaDespachoRepository guiaRepo,
                                 GuiaDespachoResumenMqRepository resumenRepo,
                                 RabbitTemplate rabbitTemplate,
                                 @Value("${app.rabbitmq.exchange:guia-despacho.exchange}") String exchange,
                                 @Value("${app.rabbitmq.routing-key:guia-despacho.creada}") String routingKey,
                                 @Value("${app.rabbitmq.queue:guia-despacho.queue}") String queue) {
        this.guiaRepo = guiaRepo;
        this.resumenRepo = resumenRepo;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.queue = queue;
    }

    @Transactional(readOnly = true)
    public MqResultadoResponse enviarGuiaARabbit(Long numeroGuia) {
        GuiaDespacho guia = guiaRepo.findById(numeroGuia)
                .orElseThrow(() -> new RecursoNoEncontradoException("Guia " + numeroGuia + " no encontrada"));
        GuiaDespachoMensaje mensaje = new GuiaDespachoMensaje(
                guia.getNumeroGuia(), guia.getTransportista(), guia.getFecha(), guia.getDestino(),
                guia.getPedido(), guia.getArchivoKey(), LocalDateTime.now());
        rabbitTemplate.convertAndSend(exchange, routingKey, mensaje);
        return new MqResultadoResponse("Guia enviada a RabbitMQ", guia.getNumeroGuia());
    }

    @Transactional
    public GuiaDespachoResumenMq consumirGuiaDesdeRabbit() {
        Object recibido = rabbitTemplate.receiveAndConvert(queue);
        if (recibido == null) {
            return null;
        }
        if (!(recibido instanceof GuiaDespachoMensaje mensaje)) {
            throw new IllegalStateException("Mensaje RabbitMQ no soportado: " + recibido.getClass().getName());
        }
        GuiaDespachoResumenMq resumen = new GuiaDespachoResumenMq();
        resumen.setNumeroGuia(mensaje.numeroGuia());
        resumen.setTransportista(mensaje.transportista());
        resumen.setFecha(mensaje.fecha());
        resumen.setDestino(mensaje.destino());
        resumen.setPedido(mensaje.pedido());
        resumen.setArchivoKey(mensaje.archivoKey());
        resumen.setFechaMensaje(mensaje.fechaMensaje());
        resumen.setFechaConsumo(LocalDateTime.now());
        return resumenRepo.save(resumen);
    }

    @Transactional(readOnly = true)
    public List<GuiaDespachoResumenMq> listarResumenesMq() {
        return resumenRepo.findAll();
    }
}
