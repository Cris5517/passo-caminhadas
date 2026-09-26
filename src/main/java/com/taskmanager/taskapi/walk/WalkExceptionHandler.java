package com.taskmanager.taskapi.walk;

import com.taskmanager.taskapi.task.ApiExceptionHandler.ApiError;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WalkExceptionHandler {

	@ExceptionHandler(WalkNotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(WalkNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiError(Instant.now(), 404, "Not Found", exception.getMessage()));
	}

	@ExceptionHandler(WalkStateException.class)
	public ResponseEntity<ApiError> handleInvalidState(WalkStateException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ApiError(Instant.now(), 409, "Conflict", exception.getMessage()));
	}
}