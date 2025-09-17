package co.com.pragma.sqs.listener.helper;

import co.com.pragma.model.logs.gateways.LoggerPort;
import co.com.pragma.model.metric.Metric;
import co.com.pragma.sqs.listener.SQSProcessor;
import co.com.pragma.sqs.listener.config.SQSProperties;
import co.com.pragma.sqs.listener.dto.MetricDTO;
import co.com.pragma.usecase.metric.MetricUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SQSListenerTest {

    @Mock
    private SqsAsyncClient asyncClient;

    @Mock
    private SQSProperties sqsProperties;

    @Mock
    private MetricUseCase metricUseCase;

    @Mock
    private LoggerPort logger;

    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        MockitoAnnotations.openMocks(this);

        var message = Message.builder().body("message").build();
        var deleteMessageResponse = DeleteMessageResponse.builder().build();
        var messageResponse = ReceiveMessageResponse.builder().messages(message).build();
        String validJsonBody = "{\"name\":\"test-metric\",\"value\":100}";
        var metricDTO = new MetricDTO("test-metric", BigDecimal.valueOf(100));
        var metric = Metric.builder().name("test-metric").value(BigDecimal.valueOf(100)).build();
        when(objectMapper.readValue(validJsonBody, MetricDTO.class)).thenReturn(metricDTO);
        when(metricUseCase.saveMetric(any(Metric.class))).thenReturn(Mono.just(metric));

        when(asyncClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(messageResponse));
        when(asyncClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(deleteMessageResponse));
    }

    @Test
    void stop_shouldShutdownListenerGracefully() {
        // --- Arrange ---
        // 1. Crear mocks para los componentes internos que `stop` debe gestionar
        ExecutorService executorServiceMock = org.mockito.Mockito.mock(ExecutorService.class);
        reactor.core.Disposable subscriptionMock = org.mockito.Mockito.mock(reactor.core.Disposable.class);

        when(sqsProperties.queueUrl()).thenReturn("http://test-queue");
        var sqsListener = SQSListener.builder().logger(logger).properties(sqsProperties).build();

        // 2. Usar ReflectionTestUtils para inyectar los mocks en el listener
        ReflectionTestUtils.setField(sqsListener, "running", true);
        ReflectionTestUtils.setField(sqsListener, "executorService", executorServiceMock);
        ReflectionTestUtils.setField(sqsListener, "subscription", subscriptionMock);

        // --- Act ---
        sqsListener.stop();

        // --- Assert ---
        // 3. Verificar que el estado y los componentes internos se manejaron correctamente
        assertFalse((Boolean) ReflectionTestUtils.getField(sqsListener, "running"));
        verify(subscriptionMock).dispose();
        verify(executorServiceMock).shutdown();
        verify(logger).info("Stopping SQS Listener for queue: {}", "http://test-queue");
        verify(logger).info("SQS Listener stopped.");
    }

    @Test
    void confirm_shouldDeleteMessageSuccessfully() {
        when(sqsProperties.queueUrl()).thenReturn("http://test-queue");
        // --- Arrange ---
        var message = Message.builder().receiptHandle("test-receipt-handle").build();
        var deleteResponse = DeleteMessageResponse.builder().build();
        when(asyncClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(deleteResponse));

        var sqsListener = SQSListener.builder().client(asyncClient).properties(sqsProperties).build();

        // --- Act ---
        Mono<Void> confirmFlow = ReflectionTestUtils.invokeMethod(sqsListener, "confirm", message);

        // --- Assert ---
        StepVerifier.create(confirmFlow).verifyComplete();

        ArgumentCaptor<DeleteMessageRequest> captor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(asyncClient).deleteMessage(captor.capture());
        assertEquals("test-receipt-handle", captor.getValue().receiptHandle());
        assertEquals("http://test-queue", captor.getValue().queueUrl());
    }

    @Test
    void confirm_whenClientFails_shouldPropagateError() {
        when(sqsProperties.queueUrl()).thenReturn("http://test-queue");
        // --- Arrange ---
        var message = Message.builder().receiptHandle("test-receipt-handle").build();
        var exception = new RuntimeException("AWS Client Error");
        when(asyncClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(exception));

        var sqsListener = SQSListener.builder().client(asyncClient).properties(sqsProperties).build();

        // --- Act ---
        Mono<Void> confirmFlow = ReflectionTestUtils.invokeMethod(sqsListener, "confirm", message);

        // --- Assert ---
        StepVerifier.create(confirmFlow)
                .expectErrorMatches(err -> err == exception)
                .verify();
    }

    @Test
    void listenerTest() {
        var sqsListener = SQSListener.builder()
                .client(asyncClient)
                .properties(sqsProperties)
                .processor(new SQSProcessor(metricUseCase, logger, objectMapper))
                .operation("operation")
                .logger(logger)
                .build();

        Flux<Void> flow = ReflectionTestUtils.invokeMethod(sqsListener, "listen");
        StepVerifier.create(flow).verifyComplete();
    }
}
