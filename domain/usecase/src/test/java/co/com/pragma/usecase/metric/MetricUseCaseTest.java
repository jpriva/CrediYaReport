package co.com.pragma.usecase.metric;

import co.com.pragma.model.constants.Metrics;
import co.com.pragma.model.exceptions.InvalidPathVariableException;
import co.com.pragma.model.logs.gateways.LoggerPort;
import co.com.pragma.model.metric.Metric;
import co.com.pragma.model.metric.gateways.MetricRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricUseCaseTest {

    @Mock
    private MetricRepository repository;

    @Mock
    private LoggerPort logger;

    @InjectMocks
    private MetricUseCase metricUseCase;

    @Test
    void saveMetric_whenMetricIsNew_shouldSaveWithValue() {
        // Arrange
        Metric newMetric = Metric.builder().name("new_metric").value(new BigDecimal("10")).build();
        when(repository.getMetric("new_metric")).thenReturn(Mono.empty());
        when(repository.saveMetric(any(Metric.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act & Assert
        StepVerifier.create(metricUseCase.saveMetric(newMetric))
                .expectNextMatches(saved -> saved.getName().equals("new_metric") && saved.getValue().compareTo(new BigDecimal("10")) == 0)
                .verifyComplete();

        verify(repository).getMetric("new_metric");
        verify(repository).saveMetric(any(Metric.class));
    }

    @Test
    void saveMetric_whenMetricExists_shouldUpdateValue() {
        // Arrange
        Metric incomingMetric = Metric.builder().name("existing_metric").value(new BigDecimal("5")).build();
        Metric existingMetricInDb = Metric.builder().name("existing_metric").value(new BigDecimal("10")).build();

        when(repository.getMetric("existing_metric")).thenReturn(Mono.just(existingMetricInDb));
        when(repository.saveMetric(any(Metric.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act & Assert
        StepVerifier.create(metricUseCase.saveMetric(incomingMetric))
                .expectNextMatches(saved -> saved.getValue().compareTo(new BigDecimal("15")) == 0)
                .verifyComplete();

        verify(repository).getMetric("existing_metric");
        verify(repository).saveMetric(argThat(m -> m.getValue().compareTo(new BigDecimal("15")) == 0));
    }

    @Test
    void saveMetric_whenRepositoryFails_shouldReturnError() {
        // Arrange
        Metric metric = Metric.builder().name("any_metric").value(BigDecimal.ONE).build();
        when(repository.getMetric(anyString())).thenReturn(Mono.error(new RuntimeException("DB Error")));

        // Act & Assert
        StepVerifier.create(metricUseCase.saveMetric(metric))
                .expectError(RuntimeException.class)
                .verify();

        verify(logger).error(eq("Error saving metric"), any(RuntimeException.class));
    }

    @Test
    void getMetric_whenNameIsValid_shouldReturnMetric() {
        // Arrange
        Metric metric = Metric.builder().name(Metrics.QUANTITY_METRIC).value(BigDecimal.TEN).build();
        when(repository.getMetric(Metrics.QUANTITY_METRIC)).thenReturn(Mono.just(metric));

        // Act & Assert
        StepVerifier.create(metricUseCase.getMetric(Metrics.QUANTITY_METRIC))
                .expectNext(metric)
                .verifyComplete();

        verify(logger).info("Metric retrieved {}", metric);
    }

    @Test
    void getMetric_whenMetricNotFound_shouldReturnEmpty() {
        // Arrange
        when(repository.getMetric(Metrics.QUANTITY_METRIC)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(metricUseCase.getMetric(Metrics.QUANTITY_METRIC))
                .verifyComplete();

        verify(logger, never()).info(anyString(), any(Metric.class));
    }

    @Test
    void getMetric_whenNameIsInvalid_shouldReturnError() {
        // Arrange
        String invalidName = "invalid_metric_name";

        // Act & Assert
        StepVerifier.create(metricUseCase.getMetric(invalidName))
                .expectError(InvalidPathVariableException.class)
                .verify();

        verify(repository, never()).getMetric(anyString());
        verify(logger).error(eq("Error getting metric"), any(InvalidPathVariableException.class));
    }

    @Test
    void getMetric_whenNameIsNull_shouldReturnError() {
        // Act & Assert
        StepVerifier.create(metricUseCase.getMetric(null))
                .expectError(InvalidPathVariableException.class)
                .verify();

        verify(repository, never()).getMetric(anyString());
    }

    @Test
    void getMetric_whenNameIsBlank_shouldReturnError() {
        // Act & Assert
        StepVerifier.create(metricUseCase.getMetric("   "))
                .expectError(InvalidPathVariableException.class)
                .verify();

        verify(repository, never()).getMetric(anyString());
    }
}