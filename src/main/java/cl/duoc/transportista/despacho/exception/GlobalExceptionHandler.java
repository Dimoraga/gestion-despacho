package cl.duoc.transportista.despacho.exception;

import cl.duoc.transportista.despacho.dto.ErrorResponse;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AccesoDenegadoException.class)
  public ResponseEntity<ErrorResponse> handleAccesoDenegado(AccesoDenegadoException ex) {
    HttpStatus status = HttpStatus.FORBIDDEN;
    return ResponseEntity.status(status)
        .body(
            new ErrorResponse(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage()));
  }

  @ExceptionHandler({RecursoNoEncontradoException.class, NoSuchElementException.class})
  public ResponseEntity<ErrorResponse> handleNoEncontrado(RuntimeException ex) {
    HttpStatus status = HttpStatus.NOT_FOUND;
    return ResponseEntity.status(status)
        .body(
            new ErrorResponse(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidacion(MethodArgumentNotValidException ex) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    String mensaje =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse(ex.getMessage());
    return ResponseEntity.status(status)
        .body(
            new ErrorResponse(
                Instant.now().toString(), status.value(), status.getReasonPhrase(), mensaje));
  }
}
