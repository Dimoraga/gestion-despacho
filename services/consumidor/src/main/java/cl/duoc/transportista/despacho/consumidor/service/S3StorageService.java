package cl.duoc.transportista.despacho.consumidor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
public class S3StorageService {
  private final S3Client client;
  private final String bucket;

  S3StorageService(S3Client client, @Value("${aws.s3.bucket}") String bucket) {
    this.client = client;
    this.bucket = bucket;
  }

  public void subir(String key, byte[] bytes) {
    client.putObject(
        PutObjectRequest.builder().bucket(bucket).key(key).contentType("application/pdf").build(),
        RequestBody.fromBytes(bytes));
  }

  public byte[] descargar(String key) {
    return client
        .getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build())
        .asByteArray();
  }

  public void borrar(String key) {
    client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
  }
}
