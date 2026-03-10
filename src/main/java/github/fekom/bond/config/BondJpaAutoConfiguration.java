package github.fekom.bond.config;

import github.fekom.bond.api.BucketStore;
import github.fekom.bond.infrastructure.repository.BlockedClientRepository;
import github.fekom.bond.infrastructure.repository.ClientJpaRepository;
import github.fekom.bond.infrastructure.repository.JpaBucketStore;
import github.fekom.bond.infrastructure.repository.RequestRepository;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration(before = BondAutoConfiguration.class)
@ConditionalOnClass(EntityManager.class)
@ConditionalOnBean(EntityManager.class)
@EnableJpaRepositories(basePackages = "github.fekom.bond.infrastructure.repository")
@EntityScan(basePackages = "github.fekom.bond.infrastructure.persistence")
public class BondJpaAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(BucketStore.class)
	public JpaBucketStore jpaBucketStore(RequestRepository requestRepository,
			ClientJpaRepository clientJpaRepository,
			BlockedClientRepository blockedClientRepository) {
		return new JpaBucketStore(requestRepository, clientJpaRepository, blockedClientRepository);
	}
}
