package cl.duoc.transportista.despacho.consumidor.service;

import cl.duoc.transportista.despacho.consumidor.dto.GuiaColaMensaje;
import cl.duoc.transportista.despacho.consumidor.dto.GuiaDespachoRegistroResponse;
import cl.duoc.transportista.despacho.consumidor.model.GuiaDespachoRegistro;
import cl.duoc.transportista.despacho.consumidor.repository.GuiaDespachoRegistroRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class GuiaColaConsumerService {

    private static final ParameterizedTypeReference<GuiaColaMensaje> TIPO_MENSAJE =
            new ParameterizedTypeReference<>() {};

    private final RabbitTemplate rabbitTemplate;
    private final GuiaDespachoRegistroRepository repo;
    private final String guiasQueueName;

    public GuiaColaConsumerService(RabbitTemplate rabbitTemplate, GuiaDespachoRegistroRepository repo,
                                    @Value("${app.rabbitmq.queue.guias}") String guiasQueueName) {
        this.rabbitTemplate = rabbitTemplate;
        this.repo = repo;
        this.guiasQueueName = guiasQueueName;
    }

    @Transactional
    public List<GuiaDespachoRegistroResponse> consumirColaGuias() {
        List<GuiaDespachoRegistroResponse> procesadas = new ArrayList<>();
        GuiaColaMensaje mensaje;
        while ((mensaje = rabbitTemplate.receiveAndConvert(guiasQueueName, TIPO_MENSAJE)) != null) {
            GuiaDespachoRegistro registro = new GuiaDespachoRegistro();
            registro.setNumeroGuia(mensaje.numeroGuia());
            registro.setTransportista(mensaje.transportista());
            registro.setFecha(mensaje.fecha());
            registro.setDestino(mensaje.destino());
            registro.setPedido(mensaje.pedido());
            registro.setArchivoKey(mensaje.archivoKey());
            registro.setFechaProcesado(Instant.now());
            repo.save(registro);
            procesadas.add(toResponse(registro));
        }
        return procesadas;
    }

    private GuiaDespachoRegistroResponse toResponse(GuiaDespachoRegistro r) {
        return new GuiaDespachoRegistroResponse(r.getId(), r.getNumeroGuia(), r.getTransportista(),
                r.getFecha(), r.getDestino(), r.getPedido(), r.getArchivoKey(), r.getFechaProcesado());
    }
}
