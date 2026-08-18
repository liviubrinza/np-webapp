package com.brinza.notary.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * Resolves a visitor's approximate (city-level) location from their IP address, for the admin
 * Statistics &gt; Traffic page. Uses the free ip-api.com lookup service - no API key, but plain
 * HTTP and rate-limited, which is fine at this site's traffic volume. Nothing here is persisted;
 * the result is handed to a caller-supplied callback so {@link TrafficStatsService} can attach it
 * to that IP's in-memory session entry whenever it arrives.
 *
 * <p>This is the app's only outbound HTTP dependency - production deployments need firewall
 * clearance for outgoing traffic to ip-api.com (see README.md).
 */
@Service
public class GeoLocationService {

    private static final Logger log = LoggerFactory.getLogger(GeoLocationService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    static final String LOCAL_LABEL = "Local";

    private final RestClient restClient;

    public GeoLocationService() {
        this(defaultRestClient());
    }

    GeoLocationService(RestClient restClient) {
        this.restClient = restClient;
    }

    private static RestClient defaultRestClient() {
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
        requestFactory.setReadTimeout(TIMEOUT);
        return RestClient.builder()
                .baseUrl("http://ip-api.com")
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Resolves {@code ip} off the calling thread and invokes {@code onResolved} once a location
     * is known. Best-effort: on lookup failure or timeout, {@code onResolved} is simply never
     * called, no retry.
     */
    @Async
    public void resolveAsync(String ip, Consumer<String> onResolved) {
        if (isPrivateOrLoopback(ip)) {
            onResolved.accept(LOCAL_LABEL);
            return;
        }
        try {
            GeoLookupResponse response = restClient.get()
                    .uri("/json/{ip}?fields=status,city,country", ip)
                    .retrieve()
                    .body(GeoLookupResponse.class);
            String location = formatLocation(response);
            if (location != null) {
                onResolved.accept(location);
            } else {
                log.debug("Geolocation lookup for ip={} did not return a resolvable location", ip);
            }
        } catch (RestClientException e) {
            log.debug("Geolocation lookup failed for ip={}: {}", ip, e.getMessage());
        }
    }

    static boolean isPrivateOrLoopback(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isLoopbackAddress() || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress() || address.isAnyLocalAddress();
        } catch (UnknownHostException e) {
            return true;
        }
    }

    static String formatLocation(GeoLookupResponse response) {
        if (response == null || !"success".equals(response.status())) {
            return null;
        }
        String city = blankToNull(response.city());
        String country = blankToNull(response.country());
        if (city == null) {
            return country;
        }
        return country == null ? city : city + ", " + country;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    record GeoLookupResponse(String status, String city, String country) {
    }
}
