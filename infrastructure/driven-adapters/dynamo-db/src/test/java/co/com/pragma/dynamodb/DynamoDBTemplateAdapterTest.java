package co.com.pragma.dynamodb;

import co.com.pragma.model.metric.Metric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamoDBTemplateAdapterTest {

    @Mock
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private DynamoDbAsyncTable<MetricEntity> table;

    private DynamoDBTemplateAdapter adapter;
    private Metric metric;
    private MetricEntity metricEntity;

    @BeforeEach
    void setUp() {
        // Mock the client to return the mocked table with the correct name
        String tableName = "reporte_aprobados";
        when(dynamoDbEnhancedAsyncClient.table(eq(tableName), any(TableSchema.class))).thenReturn(table);

        // Instantiate the adapter to be tested
        adapter = new DynamoDBTemplateAdapter(dynamoDbEnhancedAsyncClient, mapper);

        // Arrange Test Data
        metric = Metric.builder().name("test-metric").value(new BigDecimal("100.50")).build();
        metricEntity = new MetricEntity("test-metric", new BigDecimal("100.50"));

        // Mock the mapping between domain and entity objects
        lenient().when(mapper.map(metric, MetricEntity.class)).thenReturn(metricEntity);
        lenient().when(mapper.map(metricEntity, Metric.class)).thenReturn(metric);
    }

    @Test
    void saveMetric_shouldDelegateToSaveAndReturnMetric() {
        // Arrange: Mock the underlying save operation to complete successfully
        when(table.putItem(any(MetricEntity.class))).thenReturn(CompletableFuture.completedFuture(null));

        // Act & Assert: Verify that saveMetric returns the original metric
        StepVerifier.create(adapter.saveMetric(metric))
                .expectNext(metric)
                .verifyComplete();

        // Verify that the correct entity was passed to putItem
        verify(table).putItem(metricEntity);
    }

    @Test
    void getMetric_shouldDelegateToGetByIdAndReturnMetric() {
        // Arrange: Mock the underlying get operation to return the entity
        when(table.getItem(any(Key.class))).thenReturn(CompletableFuture.completedFuture(metricEntity));

        // Act & Assert: Verify that getMetric returns the mapped domain object
        StepVerifier.create(adapter.getMetric("test-metric"))
                .expectNext(metric)
                .verifyComplete();

        // Verify that getItem was called with the correct key
        ArgumentCaptor<Key> keyCaptor = ArgumentCaptor.forClass(Key.class);
        verify(table).getItem(keyCaptor.capture());
        assertEquals("test-metric", keyCaptor.getValue().partitionKeyValue().s());
    }

    @Test
    void getMetric_whenNotFound_shouldReturnEmpty() {
        // Arrange: Mock the underlying get operation to return null (not found)
        when(table.getItem(any(Key.class))).thenReturn(CompletableFuture.completedFuture(null));

        // Act & Assert: Verify that getMetric completes without emitting any item
        StepVerifier.create(adapter.getMetric("not-found-metric"))
                .verifyComplete();
    }
}