package nova.enterprise_service_hub.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * OpenAI Client Configuration — Builds a pre-configured {@link RestClient}
 * pointed at the OpenAI Chat Completions API.
 * <p>
 * The API key is <strong>never</strong> stored in source code; it is read
 * exclusively from the {@code OPENAI_API_KEY} environment variable
 * (mapped via {@code application.yml}).
 */
@Configuration
public class OpenAiConfig {

    @Value("${ai.openai.api-key}")
    private String apiKey;

    /**
     * Provides a Jackson {@link ObjectMapper} if Spring Boot's auto-configuration
     * does not supply one (e.g. when using {@code spring-boot-starter-webmvc}
     * without {@code spring-boot-starter-json}).
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Bean("openAiRestClient")
    public RestClient openAiRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
