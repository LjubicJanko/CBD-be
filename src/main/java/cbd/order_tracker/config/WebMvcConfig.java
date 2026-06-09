package cbd.order_tracker.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

	private final FeatureGuard featureGuard;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new FeatureEnforcementInterceptor(featureGuard))
				.addPathPatterns("/api/**");
	}
}
