package com.google.allenday.nanostream.launcher.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Date;

@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RestResponseEntityExceptionHandler.class);

    @ExceptionHandler(value = {BadRequestException.class})
    protected ResponseEntity<Object> handleException(BadRequestException ex, WebRequest request) {
        ErrorMessage body = new ErrorMessage(ex.getError(), ex.getMessage());
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    private static class ErrorMessage {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        private Date date = new Date();
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private String error = "";
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private String message = "";

        public ErrorMessage(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}