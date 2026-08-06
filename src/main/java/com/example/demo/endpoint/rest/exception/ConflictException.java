package com.example.demo.endpoint.rest.exception;

/** Thrown when the request is valid but conflicts with the current state (e.g. a taken seat). */
public class ConflictException extends RuntimeException {
  public ConflictException(String message) {
    super(message);
  }
}
