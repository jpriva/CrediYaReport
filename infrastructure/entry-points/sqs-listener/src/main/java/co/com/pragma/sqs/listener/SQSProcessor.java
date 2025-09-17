package co.com.pragma.sqs.listener;

import co.com.pragma.model.logs.gateways.LoggerPort;
import co.com.pragma.model.metric.Metric;
import co.com.pragma.sqs.listener.dto.MetricDTO;
import co.com.pragma.usecase.metric.MetricUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.UncheckedIOException;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class SQSProcessor implements Function<Message, Mono<Void>> {
    private final MetricUseCase metricUseCase;
    private final LoggerPort logger;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> apply(Message message) {
        logger.info("Processing message {}", message.body());
        return Mono.just(message.body())
                .flatMap(this::processMessage)
                .flatMap(metricUseCase::saveMetric)
                .then();
    }

    private Mono<Metric> processMessage(String body) {
        try {
            MetricDTO metric = objectMapper.readValue(body, MetricDTO.class);
            return Mono.just(Metric.builder()
                    .name(metric.getName())
                    .value(metric.getValue())
                    .build());
        } catch (JsonProcessingException e) {
            logger.error("Error parsing message body: {}", body, e);
            return Mono.error(new UncheckedIOException(e));
        }
    }
}
