
package com.kienlongbank.classroomservice.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.kienlongbank.classroomservice.config.WireMockConfig;
import org.junit.jupiter.api.AfterEach;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.beans.factory.annotation.Value;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.kienlongbank.classroomservice.config.BaseTestConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({WireMockExampleTest.WireMockConfiguration.class, WireMockConfig.class})
@ActiveProfiles("test")
public class WireMockExampleTest {

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
private WireMockConfig wireMockConfig;

    @Autowired
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        wireMockServer.start();
        wireMockServer.resetAll();
    }

    @Test
    void testSuccessfulResponse() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo("/api/test"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"message\":\"success\"}")));

        // When
        String response = restTemplate.getForObject(wireMockServer.baseUrl() + "/api/test", String.class);

        // Then
        assertNotNull(response);
        assertEquals("{\"message\":\"success\"}", response);
    }

    @Test
    void testNotFoundResponse() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo("/api/notfound"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())));

        // When & Then
        try {
            restTemplate.getForObject(wireMockServer.baseUrl() + "/api/notfound", String.class);
        } catch (Exception e) {
            assertEquals(404, ((org.springframework.web.client.HttpClientErrorException) e).getRawStatusCode());
        }
    }

    @Test
    void testServerErrorResponse() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo("/api/error"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        // When & Then
        try {
            restTemplate.getForObject(wireMockServer.baseUrl() + "/api/error", String.class);
        } catch (Exception e) {
            assertEquals(500, ((org.springframework.web.client.HttpServerErrorException) e).getRawStatusCode());
        }
    }

    @Test
    void testDelayedResponse() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo("/api/delayed"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withFixedDelay(2000)));

        // When
        long startTime = System.currentTimeMillis();
        restTemplate.getForObject(wireMockServer.baseUrl() + "/api/delayed", String.class);
        long endTime = System.currentTimeMillis();

        // Then
        long duration = endTime - startTime;
        assert(duration >= 2000);
    }
    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }
    @Test
    void testRequestWithHeaders() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo("/api/secured"))
                .withHeader("Authorization", matching("Bearer .*"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody("{\"message\":\"authorized\"}")));

        wireMockServer.stubFor(get(urlEqualTo("/api/secured"))
                .withHeader("Authorization", absent())
                .willReturn(aResponse()
                        .withStatus(HttpStatus.UNAUTHORIZED.value())));

        // When & Then - With Authorization
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer test-token");
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(null, headers);
        
        String response = restTemplate.exchange(
                wireMockServer.baseUrl() + "/api/secured",
                org.springframework.http.HttpMethod.GET,
                entity,
                String.class).getBody();

        assertNotNull(response);
        assertEquals("{\"message\":\"authorized\"}", response);

        // When & Then - Without Authorization
        try {
            restTemplate.getForObject(wireMockServer.baseUrl() + "/api/secured", String.class);
        } catch (Exception e) {
            assertEquals(401, ((org.springframework.web.client.HttpClientErrorException) e).getRawStatusCode());
        }
}

@Configuration
static class WireMockConfiguration {
    @Bean
    public WireMockServer wireMockServer() {
        return new WireMockServer();
    }
}
}


