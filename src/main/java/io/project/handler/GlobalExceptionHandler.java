package io.project.handler;

import io.project.exception.LanguageNotFoundException;
import io.project.exception.TranslationResourceAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(LanguageNotFoundException.class)
    public ResponseEntity<String> handleLanguageNotFoundException(LanguageNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Не найден язык исходного сообщения");
    }

    @ExceptionHandler(TranslationResourceAccessException.class)
    public ResponseEntity<String> handleTranslationResourceAccessException(TranslationResourceAccessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ошибка доступа к ресурсу перевода");
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<String> handleHttpClientErrorException(HttpClientErrorException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ex.getMessage());
    }
}
