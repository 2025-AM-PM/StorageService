package AmpmStorage.common.exception;

import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Hidden // Springdoc OpenAPI 문서에서 이 클래스를 숨깁니다.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: status={}, message={}", e.getStatus(), e.getMessage());
        Map<String, Object> body = Map.of(
            "status", e.getStatus().value(),
            "message", e.getMessage()
        );
        return new ResponseEntity<>(body, e.getStatus());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(IllegalArgumentException e) {
        log.warn("BusinessException: status={}, message={}", HttpStatus.BAD_REQUEST,
            e.getMessage());
        Map<String, Object> body = Map.of(
            "status", HttpStatus.BAD_REQUEST,
            "message", e.getMessage()
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNoResourceFound(NoResourceFoundException e) {
        // "fetch 오류"를 일으킨 OPTIONS 요청은 로그에 남기지 않음
        if (!e.getHttpMethod().equals("OPTIONS")) {
            log.warn("404 Not Found: {}", e.getMessage());
        }
        return new ResponseEntity<>("Resource not found", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception e) {
        log.error("Unhandled Exception: ", e);
        Map<String, Object> body = Map.of(
            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "message", e.getMessage()
        );
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }


}