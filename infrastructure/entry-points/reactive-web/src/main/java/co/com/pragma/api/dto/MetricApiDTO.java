package co.com.pragma.api.dto;

import co.com.pragma.api.constants.ApiConstants;
import co.com.pragma.model.constants.Metrics;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Schema(name = ApiConstants.MetricDoc.METRIC_DTO_NAME, description = ApiConstants.MetricDoc.METRIC_DTO_DESC)
public class MetricApiDTO {

    @Schema(description = ApiConstants.ApiParams.METRIC_NAME_DESC, example = Metrics.QUANTITY_METRIC)
    String name;

    @Schema(description = ApiConstants.MetricDoc.METRIC_VALUE_DESC, example = ApiConstants.MetricDoc.METRIC_VALUE_EXAMPLE)
    BigDecimal value;
}
