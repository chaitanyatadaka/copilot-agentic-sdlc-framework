package com.queueless.api;

import com.queueless.domain.ApiException;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ExceptionHandler {
  @org.springframework.web.bind.annotation.ExceptionHandler(ApiException.class)
  public ResponseEntity<Dtos.ErrorResponse> api(ApiException ex){
    String code=ex.getStatus()==400?"VALIDATION":ex.getStatus()==404?"NOT_FOUND":"CONFLICT";
    return ResponseEntity.status(ex.getStatus()).body(Dtos.ErrorResponse.of(code,ex.getMessage()));
  }
  @org.springframework.web.bind.annotation.ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Dtos.ErrorResponse> validation(MethodArgumentNotValidException ex){
    List<Dtos.FieldError> fields=new ArrayList<>();
    ex.getBindingResult().getFieldErrors().forEach(e->fields.add(new Dtos.FieldError(e.getField(),e.getDefaultMessage())));
    Dtos.ErrorResponse body=new Dtos.ErrorResponse("VALIDATION","One or more fields are invalid.",fields);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }
  @org.springframework.web.bind.annotation.ExceptionHandler({HttpMessageNotReadableException.class,ConstraintViolationException.class})
  public ResponseEntity<Dtos.ErrorResponse> malformed(Exception ex){
    return ResponseEntity.badRequest().body(Dtos.ErrorResponse.of("VALIDATION","Malformed or invalid request."));
  }
  @org.springframework.web.bind.annotation.ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Dtos.ErrorResponse> integrity(DataIntegrityViolationException ex){
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Dtos.ErrorResponse.of("CONFLICT","The operation conflicts with persisted state."));
  }
  @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
  public ResponseEntity<Dtos.ErrorResponse> unexpected(Exception ex){
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Dtos.ErrorResponse.of("INTERNAL_ERROR","An unexpected error occurred."));
  }
}
