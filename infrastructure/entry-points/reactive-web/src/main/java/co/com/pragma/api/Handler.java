package co.com.pragma.api;

import co.com.pragma.api.mapper.MetricMapper;
import co.com.pragma.usecase.metric.MetricUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class Handler {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final MetricUseCase metricUseCase;
    private final MetricMapper metricMapper;

    public Mono<ServerResponse> listenGETMetricUseCase(ServerRequest serverRequest) {
        String metricName = serverRequest.pathVariable("metric");
        return Mono.just(metricName)
                .flatMap(metricUseCase::getMetric)
                .map(metricMapper::toMetricApiDTO)
                .flatMap(metric ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(metric)
                )
                .switchIfEmpty(ServerResponse.notFound().build());
    }
}
