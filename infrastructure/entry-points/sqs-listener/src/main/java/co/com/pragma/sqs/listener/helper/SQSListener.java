package co.com.pragma.sqs.listener.helper;

import co.com.pragma.model.logs.gateways.LoggerPort;
import co.com.pragma.sqs.listener.config.SQSProperties;
import lombok.Builder;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

@Builder
public class SQSListener {
    private final SqsAsyncClient client;
    private final SQSProperties properties;
    private final Function<Message, Mono<Void>> processor;
    private final LoggerPort logger;
    private String operation;
    private volatile boolean running = true;
    private ExecutorService executorService;
    private Disposable subscription;

    public SQSListener start() {
        this.operation = "MessageFrom:" + properties.queueUrl();
        this.executorService = Executors.newFixedThreadPool(properties.numberOfThreads());
        Flux<Void> flow = listenRetryRepeat().publishOn(Schedulers.fromExecutorService(this.executorService));

        Disposable.Composite compositeDisposable = Disposables.composite();
        for (var i = 0; i < properties.numberOfThreads(); i++) {
            compositeDisposable.add(flow.subscribe());
        }
        this.subscription = compositeDisposable;
        logger.info("SQS Listener started for queue: {}", properties.queueUrl());
        return this;
    }

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

    private Flux<Void> listenRetryRepeat() {
        return listen()
                .doOnError(e -> logger.error("Error listening sqs queue", e))
                .repeat(this::isRunning);
    }

    private Flux<Void> listen() {
        return getMessages()
                .flatMap(message -> processor.apply(message)
                        .name("async_operation")
                        .tag("operation", operation)
                        .metrics()
                        .then(confirm(message)))
                .onErrorContinue((e, o) -> logger.error("Error listening sqs message", e));
    }

    private Mono<Void> confirm(Message message) {
        return Mono.fromCallable(() -> getDeleteMessageRequest(message.receiptHandle()))
                .flatMap(request -> Mono.fromFuture(client.deleteMessage(request)))
                .then();
    }

    private Flux<Message> getMessages() {
        return Mono.fromCallable(this::getReceiveMessageRequest)
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
