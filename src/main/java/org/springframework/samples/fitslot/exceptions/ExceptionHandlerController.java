package org.springframework.samples.fitslot.exceptions;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.fitslot.classes.exceptions.DuplicatedClassesNameException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.ObjectError;

@RestControllerAdvice
public class ExceptionHandlerController {

	@ExceptionHandler(Exception.class)
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	public ResponseEntity<ErrorMessage> globalExceptionHandler(Exception ex, WebRequest request) {
		ErrorMessage message = new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), new Date(), ex.getMessage(),
				request.getDescription(false));

		return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	public ResponseEntity<ErrorMessage> resourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
		ErrorMessage message = new ErrorMessage(HttpStatus.NOT_FOUND.value(), new Date(), ex.getMessage(),
				request.getDescription(false));

		return new ResponseEntity<>(message, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(ResourceNotOwnedException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ResponseEntity<ErrorMessage> resourceNotOwnedException(ResourceNotOwnedException ex, WebRequest request) {
		ErrorMessage message = new ErrorMessage(HttpStatus.BAD_REQUEST.value(), new Date(), ex.getMessage(),
				request.getDescription(false));

		return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(UpperPlanFeatureException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ResponseEntity<ErrorMessage> upperPlanFeatureException(UpperPlanFeatureException ex, WebRequest request) {
		ErrorMessage message = new ErrorMessage(HttpStatus.BAD_REQUEST.value(), new Date(), ex.getMessage(),
				request.getDescription(false));

		return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
	}

//	@ExceptionHandler(value = TokenRefreshException.class)
//	@ResponseStatus(HttpStatus.FORBIDDEN)
//	public ResponseEntity<ErrorMessage> handleTokenRefreshException(TokenRefreshException ex, WebRequest request) {
//		ErrorMessage message = new ErrorMessage(HttpStatus.FORBIDDEN.value(), new Date(), ex.getMessage(),
//				request.getDescription(false));
//
//		return new ResponseEntity<ErrorMessage>(message, HttpStatus.FORBIDDEN);
//	}

	@ExceptionHandler(value = LimitReachedException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public ResponseEntity<ErrorMessage> handleLimitReachedException(LimitReachedException ex, WebRequest request) {
		ErrorMessage message = new ErrorMessage(HttpStatus.FORBIDDEN.value(), new Date(), ex.getMessage(),
				request.getDescription(false));

		return new ResponseEntity<>(message, HttpStatus.FORBIDDEN);
	}

	@ExceptionHandler(value = DuplicatedClassesNameException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ResponseEntity<ErrorMessage> handleDuplicatedClassesNameException(DuplicatedClassesNameException ex,
			WebRequest request) {
		ErrorMessage message = new ErrorMessage(HttpStatus.BAD_REQUEST.value(), new Date(), ex.getMessage(),
				request.getDescription(false));

		return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(value = MethodArgumentNotValidException.class)
	public final ResponseEntity<ErrorMessage> handleMethodArgumentException(MethodArgumentNotValidException ex,
			WebRequest request) {
		Map<String, Object> fieldError = new HashMap<>();
		List<ObjectError> errors = ex.getBindingResult().getAllErrors();
		for (ObjectError error : errors) {
			String key = error instanceof FieldError field ? field.getField() : "request";
			fieldError.put(key, error.getDefaultMessage());
		}
		ErrorMessage message = new ErrorMessage(HttpStatus.BAD_REQUEST.value(), new Date(), fieldError.toString(),
				request.getDescription(false));

		return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(value = HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorMessage> handleUnreadableRequest(HttpMessageNotReadableException ex,
			WebRequest request) {
		ErrorMessage message = new ErrorMessage(HttpStatus.BAD_REQUEST.value(), new Date(),
				"Invalid request body", request.getDescription(false));

		return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(value = AccessDeniedException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public ResponseEntity<ErrorMessage> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
		ErrorMessage message = new ErrorMessage(HttpStatus.FORBIDDEN.value(), new Date(), ex.getMessage(),
				request.getDescription(false));

		return new ResponseEntity<>(message, HttpStatus.FORBIDDEN);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ErrorMessage> handleResponseStatus(
			ResponseStatusException ex, WebRequest request) {
		HttpStatusCode status = ex.getStatusCode();
		String reason = ex.getReason() == null ? "Request failed" : ex.getReason();

		ErrorMessage body = new ErrorMessage(
			status.value(),
			new Date(),
			reason,
			request.getDescription(false)
		);

		return ResponseEntity.status(status).body(body);
	}

}
