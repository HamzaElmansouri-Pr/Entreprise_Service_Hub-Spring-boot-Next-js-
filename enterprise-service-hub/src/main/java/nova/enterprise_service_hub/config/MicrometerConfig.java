package nova.enterprise_service_hub.config;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import nova.enterprise_service_hub.security.TenantContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MicrometerConfig {

    @Bean
    public ObservationFilter tenantObservationFilter() {
        return context -> {
            String tenantId = TenantContext.getTenantId();
            if (tenantId != null && !tenantId.isBlank()) {
                context.addLowCardinalityKeyValue(KeyValue.of("tenant.id", tenantId));
            } else {
                context.addLowCardinalityKeyValue(KeyValue.of("tenant.id", "system"));
            }
            return context;
        };
    }
}
