package org.springframework.samples.fitslot.medicalcertificate;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MedicalCertificateConfiguration {

    @Bean
    Clock applicationClock() {
        return Clock.systemDefaultZone();
    }
}
