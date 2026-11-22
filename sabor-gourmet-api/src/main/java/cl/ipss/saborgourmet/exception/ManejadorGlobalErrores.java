package cl.ipss.saborgourmet.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ManejadorGlobalErrores {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validarCampos(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, mensaje, request);
    }

    @ExceptionHandler({IllegalArgumentException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorRespuesta> solicitudIncorrecta(
            Exception ex, HttpServletRequest request) {
        // No se incluyen detalles de deserialización en la respuesta al cliente.
        String mensaje = (ex instanceof IllegalArgumentException)
                ? ex.getMessage() : "La solicitud contiene valores o formato JSON invalidos";
        return error(HttpStatus.BAD_REQUEST, mensaje, request);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> recursoInexistente(
            RecursoNoEncontradoException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictoNegocioException.class)
    public ResponseEntity<ErrorRespuesta> conflicto(
            ConflictoNegocioException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> falloInterno(
            Exception ex, HttpServletRequest request) {
        // La API responde sin exponer rutas de archivos ni trazas de excepción.
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "No fue posible completar la operacion", request);
    }

    private ResponseEntity<ErrorRespuesta> error(
            HttpStatus estado, String mensaje, HttpServletRequest request) {
        ErrorRespuesta respuesta = new ErrorRespuesta(
                estado.value(), estado.getReasonPhrase(), mensaje, request.getRequestURI());
        return ResponseEntity.status(estado).body(respuesta);
    }
}
