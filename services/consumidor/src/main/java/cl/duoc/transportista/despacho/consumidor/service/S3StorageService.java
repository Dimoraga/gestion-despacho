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

  /** Creates the deterministic object once. A 412 is safe only when its stored checksum matches. */
  public void subirSiAusente(String key, byte[] bytes, String checksum) {
    try {
      client.putObject(
          PutObjectRequest.builder()
              .bucket(bucket)
              .key(key)
              .contentType("application/pdf")
              .metadata(java.util.Map.of("sha256", checksum))
              .ifNoneMatch("*")
              .build(),
          RequestBody.fromBytes(bytes));
    } catch (S3Exception e) {
      if (e.statusCode() != 412) throw e;
      String existing = client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build())
          .metadata().get("sha256");
      if (!checksum.equals(existing)) throw new IllegalStateException("S3 key exists with another checksum", e);
    }
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
