package org.springframework.samples.fitslot.membership;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/membership-tiers")
public class MembershipTierController {

    private final MembershipTierService service;

    public MembershipTierController(
            MembershipTierService service) {
        this.service = service;
    }

    @GetMapping
    public Iterable<MembershipTier> getAllTiers() {
        return service.getAllTiers();
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

        if (services == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Services cannot be null"
            );
        }

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

}
