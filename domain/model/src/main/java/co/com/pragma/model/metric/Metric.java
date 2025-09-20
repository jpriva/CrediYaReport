package co.com.pragma.model.metric;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
public class Metric {
    private String name;
    private BigDecimal value;
}
