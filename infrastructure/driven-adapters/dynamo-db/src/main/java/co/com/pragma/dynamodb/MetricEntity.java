package co.com.pragma.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.math.BigDecimal;

@DynamoDbBean
public class MetricEntity {

    private String name;
    private BigDecimal value;

    public MetricEntity() {
    }

    public MetricEntity(String name, BigDecimal value) {
        this.name = name;
        this.value = value;
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("metrica")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @DynamoDbAttribute("valor")
    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}
