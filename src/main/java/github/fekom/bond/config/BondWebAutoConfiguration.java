package github.fekom.bond.config;

import github.fekom.bond.api.RateLimiterService;
import github.fekom.bond.infrastructure.web.BondRateLimitFilter;
import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration(after = BondAutoConfiguration.class)
@ConditionalOnClass(Filter.class)
@ConditionalOnProperty(prefix = "bond", name = "filter-enabled", havingValue = "true", matchIfMissing = true)
public class BondWebAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(BondRateLimitFilter.class)
	public FilterRegistrationBean<BondRateLimitFilter> bondRateLimitFilter(RateLimiterService rateLimiterService) {
		FilterRegistrationBean<BondRateLimitFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(new BondRateLimitFilter(rateLimiterService));
		registration.addUrlPatterns("/*");
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
		registration.setName("bondRateLimitFilter");
		return registration;
	}
}
