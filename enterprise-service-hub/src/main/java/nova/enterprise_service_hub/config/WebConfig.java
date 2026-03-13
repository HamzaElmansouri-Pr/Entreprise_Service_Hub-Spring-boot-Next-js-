package nova.enterprise_service_hub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

/**
 * Web MVC Configuration — Resource handling + i18n locale resolution.
 * <p>
 * Reads the {@code Accept-Language} header to resolve the client locale
 * for localized error messages (EN, AR, FR).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setSupportedLocales(List.of(
                Locale.ENGLISH,
                Locale.FRENCH,
                Locale.forLanguageTag("ar")));
        return resolver;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = "file:" + Paths.get("uploads").toAbsolutePath().toString().replace("\\", "/") + "/";
        
        registry.addResourceHandler("/v1/uploads/**", "/uploads/**")
                .addResourceLocations(uploadPath)
                .setCachePeriod(3600);
    }
}
