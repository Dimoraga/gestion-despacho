package cl.duoc.transportista.despacho.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ConsumerClientConfig {

  @Bean
  @Primary
  RestOperations consumidorRestOperations(
      RestTemplateBuilder builder,
      @Value("${app.consumer.base-url}") String consumerBaseUrl,
      @Value("${app.consumer.connect-timeout:2s}") Duration connectTimeout,
      @Value("${app.consumer.read-timeout:3s}") Duration readTimeout) {
    RestTemplate restTemplate =
        builder
            .rootUri(consumerBaseUrl)
            .setConnectTimeout(connectTimeout)
            .setReadTimeout(readTimeout)
            .build();
    return restTemplate;
  }
}
