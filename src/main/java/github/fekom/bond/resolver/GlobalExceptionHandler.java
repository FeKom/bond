package github.fekom.bond.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	public record ErrorResponse(String code, String message) {}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException e) {
		log.warn("Bad request: {}", e.getMessage());
		return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
		log.warn("Invalid request body: {}", e.getMessage());
		return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_REQUEST_BODY", "Invalid request body format"));
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<ErrorResponse> handleMissingRequestHeader(MissingRequestHeaderException e) {
		log.warn("Missing required header: {}", e.getHeaderName());
		return ResponseEntity.badRequest().body(
			new ErrorResponse("MISSING_HEADER", "Missing required header: " + e.getHeaderName())
		);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
		log.error("Unexpected error", e);
		return ResponseEntity.internalServerError().body(
			new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred")
		);
	}
}
