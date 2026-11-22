
package org.springframework.samples.fitslot.configuration;

import static org.springframework.security.config.Customizer.withDefaults;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.samples.fitslot.configuration.jwt.AuthEntryPointJwt;
import org.springframework.samples.fitslot.configuration.jwt.AuthTokenFilter;
import org.springframework.samples.fitslot.configuration.services.UserDetailsServiceImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    DataSource dataSource;

    private static final String ADMIN = "ADMIN";
    private static final String GYM_OWNER = "GYM_OWNER";
    private static final String MEMBER = "MEMBER";
    private static final String RECEPTIONIST = "RECEPTIONIST";
    private static final String INSTRUCTOR = "INSTRUCTOR";

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http)
            throws Exception {

        http
            .cors(withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )
            .headers(headers ->
                headers.frameOptions(frameOptions ->
                    frameOptions.disable()
                )
            )
            .exceptionHandling(exceptionHandling ->
                exceptionHandling.authenticationEntryPoint(
                    unauthorizedHandler
                )
            )

            .authorizeHttpRequests(auth -> auth

                // Static resources
                .requestMatchers(
                    PathRequest.toStaticResources()
                        .atCommonLocations()
                ).permitAll()

                // H2 Console
                .requestMatchers(
                    PathRequest.toH2Console()
                ).permitAll()

                .requestMatchers("/h2-console/**")
                .permitAll()

                // Public pages
                .requestMatchers("/", "/oups")
                .permitAll()

                // Swagger / OpenAPI
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/swagger-resources/**"
                ).permitAll()

                // Public API
                .requestMatchers("/api/v1/auth/**")
                .permitAll()

                .requestMatchers("/api/v1/developers")
                .permitAll()

                .requestMatchers("/api/v1/plan")
                .permitAll()

                .requestMatchers("/api/v1/gyms")
                .permitAll()

                // Administrator endpoints
                .requestMatchers("/api/v1/users/**")
                .hasAuthority(ADMIN)

                .requestMatchers("/api/v1/gymOwners/all")
                .hasAuthority(ADMIN)

                .requestMatchers("/api/v1/classs/stats")
                .hasAuthority(ADMIN)

                .requestMatchers("/api/v1/instructors/stats")
                .hasAuthority(ADMIN)

                // Gym owner endpoints
                .requestMatchers("/api/v1/gymOwners/**")
                .hasAnyAuthority(ADMIN, GYM_OWNER)

                // Member visits
                .requestMatchers("/api/v1/visits/**")
                .hasAnyAuthority(MEMBER, RECEPTIONIST)

                // Existing class endpoints
                .requestMatchers("/api/v1/classs")
                .authenticated()

                .requestMatchers("/api/v1/classs/**")
                .authenticated()

                // ========================================
                // CLASS SCHEDULE AND AVAILABILITY
                // Members can view scheduled classes
                // and available spots.
                // ========================================

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/class-sessions",
                    "/api/v1/class-sessions/*",
                    "/api/class-sessions",
                    "/api/class-sessions/*"
                )
                .hasAnyAuthority(
                    MEMBER,
                    GYM_OWNER,
                    ADMIN
                )

                // ========================================
                // INSTRUCTOR SUBSTITUTION
                // Only gym owners can substitute instructors
                // ========================================

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/class-sessions/*/available-instructors"
                )
                .hasAuthority(GYM_OWNER)

                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/v1/class-sessions/*/instructor"
                )
                .hasAuthority(GYM_OWNER)

                // Other class session operations
                .requestMatchers(
                    "/api/v1/class-sessions/**"
                )
                .hasAnyAuthority(GYM_OWNER, ADMIN)

                .requestMatchers(
                    "/api/class-sessions/**"
                )
                .hasAnyAuthority(GYM_OWNER, ADMIN)

                // Gym management
                .requestMatchers("/api/v1/gyms/**")
                .hasAnyAuthority(GYM_OWNER, ADMIN)

                // Instructor availability
                .requestMatchers(
                    "/api/v1/instructors/me/availability",
                    "/api/v1/instructors/me/availability/**"
                )
                .hasAuthority(INSTRUCTOR)

                // Instructor information
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/instructors/**"
                )
                .authenticated()

                // Other instructor operations
                .requestMatchers("/api/v1/instructors/**")
                .hasAnyAuthority(
                    ADMIN,
                    INSTRUCTOR,
                    GYM_OWNER
                )

                // Member management
                .requestMatchers("/api/v1/members/**")
                .hasAnyAuthority(ADMIN, RECEPTIONIST)

                // Access management
                .requestMatchers("/api/v1/access/**")
                .hasAnyAuthority(ADMIN, RECEPTIONIST)

                // Payments
                .requestMatchers("/api/v1/payments/**")
                .hasAnyAuthority(ADMIN, RECEPTIONIST)

                // Medical certificates
                .requestMatchers(
                    "/api/v1/members/*/medical-certificates/**"
                )
                .hasAnyAuthority(ADMIN, RECEPTIONIST)

                // Membership tiers - Read
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/membership-tiers/**"
                )
                .hasAnyAuthority(
                    GYM_OWNER,
                    ADMIN,
                    RECEPTIONIST
                )

                // Membership tiers - Management
                .requestMatchers("/api/membership-tiers/**")
                .hasAuthority(GYM_OWNER)

                // Time bands
                .requestMatchers("/api/time-bands/**")
                .hasAuthority(GYM_OWNER)

                // Deny all other requests
                .anyRequest().denyAll()
            )

            .addFilterBefore(
                authenticationJwtTokenFilter(),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
