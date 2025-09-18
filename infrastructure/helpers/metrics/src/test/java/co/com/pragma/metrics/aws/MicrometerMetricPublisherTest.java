package co.com.pragma.metrics.aws;

import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.metrics.internal.EmptyMetricCollection;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class MicrometerMetricPublisherTest {

    @Test
    void metricTest() {
        LoggingMeterRegistry loggingMeterRegistry = LoggingMeterRegistry
            .builder(LoggingRegistryConfig.DEFAULT)
            .build();

        MicrometerMetricPublisher micrometerMetricPublisher = new MicrometerMetricPublisher(loggingMeterRegistry);

        micrometerMetricPublisher.publish(EmptyMetricCollection.create());
        micrometerMetricPublisher.close();

        assertNotNull(micrometerMetricPublisher);

    }
}