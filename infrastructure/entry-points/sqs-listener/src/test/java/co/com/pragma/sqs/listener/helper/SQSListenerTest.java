package co.com.pragma.sqs.listener.helper;

import co.com.pragma.model.logs.gateways.LoggerPort;
import co.com.pragma.sqs.listener.config.SQSProperties;
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
import software.amazon.awssdk.services.sqs.model.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
        assertEquals(false, ReflectionTestUtils.getField(sqsListener, "running"));
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

        var sqsListener = SQSListener.builder().client(asyncClient).properties(sqsProperties).logger(logger).build();

        // --- Act ---
        Mono<Void> confirmFlow = sqsListener.confirm(message);

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

        var sqsListener = SQSListener.builder().client(asyncClient).properties(sqsProperties).logger(logger).build();

        // --- Act ---
        Mono<Void> confirmFlow = sqsListener.confirm(message);

        // --- Assert ---
        StepVerifier.create(confirmFlow)
                .expectErrorMatches(err -> err == exception)
                .verify();
    }

    @Test
    void getMessages_shouldBuildCorrectRequestAndReturnMessages() {
        // --- Arrange ---
        when(sqsProperties.queueUrl()).thenReturn("http://test-queue");
        when(sqsProperties.maxNumberOfMessages()).thenReturn(5);
        when(sqsProperties.waitTimeSeconds()).thenReturn(10);
        when(sqsProperties.visibilityTimeoutSeconds()).thenReturn(30);

        Message message1 = Message.builder().body("msg1").build();
        Message message2 = Message.builder().body("msg2").build();
        ReceiveMessageResponse response = ReceiveMessageResponse.builder().messages(message1, message2).build();
        when(asyncClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        var sqsListener = SQSListener.builder()
                .client(asyncClient)
                .properties(sqsProperties)
                .logger(logger)
                .build();

        // --- Act ---
        Flux<Message> messagesFlux = sqsListener.getMessages();

        // --- Assert ---
        StepVerifier.create(messagesFlux)
                .expectNext(message1, message2)
                .verifyComplete();

        ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(asyncClient).receiveMessage(captor.capture());
        ReceiveMessageRequest capturedRequest = captor.getValue();

        assertEquals("http://test-queue", capturedRequest.queueUrl());
        assertEquals(5, capturedRequest.maxNumberOfMessages());
        assertEquals(10, capturedRequest.waitTimeSeconds());
        assertEquals(30, capturedRequest.visibilityTimeout());
    }

    @Test
    void listen_whenMessagesReceived_shouldProcessAndConfirm() {
        // --- Arrange ---
        String queueUrl = "http://test-queue";
        Message testMessage = Message.builder().body("test body").receiptHandle("test-receipt").build();
        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder().messages(testMessage).build();
        DeleteMessageResponse deleteResponse = DeleteMessageResponse.builder().build();

        when(sqsProperties.queueUrl()).thenReturn(queueUrl);
        when(asyncClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveResponse));
        when(asyncClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(deleteResponse));

        Function<Message, Mono<Void>> processorMock = mock(Function.class);
        when(processorMock.apply(any(Message.class))).thenReturn(Mono.empty());

        var sqsListener = SQSListener.builder()
                .client(asyncClient)
                .properties(sqsProperties)
                .processor(processorMock)
                .logger(logger)
                .build();

        // Manually set the 'operation' field, which is normally initialized in the start() method.
        ReflectionTestUtils.setField(sqsListener, "operation", "test-operation");

        // --- Act ---
        Flux<Void> listenFlow = sqsListener.listen();

        // --- Assert ---
        StepVerifier.create(listenFlow).verifyComplete();
        verify(processorMock).apply(testMessage);
        verify(asyncClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void listen_whenProcessorFails_shouldLogErrorAndNotConfirm() {
        // --- Arrange ---
        Message testMessage = Message.builder().messageId("msg-123").receiptHandle("test-receipt").build();
        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder().messages(testMessage).build();
        RuntimeException processorException = new RuntimeException("Processing failed!");

        when(asyncClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(receiveResponse));

        Function<Message, Mono<Void>> processorMock = mock(Function.class);
        when(processorMock.apply(any(Message.class))).thenReturn(Mono.error(processorException));

        var sqsListener = SQSListener.builder()
                .client(asyncClient)
                .properties(sqsProperties)
                .processor(processorMock)
                .logger(logger)
                .build();

        // Manually set the 'operation' field for the metrics tag.
        ReflectionTestUtils.setField(sqsListener, "operation", "test-operation");

        // --- Act ---
        Flux<Void> listenFlow = sqsListener.listen();

        // --- Assert ---
        StepVerifier.create(listenFlow).verifyComplete();
        verify(logger).error(anyString(), eq("msg-123"), eq("Processing failed!"), eq(processorException));
        verify(asyncClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void start_shouldInitializeAndSubscribeCorrectly() {
        // --- Arrange ---
        when(sqsProperties.queueUrl()).thenReturn("http://test-queue");
        when(sqsProperties.numberOfThreads()).thenReturn(2);

        // Create a spy to partially mock the listener
        SQSListener realListener = SQSListener.builder()
                .client(asyncClient)
                .properties(sqsProperties)
                .processor(mock(Function.class))
                .logger(logger)
                .build();
        SQSListener spyListener = spy(realListener);

        // Mock the infinite loop to return an empty, non-terminating Flux for this test
        doReturn(Flux.never()).when(spyListener).listenRetryRepeat();

        // --- Act ---
        spyListener.start();

        // --- Assert ---
        assertEquals(true, ReflectionTestUtils.getField(spyListener, "running"));
        assertEquals("MessageFrom:http://test-queue", ReflectionTestUtils.getField(spyListener, "operation"));
        verify(logger).info("SQS Listener started for queue: {}", "http://test-queue");

        // Verify that we subscribed 'numberOfThreads' times
        // This is an indirect verification, but we can check that the subscription object is not null
        Object subscription = ReflectionTestUtils.getField(spyListener, "subscription");
        assertEquals(false, ((reactor.core.Disposable) subscription).isDisposed());
    }

    @Test
    void listenRetryRepeat_shouldRepeatWithDelay() {
        // --- Arrange ---
        SQSListener realListener = SQSListener.builder().logger(logger).build();
        SQSListener spyListener = spy(realListener);

        ReflectionTestUtils.setField(spyListener, "running", true);
        doReturn(Flux.empty()).when(spyListener).listen(); // Mock listen() to complete immediately

        // --- Act ---
        Flux<Void> repeatingFlow = spyListener.listenRetryRepeat();

        // --- Assert ---
        // Use virtual time to test the delay without actually waiting
        StepVerifier.withVirtualTime(() -> repeatingFlow.take(2))
                .expectSubscription()
                .then(() -> verify(logger, times(1)).debug("SQS Polling Cycle: Cycle complete. Waiting before next poll..."))
                .thenAwait(Duration.ofMillis(1000)) // Advance time by 1s for the first delay
                .then(() -> verify(logger, times(2)).debug("SQS Polling Cycle: Cycle complete. Waiting before next poll..."))
                .thenAwait(Duration.ofMillis(1000)) // Advance time by 1s for the second delay
                .thenCancel()
                .verify();
    }
}
