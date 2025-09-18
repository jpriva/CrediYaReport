package co.com.pragma.model.metric.gateways;

import co.com.pragma.model.metric.Metric;
import reactor.core.publisher.Mono;

public interface MetricRepository {
    Mono<Metric> saveMetric(Metric metric);

    Mono<Metric> getMetric(String name);
}
