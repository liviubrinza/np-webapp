package com.brinza.notary.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    // H2 console's own UI relies on inline scripts/styles and framing that a strict CSP would
    // break, and it's a dev-only tool anyway (see the CSRF exemption below), so it's excluded
    // rather than loosened for everyone.
    private static final String CSP_DIRECTIVES = String.join("; ",
            "default-src 'self'",
            // 'unsafe-inline' is still needed for script-src/style-src: templates carry inline
            // <script th:inline="javascript"> blocks (map/calendar/chart init) and Bootstrap/Google
            // Fonts inject inline styles. Tightening this to a nonce-based policy is follow-up work.
            "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net",
            "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com",
            "font-src 'self' https://fonts.gstatic.com",
            "img-src 'self' data: https://cdn.jsdelivr.net https://*.tile.openstreetmap.org",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "frame-ancestors 'self'",
            "form-action 'self'");

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("passwordEncoder called");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                new NegatedRequestMatcher(PathPatternRequestMatcher.pathPattern("/h2-console/**")),
                                new ContentSecurityPolicyHeaderWriter(CSP_DIRECTIVES))))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/admin/login").permitAll()
                        .requestMatchers("/admin/appointments/**").hasAnyRole("ADMIN", "TECHNICIAN")
                        // Exact match, evaluated before the "/admin/statistics/**" rule below: the
                        // Requests (Cereri) tab is open to both roles, every other statistics tab
                        // (traffic/activity/logs) stays TECHNICIAN-only.
                        .requestMatchers("/admin/statistics").hasAnyRole("ADMIN", "TECHNICIAN")
                        .requestMatchers("/admin/statistics/**", "/admin/users/**", "/admin/settings/**").hasRole("TECHNICIAN")
                        .requestMatchers("/admin/**").authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .defaultSuccessUrl("/admin", true)
                        // A locked account gets its own query param/message (surfaced on
                        // login.html) rather than the generic wrong-credentials one, per explicit
                        // request - note this does mean a locked username is now distinguishable
                        // from a merely-wrong-password one.
                        .failureHandler((request, response, exception) -> response.sendRedirect(
                                request.getContextPath() + (exception instanceof LockedException
                                        ? "/admin/login?locked"
                                        : "/admin/login?error")))
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout")
                        .permitAll()
                );
        log.debug("Security filter chain configured");
        return http.build();
    }
}
