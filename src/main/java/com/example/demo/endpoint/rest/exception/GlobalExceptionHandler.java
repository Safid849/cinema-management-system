package com.example.demo.endpoint.rest.exception;

import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorDTO> handleNotFound(NotFoundException e) {
    return status(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorDTO> handleBadRequest(BadRequestException e) {
    return status(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
  }

  /** Bean-validation failures on @Valid bodies, reported field by field. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorDTO> handleInvalidBody(MethodArgumentNotValidException e) {
    String details =
        e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + defaultMessage(error))
            .collect(Collectors.joining(", "));
    return status(HttpStatus.BAD_REQUEST, "BAD_REQUEST", details);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ErrorDTO> handleUnreadable(Exception e) {
    return status(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Malformed request: " + e.getMessage());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorDTO> handleAccessDenied(AccessDeniedException e) {
    return status(HttpStatus.FORBIDDEN, "FORBIDDEN", e.getMessage());
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorDTO> handleAuthentication(AuthenticationException e) {
    return status(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", e.getMessage());
  }

  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ResponseEntity<ErrorDTO> handleEmailAlreadyExists(EmailAlreadyExistsException e) {
    return status(HttpStatus.CONFLICT, "CONFLICT", e.getMessage());
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorDTO> handleConflict(ConflictException e) {
    return status(HttpStatus.CONFLICT, "CONFLICT", e.getMessage());
  }

  /**
   * The database has the last word on double-booking and uniqueness: a race that slips past the
   * service checks still surfaces as a clean 409 rather than a 500.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorDTO> handleDataIntegrity(DataIntegrityViolationException e) {
    return status(HttpStatus.CONFLICT, "CONFLICT", "The request conflicts with an existing record");
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ErrorDTO> handleInvalidCredentials(InvalidCredentialsException e) {
    return status(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", e.getMessage());
  }

  private static String defaultMessage(FieldError error) {
    return error.getDefaultMessage() == null ? "is invalid" : error.getDefaultMessage();
  }

  private static ResponseEntity<ErrorDTO> status(HttpStatus httpStatus, String code, String msg) {
    return ResponseEntity.status(httpStatus).body(new ErrorDTO(code, msg));
  }
}
