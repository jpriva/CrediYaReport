package co.com.pragma.sqs.listener.helper;

import co.com.pragma.model.logs.gateways.LoggerPort;
import co.com.pragma.sqs.listener.config.SQSProperties;
import lombok.Builder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

@Builder(toBuilder = true)
public class SQSListener {
    private final SqsAsyncClient client;
    private final SQSProperties properties;
    private final Function<Message, Mono<Void>> processor;
    private final LoggerPort logger;
    private String operation;
    private volatile boolean running;
    private ExecutorService executorService;
    private Disposable subscription;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        running = true;

        this.operation = "MessageFrom:" + properties.queueUrl();
        this.executorService = Executors.newFixedThreadPool(properties.numberOfThreads());
        Flux<Void> flow = listenRetryRepeat()
                .doOnCancel(() -> logger.info("SQS subscription for queue {} is being cancelled/disposed.", properties.queueUrl()))
                .publishOn(Schedulers.fromExecutorService(this.executorService));

        Disposable.Composite compositeDisposable = Disposables.composite();
        for (var i = 0; i < properties.numberOfThreads(); i++) {
            compositeDisposable.add(flow.subscribe(
                null,
                error -> {
                    if (error instanceof InterruptedException) {
                        logger.debug("SQS listener polling stopped intentionally as part of shutdown process.");
                    } else {
                        logger.error("SQS listener subscription terminated with an unexpected error.", error);
                    }
                }
            ));
        }
        this.subscription = compositeDisposable;
        logger.info("SQS Listener started for queue: {}", properties.queueUrl());
    }

    @EventListener(ContextClosedEvent.class)
    public void stop() {
        logger.info("Stopping SQS Listener for queue: {}", properties.queueUrl());
        if (running) {
            running = false;
            if (subscription != null && !subscription.isDisposed()) {
                subscription.dispose();
            }
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
            logger.info("SQS Listener stopped.");
        }
    }

    private boolean isRunning() {
        return this.running;
    }

    Flux<Void> listenRetryRepeat() {
        return listen()
            .doOnError(e -> logger.error("An unrecoverable error occurred in the SQS listener loop. Restarting poll.", e))
            .repeatWhen(completedSignalFlux -> completedSignalFlux
                .concatMap(ignored -> {
                    if (!isRunning()) {
                        return Mono.error(new InterruptedException("Listener has been stopped."));
                    }
                    logger.debug("SQS Polling Cycle: Cycle complete. Waiting before next poll...");
                    return Mono.delay(Duration.ofMillis(1000));
                }));
    }

    Flux<Void> listen() {
        return getMessages()
                .doOnSubscribe(s -> logger.debug("SQS Batch: Subscribed to process a new batch of messages."))
                .flatMap(message -> processor.apply(message)
                        .name("async_operation")
                        .tag("operation", operation)
                        .metrics()
                        .then(confirm(message))
                        .onErrorResume(error -> {
                            logger.error("SQS Batch: Failed to process message [id={}]. It will be re-processed after visibility timeout. Error: {}",
                                    message.messageId(), error.getMessage(), error);
                            return Mono.empty();
                        })
                );
    }

    Mono<Void> confirm(Message message) {
        return Mono.fromCallable(() -> getDeleteMessageRequest(message.receiptHandle()))
                .doOnNext(req -> logger.debug("SQS Confirm: Attempting to delete message [id={}]", message.messageId()))
                .flatMap(request -> Mono.fromFuture(client.deleteMessage(request)))
                .doOnError(e -> logger.error("SQS Confirm: Failed to delete message [id={}]. It will be reprocessed. Error: {}",
                        message.messageId(), e.getMessage(), e))
                .then();
    }

    Flux<Message> getMessages() {
        return Mono.fromCallable(this::getReceiveMessageRequest)
                .doOnNext(req -> logger.debug("SQS Receive: Sending request to SQS with waitTime: {}s, maxMessages: {}",
                        req.waitTimeSeconds(), req.maxNumberOfMessages()))
                .flatMap(request -> Mono.fromFuture(client.receiveMessage(request)))
                .doOnNext(response -> logger.debug("{} received messages from sqs", response.messages().size()))
                .flatMapMany(response -> Flux.fromIterable(response.messages()));
    }

    private ReceiveMessageRequest getReceiveMessageRequest() {
        return ReceiveMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .maxNumberOfMessages(properties.maxNumberOfMessages())
                .waitTimeSeconds(properties.waitTimeSeconds())
                .visibilityTimeout(properties.visibilityTimeoutSeconds())
                .build();
    }

    private DeleteMessageRequest getDeleteMessageRequest(String receiptHandle) {
        return DeleteMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .receiptHandle(receiptHandle)
                .build();
    }
}
