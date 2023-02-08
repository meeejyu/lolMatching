package com.lol.match.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class ExceptionController {

    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<?> handleBusinessLogicException(BusinessLogicException exception) {
        return ResponseEntity.status(exception.getExceptionCode().getStatus()).body(exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleJwtException(Exception exception) {
        log.info("error class: {} ", exception.getClass());
        return ResponseEntity.status(500).body(exception.getMessage());
    }

}
