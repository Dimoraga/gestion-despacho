package cl.duoc.transportista.despacho.consumidor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {
  @Bean
  S3Client s3Client(
      @Value("${aws.s3.region:us-east-1}") String region,
      @Value("${aws.accessKeyId:}") String key,
      @Value("${aws.secretAccessKey:}") String secret,
      @Value("${aws.sessionToken:}") String token) {
    var builder = S3Client.builder().region(Region.of(region));
    if (!key.isBlank() && !secret.isBlank())
      builder.credentialsProvider(
          StaticCredentialsProvider.create(
              token.isBlank()
                  ? AwsBasicCredentials.create(key, secret)
                  : AwsSessionCredentials.create(key, secret, token)));
    else builder.credentialsProvider(DefaultCredentialsProvider.create());
    return builder.build();
  }
}
