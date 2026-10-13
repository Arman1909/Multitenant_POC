package com.example.pbm.exception;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AccessDeniedException.class) ResponseEntity<Map<String, String>> denied(AccessDeniedException e) { return err(HttpStatus.FORBIDDEN, e.getMessage()); }
    @ExceptionHandler(BadCredentialsException.class) ResponseEntity<Map<String, String>> creds(BadCredentialsException e) { return err(HttpStatus.UNAUTHORIZED, "Invalid credentials"); }
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<Map<String, String>> bad(IllegalArgumentException e) { return err(HttpStatus.BAD_REQUEST, e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) { return err(HttpStatus.BAD_REQUEST, "Validation failed"); }
    private ResponseEntity<Map<String, String>> err(HttpStatus s, String m) { return ResponseEntity.status(s).body(Map.of("error", String.valueOf(m))); }
}
