package cl.duoc.transportista.despacho.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

  @Mock private S3Client s3Client;

  private S3StorageService service;

  @BeforeEach
  void setUp() {
    service = new S3StorageService(s3Client);
    ReflectionTestUtils.setField(service, "bucket", "test-bucket");
  }

  @Test
  void subir_enviaRequestConBucketYKey() {
    byte[] contenido = new byte[] {1, 2, 3};
    String key = "20210315/transportistaX/guia1.pdf";

    service.subir(key, contenido, "application/pdf");

    ArgumentCaptor<PutObjectRequest> cap = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(cap.capture(), any(RequestBody.class));
    assertEquals("test-bucket", cap.getValue().bucket());
    assertEquals(key, cap.getValue().key());
  }

  @Test
  void listar_devuelveKeysDelResponse() {
    String key = "20210315/transportistaX/guia1.pdf";
    ListObjectsV2Response response =
        ListObjectsV2Response.builder().contents(S3Object.builder().key(key).build()).build();
    when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);

    List<String> resultado = service.listar("20210315/");

    assertTrue(resultado.contains(key));
  }

  @Test
  void descargar_devuelveBytesDelObjeto() {
    byte[] esperado = new byte[] {9};
    ResponseBytes<GetObjectResponse> responseBytes =
        ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), esperado);
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

    byte[] resultado = service.descargar("20210315/transportistaX/guia1.pdf");

    assertArrayEquals(esperado, resultado);
  }
}
