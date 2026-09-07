package academic.academic.global.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * 로컬 프론트엔드 개발 서버(Vite, {@code Academic_FE}, {@code http://localhost:5173})가 브라우저에서
 * 직접 API를 호출할 수 있게 허용한다. {@link WebMvcConfigurer#addCorsMappings}가 아니라 서블릿
 * {@link CorsFilter}로 직접 구현하는 이유: {@link JwtAuthenticationFilter}가 인증 실패를 직접
 * 401로 응답하고 끝내버리는데(서블릿 필터라 DispatcherServlet보다 먼저 실행됨), WebMvcConfigurer 기반
 * CORS는 DispatcherServlet의 핸들러 매핑 단계에서만 헤더를 붙이므로 그 401 응답에는 CORS 헤더가 아예
 * 안 붙는다 — 브라우저가 401 본문을 읽지 못하고 네트워크 에러로 처리해버려서 프런트의 401→refresh
 * 재시도 로직이 동작하지 않는다. {@link Ordered#HIGHEST_PRECEDENCE}로 등록해 항상
 * {@link JwtAuthenticationFilter}보다 먼저 실행되게 하면, 그 필터가 무엇을 하든 응답에 CORS 헤더가
 * 이미 붙어 있다. 인증은 쿠키가 아니라 {@code Authorization: Bearer} 헤더로만 하므로
 * {@code allowCredentials}는 필요 없다 — {@code X-Active-Student-Id}(공지 API, §12)도 커스텀
 * 헤더라 명시적으로 허용해야 한다.
 */
@Configuration
public class CorsConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Active-Student-Id"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/v1/**", configuration);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
