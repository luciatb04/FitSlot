package org.springframework.samples.fitslot.access;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/access")
public class AccessController {

    private final AccessService accessService;

    public AccessController(AccessService accessService) {
        this.accessService = accessService;
    }

    @PostMapping("/check-in")
    public CheckInResponse checkIn(@Valid @RequestBody CheckInRequest request) {
        return accessService.checkIn(request.memberId(), request.serviceAccessed());
    }
}
