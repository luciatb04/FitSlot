package org.springframework.samples.fitslot.classes;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/class-sessions")
@PreAuthorize("hasAuthority('GYM_OWNER')")
public class InstructorSubstitutionController {

    private final InstructorSubstitutionService service;

    public InstructorSubstitutionController(
            InstructorSubstitutionService service) {
        this.service = service;
    }

    @GetMapping("/{id}/available-instructors")
    public ResponseEntity<List<AvailableInstructorResponse>>
            getAvailableInstructors(@PathVariable Integer id) {

        return ResponseEntity.ok(
            service.getAvailableInstructors(id)
        );
    }

    @PutMapping("/{id}/instructor")
    public ResponseEntity<Void> substituteInstructor(
            @PathVariable Integer id,
            @Valid @RequestBody SubstituteInstructorRequest request) {

        service.substituteInstructor(id, request.instructorId());

        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NoSuchElementException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(
            IllegalArgumentException ex) {

        return ResponseEntity.badRequest()
            .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            IllegalStateException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(ex.getMessage()));
    }

    public record ErrorResponse(String message) {}
}
