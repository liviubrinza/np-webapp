package com.brinza.notary.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class GeoLocationServiceTest {

    @Test
    void resolveAsyncSkipsPrivateIpsWithoutCallingRestClient() {
        RestClient restClient = mock(RestClient.class);
        GeoLocationService service = new GeoLocationService(restClient);
        AtomicReference<String> result = new AtomicReference<>();

        service.resolveAsync("127.0.0.1", result::set);

        assertThat(result.get()).isEqualTo(GeoLocationService.LOCAL_LABEL);
        verifyNoInteractions(restClient);
    }

    @Test
    void resolveAsyncSwallowsRestClientExceptionWithoutCallingConsumer() {
        RestClient restClient = mock(RestClient.class);
        doThrow(new ResourceAccessException("timeout")).when(restClient).get();
        GeoLocationService service = new GeoLocationService(restClient);
        AtomicReference<String> result = new AtomicReference<>();

        assertThatCode(() -> service.resolveAsync("8.8.8.8", result::set)).doesNotThrowAnyException();
        assertThat(result.get()).isNull();
    }

    @Test
    void isPrivateOrLoopbackTrueForLoopbackAndPrivateRanges() {
        assertThat(GeoLocationService.isPrivateOrLoopback("127.0.0.1")).isTrue();
        assertThat(GeoLocationService.isPrivateOrLoopback("::1")).isTrue();
        assertThat(GeoLocationService.isPrivateOrLoopback("192.168.1.10")).isTrue();
        assertThat(GeoLocationService.isPrivateOrLoopback("10.0.0.5")).isTrue();
        assertThat(GeoLocationService.isPrivateOrLoopback("172.16.4.4")).isTrue();
    }

    @Test
    void isPrivateOrLoopbackFalseForPublicIp() {
        assertThat(GeoLocationService.isPrivateOrLoopback("8.8.8.8")).isFalse();
    }

    @Test
    void isPrivateOrLoopbackTrueForUnparsableInput() {
        assertThat(GeoLocationService.isPrivateOrLoopback("not-an-ip-address")).isTrue();
    }

    @Test
    void formatLocationCombinesCityAndCountry() {
        assertThat(GeoLocationService.formatLocation(new GeoLocationService.GeoLookupResponse("success", "Cluj-Napoca", "Romania")))
                .isEqualTo("Cluj-Napoca, Romania");
    }

    @Test
    void formatLocationFallsBackToCountryWhenCityMissing() {
        assertThat(GeoLocationService.formatLocation(new GeoLocationService.GeoLookupResponse("success", "", "Romania")))
                .isEqualTo("Romania");
    }

    @Test
    void formatLocationFallsBackToCityWhenCountryMissing() {
        assertThat(GeoLocationService.formatLocation(new GeoLocationService.GeoLookupResponse("success", "Cluj-Napoca", null)))
                .isEqualTo("Cluj-Napoca");
    }

    @Test
    void formatLocationReturnsNullWhenStatusIsNotSuccess() {
        assertThat(GeoLocationService.formatLocation(new GeoLocationService.GeoLookupResponse("fail", "Cluj-Napoca", "Romania"))).isNull();
    }

    @Test
    void formatLocationReturnsNullForNullResponse() {
        assertThat(GeoLocationService.formatLocation(null)).isNull();
    }
}
