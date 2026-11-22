
package org.springframework.samples.fitslot.classes;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping({ "/api/v1/class-sessions", "/api/class-sessions" })
public class ClassSessionController {

    private final ClassSessionService classSessionService;

    public ClassSessionController(
            ClassSessionService classSessionService) {
        this.classSessionService = classSessionService;
    }

    @GetMapping
    public ResponseEntity<List<ClassSessionResponse>> getClassSessions() {
        return ResponseEntity.ok(
                classSessionService.getClassSessionResponses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassSessionResponse> getClassSession(
            @PathVariable Integer id) {
        try {
            return ResponseEntity.ok(
                    classSessionService.getClassSessionResponse(id));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PutMapping("/{id}/schedule")
    public ResponseEntity<Void> updateSchedule(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateClassSessionRequest request) {

        try {
            classSessionService.updateClassSession(
                    id,
                    request.date(),
                    request.startTime(),
                    request.endTime(),
                    request.room());

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, ex.getMessage());

        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, ex.getMessage());
        }
    }
}
