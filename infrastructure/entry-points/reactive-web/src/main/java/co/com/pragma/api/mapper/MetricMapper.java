package co.com.pragma.api.mapper;

import co.com.pragma.api.dto.MetricApiDTO;
import co.com.pragma.model.metric.Metric;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MetricMapper {
    MetricApiDTO toMetricApiDTO(Metric metric);
}
