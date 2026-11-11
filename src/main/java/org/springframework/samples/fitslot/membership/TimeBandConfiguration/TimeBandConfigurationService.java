
package org.springframework.samples.fitslot.membership.TimeBandConfiguration;

import java.time.LocalTime;

import org.springframework.http.HttpStatus;
import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TimeBandConfigurationService {

    private final TimeBandConfigurationRepository repository;

    public TimeBandConfigurationService(
            TimeBandConfigurationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Iterable<TimeBandConfiguration> getAll() {
        return repository.findAll();
    }

    @Transactional
    public TimeBandConfiguration update(
            TimeBand timeBand,
            LocalTime startTime,
            LocalTime endTime) {

        if (startTime == null || endTime == null ||
                !startTime.isBefore(endTime)) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid time range"
            );
        }

        TimeBandConfiguration configuration =
            repository.findByTimeBand(timeBand)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Time band configuration not found"
                    )
                );

        configuration.setStartTime(startTime);
        configuration.setEndTime(endTime);

        return repository.save(configuration);
    }
    
}
