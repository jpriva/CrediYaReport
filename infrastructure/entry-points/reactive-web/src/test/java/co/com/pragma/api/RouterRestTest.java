package co.com.pragma.api;

import co.com.pragma.api.config.WebSecurityConfig;
import co.com.pragma.api.dto.MetricApiDTO;
import co.com.pragma.api.exception.handler.CustomAccessDeniedHandler;
import co.com.pragma.api.exception.handler.CustomAuthenticationEntryPoint;
import co.com.pragma.api.exception.handler.GlobalExceptionHandler;
import co.com.pragma.api.mapper.MetricMapper;
import co.com.pragma.model.constants.Errors;
import co.com.pragma.model.constants.Metrics;
import co.com.pragma.model.jwt.JwtData;
import co.com.pragma.model.jwt.gateways.JwtProviderPort;
import co.com.pragma.model.logs.gateways.LoggerPort;
import co.com.pragma.model.metric.Metric;
import co.com.pragma.usecase.metric.MetricUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static co.com.pragma.api.constants.ApiConstants.ApiPaths.REPORT_BY_NAME_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
        RouterRest.class, Handler.class,
        GlobalExceptionHandler.class, WebSecurityConfig.class,
        CustomAccessDeniedHandler.class, CustomAuthenticationEntryPoint.class
})
@WebFluxTest(controllers = GlobalExceptionHandler.class)
class RouterRestTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private MetricUseCase metricUseCase;

    @MockitoBean
    private MetricMapper metricMapper;

    @MockitoBean
    private JwtProviderPort jwtProvider;

    @MockitoBean
    private LoggerPort logger;

    private Metric metricDomain;
    private MetricApiDTO metricApiDTO;
    private final String metricName = Metrics.QUANTITY_METRIC;

    @BeforeEach
    void setUp() {
        metricDomain = Metric.builder().name(metricName).value(new BigDecimal("150.75")).build();
        metricApiDTO = MetricApiDTO.builder().name(metricName).value(new BigDecimal("150.75")).build();
    }

    @Test
    void getMetric_whenAdmin_shouldReturnOkAndMetricDto() {

        JwtData jwtData = new JwtData("test@example.com", "ADMIN", 1, "Test", "12345");

        when(jwtProvider.getClaims(anyString())).thenReturn(jwtData);
        when(metricUseCase.getMetric(metricName)).thenReturn(Mono.just(metricDomain));
        when(metricMapper.toMetricApiDTO(metricDomain)).thenReturn(metricApiDTO);

        webTestClient.get()
                .uri(REPORT_BY_NAME_PATH, metricName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer dummy-token")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(MetricApiDTO.class)
                .isEqualTo(metricApiDTO);
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void getMetric_whenNotAdmin_shouldReturnForbidden() {
        JwtData jwtData = new JwtData("test@example.com", "CLIENTE", 1, "Test", "12345");

        when(jwtProvider.getClaims(anyString())).thenReturn(jwtData);
        // --- Act & Assert ---
        webTestClient.get()
                .uri(REPORT_BY_NAME_PATH, metricName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer dummy-token")
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.code").isEqualTo(Errors.ACCESS_DENIED_CODE)
                .jsonPath("$.message").isEqualTo(Errors.ACCESS_DENIED);

        // Verify that the business logic was never called
        Mockito.verify(metricUseCase, Mockito.never()).getMetric(any());
    }

    @Test
    void getMetric_whenUnauthenticated_shouldReturnUnauthorized() {
        // --- Act & Assert ---
        webTestClient.get()
                .uri(REPORT_BY_NAME_PATH, metricName)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo(Errors.INVALID_CREDENTIALS_CODE)
                .jsonPath("$.message").isEqualTo(Errors.INVALID_CREDENTIALS);

        // Verify that the business logic was never called
        Mockito.verify(metricUseCase, Mockito.never()).getMetric(any());
    }

    @Test
    void getMetric_whenAdminAndMetricNotFound_shouldReturnNotFound() {
        String nonExistentMetric = "non-existent";
        JwtData jwtData = new JwtData("test@example.com", "ADMIN", 1, "Test", "12345");

        when(jwtProvider.getClaims(anyString())).thenReturn(jwtData);
        when(metricUseCase.getMetric(nonExistentMetric)).thenReturn(Mono.empty());

        // --- Act & Assert ---
        webTestClient.get()
                .uri(REPORT_BY_NAME_PATH, nonExistentMetric)
                .header(HttpHeaders.AUTHORIZATION, "Bearer dummy-token")
                .exchange()
                .expectStatus().isNotFound();
    }
}
