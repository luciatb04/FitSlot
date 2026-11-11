package org.springframework.samples.fitslot.membership.TimeBandConfiguration;

import java.time.LocalTime;
import java.util.Map;

import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBand;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/time-bands")
public class TimeBandConfigurationController {

    private final TimeBandConfigurationService service;

    public TimeBandConfigurationController(
            TimeBandConfigurationService service) {
        this.service = service;
    }

    @GetMapping
    public Iterable<TimeBandConfiguration> getAll() {
        return service.getAll();
    }

    @PutMapping("/{timeBand}")
    public TimeBandConfiguration update(
            @PathVariable TimeBand timeBand,
            @RequestBody Map<String, LocalTime> times) {

        return service.update(
            timeBand,
            times.get("startTime"),
            times.get("endTime")
        );
    }
}
