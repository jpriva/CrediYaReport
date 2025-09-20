package co.com.pragma.api.mapper;

import co.com.pragma.api.dto.MetricApiDTO;
import co.com.pragma.model.metric.Metric;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MetricMapperTest {

    private final MetricMapper metricMapper = Mappers.getMapper(MetricMapper.class);

    @Test
    void shouldMapMetricToMetricApiDTO() {
        // --- Arrange ---
        // Create a source Metric object
        Metric sourceMetric = Metric.builder()
                .name("test_metric")
                .value(new BigDecimal("123.45"))
                .build();

        // --- Act ---
        // Perform the mapping
        MetricApiDTO resultDTO = metricMapper.toMetricApiDTO(sourceMetric);

        // --- Assert ---
        // Verify that the result is not null and the fields are mapped correctly
        assertNotNull(resultDTO);
        assertEquals(sourceMetric.getName(), resultDTO.getName());
        assertEquals(0, sourceMetric.getValue().compareTo(resultDTO.getValue())); // Use compareTo for BigDecimal
    }

    @Test
    void shouldReturnNullWhenMetricIsNull() {
        // --- Arrange ---
        // Source object is null
        Metric sourceMetric = null;

        // --- Act ---
        // Perform the mapping
        MetricApiDTO resultDTO = metricMapper.toMetricApiDTO(sourceMetric);

        // --- Assert ---
        // Verify that the result is null, as per MapStruct's default behavior
        assertNull(resultDTO);
    }
}