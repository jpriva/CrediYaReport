package co.com.pragma.api.config;

import co.com.pragma.api.Handler;
import co.com.pragma.api.RouterRest;
import co.com.pragma.api.constants.ApiConstants;
import co.com.pragma.api.mapper.MetricMapper;
import co.com.pragma.model.constants.Metrics;
import co.com.pragma.model.jwt.gateways.JwtProviderPort;
import co.com.pragma.model.logs.gateways.LoggerPort;
import co.com.pragma.usecase.metric.MetricUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@ContextConfiguration(classes = {RouterRest.class, Handler.class})
@WebFluxTest
@Import({CorsConfig.class, SecurityHeadersConfig.class})
class ConfigTest {

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

    @Test
    void corsConfigurationShouldAllowOrigins() {
        webTestClient.post()
                .uri(ApiConstants.ApiPaths.REPORT_PATH, Metrics.QUANTITY_METRIC)
                .exchange()
                .expectStatus().isForbidden();
    }

}