package co.com.pragma.dynamodb.helper;

import co.com.pragma.dynamodb.DynamoDBTemplateAdapter;
import co.com.pragma.dynamodb.MetricEntity;
import co.com.pragma.model.metric.Metric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.reactivecommons.utils.ObjectMapper;
import org.reactivestreams.Subscription;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TemplateAdapterOperationsTest {

    @Mock
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private DynamoDbAsyncTable<MetricEntity> table;

    @Mock
    private DynamoDbAsyncIndex<MetricEntity> index;

    private MetricEntity metricEntity;
    private Metric metric;
    private DynamoDBTemplateAdapter dynamoDBTemplateAdapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Corregir el nombre de la tabla para que coincida con la implementación
        when(dynamoDbEnhancedAsyncClient.table("reporte_aprobados", TableSchema.fromBean(MetricEntity.class)))
                .thenReturn(table);
        when(table.index("some-index")).thenReturn(index);

        // Crear una única instancia del adaptador para todas las pruebas
        dynamoDBTemplateAdapter = new DynamoDBTemplateAdapter(dynamoDbEnhancedAsyncClient, mapper);

        // Configurar objetos de dominio y entidad para las pruebas
        metric = Metric.builder().name("test-metric").value(new BigDecimal("123.45")).build();
        metricEntity = new MetricEntity("test-metric", new BigDecimal("123.45"));

        // Configurar los mocks del ObjectMapper para la conversión
        when(mapper.map(metric, MetricEntity.class)).thenReturn(metricEntity);
        when(mapper.map(metricEntity, Metric.class)).thenReturn(metric);
    }

    @Test
    void modelEntityPropertiesMustNotBeNull() {
        MetricEntity metricEntityUnderTest = new MetricEntity("id", new BigDecimal("1.00"));

        assertNotNull(metricEntityUnderTest.getName());
        assertNotNull(metricEntityUnderTest.getValue());
    }

    @Test
    void testSave() {
        // Arrange: Simular que la operación putItem se completa exitosamente
        when(table.putItem(metricEntity)).thenReturn(CompletableFuture.completedFuture(null));

        // Act & Assert: Verificar que saveMetric devuelve la métrica original al completarse
        StepVerifier.create(dynamoDBTemplateAdapter.save(metric))
                .expectNext(metric)
                .verifyComplete();
    }

    @Test
    void testGetById() {
        // Arrange: Simular que getItem devuelve nuestra entidad de ejemplo
        when(table.getItem(any(Key.class)))
                .thenReturn(CompletableFuture.completedFuture(metricEntity));

        // Act & Assert: Verificar que getById devuelve el objeto de dominio correctamente mapeado
        StepVerifier.create(dynamoDBTemplateAdapter.getById("id"))
                .expectNext(metric)
                .verifyComplete();
    }

    @Test
    void testDelete() {
        // Arrange: Simular que deleteItem devuelve la entidad eliminada
        when(table.deleteItem(any(MetricEntity.class)))
                .thenReturn(CompletableFuture.completedFuture(metricEntity));

        // Act & Assert: Verificar que delete devuelve el objeto de dominio correctamente mapeado
        StepVerifier.create(dynamoDBTemplateAdapter.delete(metric))
                .expectNext(metric)
                .verifyComplete();
    }

    @Test
    void testQuery() {
        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder().build();
        Page<MetricEntity> page = Page.builder(MetricEntity.class)
                .items(List.of(metricEntity))
                .build();

        PagePublisher<MetricEntity> pagePublisher = s -> s.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                if (n > 0) {
                    s.onNext(page);
                    s.onComplete();
                }
            }

            @Override
            public void cancel() {
                //not implemented
            }
        });

        when(table.query(any(QueryEnhancedRequest.class))).thenReturn(pagePublisher);

        StepVerifier.create(dynamoDBTemplateAdapter.query(queryEnhancedRequest))
                .expectNext(List.of(metric))
                .verifyComplete();
    }


    @Test
    void testQueryByIndex() {
        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder().build();
        Page<MetricEntity> page = Page.builder(MetricEntity.class)
                .items(List.of(metricEntity))
                .build();

        SdkPublisher<Page<MetricEntity>> mockPublisher = subscriber -> {
            subscriber.onSubscribe(new Subscription() {
                private boolean completed = false;

                @Override
                public void request(long n) {
                    if (n > 0 && !completed) {
                        completed = true;
                        subscriber.onNext(page);
                        subscriber.onComplete();
                    }
                }

                @Override
                public void cancel() {
                    completed = true;
                }
            });
        };
        when(index.query(any(QueryEnhancedRequest.class))).thenReturn(mockPublisher);

        StepVerifier.create(dynamoDBTemplateAdapter.queryByIndex(queryEnhancedRequest, "some-index")) // <-- Pasa el nombre del índice aquí
                .expectNext(List.of(metric))
                .verifyComplete();
    }
}