package co.com.pragma.usecase.metric;

import co.com.pragma.model.metric.Metric;
import co.com.pragma.model.metric.gateways.MetricRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class MetricUseCase {
    private final MetricRepository repository;

    public Mono<Metric> saveMetric(Metric metric) {
        return null;
    }
}
