package co.com.pragma.sqs.listener;

import co.com.pragma.model.logs.gateways.LoggerPort;
import co.com.pragma.model.metric.Metric;
import co.com.pragma.sqs.listener.dto.MetricDTO;
import co.com.pragma.usecase.metric.MetricUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.UncheckedIOException;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SQSProcessorTest {

    @Mock
    private MetricUseCase metricUseCase;

    @Mock
    private LoggerPort logger;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SQSProcessor sqsProcessor;

    @Test
    void apply_whenMessageIsValid_shouldProcessAndSaveMetric() throws JsonProcessingException {
        // --- Arrange ---
        String validJson = "{\"name\":\"test-metric\",\"value\":123.45}";
        Message message = Message.builder().body(validJson).build();

        MetricDTO metricDTO = new MetricDTO("test-metric", new BigDecimal("123.45"));
        Metric metric = Metric.builder().name("test-metric").value(new BigDecimal("123.45")).build();

        // Mock dependencies
        when(objectMapper.readValue(validJson, MetricDTO.class)).thenReturn(metricDTO);
        when(metricUseCase.saveMetric(any(Metric.class))).thenReturn(Mono.just(metric));

        // --- Act ---
        Mono<Void> result = sqsProcessor.apply(message);

        // --- Assert ---
        StepVerifier.create(result)
                .verifyComplete();

        // Verify interactions
        verify(logger).info("Processing message {}", validJson);
        verify(metricUseCase).saveMetric(argThat(m ->
                m.getName().equals("test-metric") && m.getValue().compareTo(new BigDecimal("123.45")) == 0
        ));
    }

    @Test
    void apply_whenJsonIsInvalid_shouldReturnErrorAndLog() throws JsonProcessingException {
        // --- Arrange ---
        String invalidJson = "{\"name\":\"test-metric\""; // Malformed JSON
        Message message = Message.builder().body(invalidJson).build();

        JsonProcessingException jsonException = new JsonProcessingException("Parsing error") {};
        when(objectMapper.readValue(invalidJson, MetricDTO.class)).thenThrow(jsonException);

        // --- Act ---
        Mono<Void> result = sqsProcessor.apply(message);

        // --- Assert ---
        StepVerifier.create(result)
                .expectError(UncheckedIOException.class)
                .verify();

        // Verify interactions
        verify(logger).info("Processing message {}", invalidJson);
        verify(logger).error("Error parsing message body: {}", invalidJson, jsonException);
        verify(metricUseCase, never()).saveMetric(any());
    }

    @Test
    void apply_whenSaveMetricFails_shouldReturnError() throws JsonProcessingException {
        // --- Arrange ---
        String validJson = "{\"name\":\"test-metric\",\"value\":123.45}";
        Message message = Message.builder().body(validJson).build();

        MetricDTO metricDTO = new MetricDTO("test-metric", new BigDecimal("123.45"));
        RuntimeException dbException = new RuntimeException("Database is down");

        when(objectMapper.readValue(validJson, MetricDTO.class)).thenReturn(metricDTO);
        when(metricUseCase.saveMetric(any(Metric.class))).thenReturn(Mono.error(dbException));

        // --- Act ---
        Mono<Void> result = sqsProcessor.apply(message);

        // --- Assert ---
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable == dbException)
                .verify();

        // Verify interactions
        verify(logger).info("Processing message {}", validJson);
        verify(metricUseCase).saveMetric(any(Metric.class));
    }
}