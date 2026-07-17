package com.revature.todomanagement.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for CORS policy and request interceptors.
 *
 * <h3>CORS (Cross-Origin Resource Sharing)</h3>
 * <p>Browsers enforce the Same-Origin Policy: JavaScript loaded from one origin (protocol +
 * host + port) cannot make HTTP requests to a different origin unless the server explicitly
 * permits it via CORS headers. In production, the Angular frontend is served from S3
 * (e.g., {@code http://bucket.s3-website.amazonaws.com}) while the API lives on EC2
 * ({@code http://ec2-ip:8080}). These are different origins, so the backend must respond
 * with {@code Access-Control-Allow-Origin} matching the frontend's origin.</p>
 *
 * <h3>Relaxed Binding (Spring Boot)</h3>
 * <p>The {@code @Value("${cors.allowed-origins}")} annotation reads the property
 * {@code cors.allowed-origins} from the Spring Environment. Via Spring Boot relaxed binding,
 * the environment variable {@code CORS_ALLOWED_ORIGINS} maps to this property automatically.
 * The mapping rule: dots become underscores, hyphens become underscores, and the result is
 * uppercased. So {@code cors.allowed-origins} -> {@code CORS_ALLOWED_ORIGINS}.</p>
 *
 * <p>In the local Docker Compose setup, this value is {@code http://localhost:4200}.
 * In production, it is the S3 website URL. The codebase never changes -- only the
 * environment variable differs per machine.</p>
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.relaxed-binding">
 *      Spring Boot Relaxed Binding</a>
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /**
     * Comma-separated list of allowed origins for CORS.
     *
     * <p>Resolved from property {@code cors.allowed-origins}, overridden in production
     * by the environment variable {@code CORS_ALLOWED_ORIGINS} set in the EC2 {@code .env} file.</p>
     */
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/auth/register", "/api/auth/login");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true);
    }
}
