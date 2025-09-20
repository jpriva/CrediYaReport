package co.com.pragma.dynamodb;

import co.com.pragma.dynamodb.helper.TemplateAdapterOperations;
import co.com.pragma.model.metric.Metric;
import co.com.pragma.model.metric.gateways.MetricRepository;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;


@Repository
public class DynamoDBTemplateAdapter extends TemplateAdapterOperations<Metric, String, MetricEntity> implements MetricRepository {

    public DynamoDBTemplateAdapter(DynamoDbEnhancedAsyncClient connectionFactory, ObjectMapper mapper) {
        super(connectionFactory, mapper, d -> mapper.map(d, Metric.class), "reporte_aprobados");
    }

    @Override
    public Mono<Metric> saveMetric(Metric metric) {
        return save(metric);
    }

    @Override
    public Mono<Metric> getMetric(String name) {
        return getById(name);
    }
}
