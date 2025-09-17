package co.com.pragma.usecase.metric;

import co.com.pragma.model.logs.gateways.LoggerPort;
import co.com.pragma.model.metric.Metric;
import co.com.pragma.model.metric.gateways.MetricRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class MetricUseCase {
    private final MetricRepository repository;
    private final LoggerPort logger;

    public Mono<Metric> saveMetric(Metric metric) {
        logger.info("Saving metric {}", metric);
        return repository.getMetric(metric.getName())
                .switchIfEmpty(Mono.just(Metric.builder().name(metric.getName()).value(BigDecimal.ZERO).build()))
                .map(metricDb -> metricDb.toBuilder().value(metricDb.getValue().add(metric.getValue())).build())
                .flatMap(repository::saveMetric)
                .doOnError(ex -> logger.error("Error saving metric", ex))
                .doOnSuccess(metricDb -> logger.info("Metric saved {}", metricDb));
    }
}
