package co.com.pragma.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class MetricEntity {

    private String name;
    private String value;

    public MetricEntity() {
    }

    public MetricEntity(String name, String value) {
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
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
