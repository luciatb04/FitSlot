
package org.springframework.samples.fitslot.instructor;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/instructors/me/availability")
@PreAuthorize("hasAuthority('INSTRUCTOR')")
public class InstructorAvailabilityRestController {

    private final InstructorAvailabilityService service;

    public InstructorAvailabilityRestController(
            InstructorAvailabilityService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<InstructorAvailabilityResponse>> getMyAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {

        List<InstructorAvailabilityResponse> result = service.getMyAvailability(weekStart)
                .stream()
                .map(InstructorAvailabilityResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(result);
    }

    // Add availability
    @PostMapping
    public ResponseEntity<InstructorAvailabilityResponse> addAvailability(
            @Valid @RequestBody AvailabilitySlotRequest request) {

        InstructorAvailability saved = service.addAvailability(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(InstructorAvailabilityResponse.fromEntity(saved));
    }

    // Update availability
    @PutMapping("/{id}")
    public ResponseEntity<InstructorAvailabilityResponse> updateAvailability(
            @PathVariable Integer id,
            @Valid @RequestBody AvailabilitySlotRequest request) {

        InstructorAvailability updated = service.updateAvailability(id, request);

        return ResponseEntity.ok(
                InstructorAvailabilityResponse.fromEntity(updated));
    }

    // Delete availability
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAvailability(
            @PathVariable Integer id) {

        service.deleteAvailability(id);

        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPeriod(
            IllegalArgumentException ex) {

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleOverlap(
            IllegalStateException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NoSuchElementException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    public record ErrorResponse(String message) {
    }
}
