package org.springframework.samples.fitslot.membership;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/membership-tiers")
public class MembershipTierController {

    private final MembershipTierService service;

    public MembershipTierController(
            MembershipTierService service) {
        this.service = service;
    }

    @GetMapping
    public List<MembershipTierResponse> getAllTiers() {
        List<MembershipTierResponse> responses = new ArrayList<>();
        for (MembershipTier tier : service.getAllTiers()) {
            responses.add(toResponse(tier));
        }
        return responses;
    }

    @PostMapping
    public ResponseEntity<MembershipTierResponse> createTier(
            @Valid @RequestBody MembershipTierRequest request) {
        MembershipTier createdTier = service.createTier(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(createdTier));
    }

    @GetMapping("/{id}/services")
    public Set<ServiceType> getServicesById(
            @PathVariable Integer id) {

        return service.getServices(id);
    }

    @PutMapping("/{id}/services")
    public Set<ServiceType> updateServices(
            @PathVariable Integer id,
            @RequestBody Set<ServiceType> services) {

        return service.updateServices(id, services);
    }


    @GetMapping("/{id}/time-band")
    public TimeBand getTimeBandById(
        @PathVariable Integer id) {

         return service.getTimeBand(id);
    }

    @PutMapping("/{id}/time-band")
    public TimeBand updateTimeBand(
        @PathVariable Integer id,
        @RequestBody TimeBand timeBand) {

        return service.updateTimeBand(id, timeBand);
    }

    private MembershipTierResponse toResponse(MembershipTier tier) {
        return new MembershipTierResponse(
            tier.getId(),
            tier.getName(),
            tier.getPrice(),
            tier.getDurationDays(),
            tier.getEntryQuota(),
            Set.copyOf(tier.getAllowedServices()),
            tier.getTimeBand()
        );
    }

}
