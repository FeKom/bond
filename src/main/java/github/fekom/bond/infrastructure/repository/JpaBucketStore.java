package github.fekom.bond.infrastructure.repository;

import github.fekom.bond.algorithms.TokenBucket;
import github.fekom.bond.api.BucketStore;
import github.fekom.bond.domain.Capacity;
import github.fekom.bond.infrastructure.persistence.BlockedClient;
import github.fekom.bond.infrastructure.persistence.Request;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public class JpaBucketStore implements BucketStore {

	private final RequestRepository requestRepository;
	private final ClientJpaRepository clientJpaRepository;
	private final BlockedClientRepository blockedClientRepository;

	public JpaBucketStore(RequestRepository requestRepository, ClientJpaRepository clientJpaRepository,
			BlockedClientRepository blockedClientRepository) {
		this.requestRepository = requestRepository;
		this.clientJpaRepository = clientJpaRepository;
		this.blockedClientRepository = blockedClientRepository;
	}

	@Override
	public Optional<TokenBucket> findBucket(String ipAddress, String endpoint) {
		return requestRepository.findByIpAddressAndEndpoint(ipAddress, endpoint)
				.map(Request::getBucket);
	}

	@Override
	@Transactional
	public void saveBucket(String ipAddress, String endpoint, TokenBucket bucket) {
		Request entity = requestRepository.findByIpAddressAndEndpoint(ipAddress, endpoint)
				.orElseGet(() -> {
					var req = new Request();
					req.setId(com.fasterxml.uuid.Generators.timeBasedEpochGenerator().generate().toString());
					req.setIpAddress(ipAddress);
					req.setEndpoint(endpoint);
					req.setCreatedAt(LocalDateTime.now());
					return req;
				});
		entity.setBucket(bucket);
		entity.setUpdatedAt(LocalDateTime.now());
		requestRepository.save(entity);
	}

	@Override
	public Optional<Capacity> findCapacityByIp(String ipAddress) {
		return clientJpaRepository.findByIpAddress(ipAddress)
				.map(github.fekom.bond.infrastructure.persistence.Client::capacity);
	}

	@Override
	@Transactional
	public void saveCapacityOverride(String ipAddress, Capacity capacity) {
		github.fekom.bond.infrastructure.persistence.Client entity =
				clientJpaRepository.findByIpAddress(ipAddress)
						.orElseGet(() -> {
							var client = new github.fekom.bond.infrastructure.persistence.Client();
							client.setId(com.fasterxml.uuid.Generators.timeBasedEpochGenerator().generate().toString());
							client.setIpAddress(ipAddress);
							client.setCreatedAt(LocalDateTime.now());
							return client;
						});
		entity.setCapacityBytes(capacity.capacityBytes());
		entity.setRefillRateBytesPerSecond(capacity.refillRateBytesPerSecond());
		entity.setBurstMultiplier(capacity.burstMultiplier());
		clientJpaRepository.save(entity);
	}

	@Override
	public boolean isBlocked(String ipAddress) {
		return blockedClientRepository.existsByIpAddress(ipAddress);
	}

	@Override
	@Transactional
	public void blockIp(String ipAddress, String reason) {
		if (blockedClientRepository.existsByIpAddress(ipAddress)) {
			return;
		}
		var blocked = new BlockedClient();
		blocked.setId(com.fasterxml.uuid.Generators.timeBasedEpochGenerator().generate().toString());
		blocked.setIpAddress(ipAddress);
		blocked.setReason(reason);
		blocked.setBlockedAt(LocalDateTime.now());
		blockedClientRepository.save(blocked);
	}

	@Override
	@Transactional
	public boolean unblockIp(String ipAddress) {
		Optional<BlockedClient> found = blockedClientRepository.findByIpAddress(ipAddress);
		if (found.isPresent()) {
			blockedClientRepository.delete(found.get());
			return true;
		}
		return false;
	}
}
