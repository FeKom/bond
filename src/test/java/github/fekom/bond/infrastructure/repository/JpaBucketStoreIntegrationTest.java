package github.fekom.bond.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.*;

import github.fekom.bond.algorithms.TokenBucket;
import github.fekom.bond.api.BucketStore;
import github.fekom.bond.domain.Capacity;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
	"bond.filter-enabled=false",
	"spring.main.allow-bean-definition-overriding=true"
})
@Testcontainers
@DisplayName("JpaBucketStore Integration")
class JpaBucketStoreIntegrationTest {

	private static final Capacity DEFAULT_CAPACITY = new Capacity(32_768, 9, 1.5);
	private static final Capacity OVERRIDE_CAPACITY = new Capacity(1_048_576, 291, 2.0);
	private static final String IP = "192.168.1.50";
	private static final String ENDPOINT = "/api/data";

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
			.withDatabaseName("bond_test")
			.withUsername("test")
			.withPassword("test");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
	}

	@SpringBootApplication
	@EnableJpaRepositories(basePackages = "github.fekom.bond.infrastructure.repository")
	@EntityScan(basePackages = "github.fekom.bond.infrastructure.persistence")
	static class TestApp {
	}

	@Autowired
	private BucketStore bucketStore;

	@Autowired
	private RequestRepository requestRepository;

	@Autowired
	private ClientJpaRepository clientJpaRepository;

	@Autowired
	private BlockedClientRepository blockedClientRepository;

	@BeforeEach
	void setUp() {
		requestRepository.deleteAll();
		clientJpaRepository.deleteAll();
		blockedClientRepository.deleteAll();
	}

	// --- Bucket operations ---

	@Test
	@DisplayName("should return empty when bucket does not exist")
	void shouldReturnEmptyWhenBucketDoesNotExist() {
		Optional<TokenBucket> result = bucketStore.findBucket(IP, ENDPOINT);
		assertTrue(result.isEmpty());
	}

	@Test
	@DisplayName("should save and retrieve bucket with preserved state")
	void shouldSaveAndRetrieveBucket() {
		TokenBucket bucket = new TokenBucket(DEFAULT_CAPACITY);
		bucket.allowRequest(100);

		bucketStore.saveBucket(IP, ENDPOINT, bucket);

		Optional<TokenBucket> found = bucketStore.findBucket(IP, ENDPOINT);
		assertTrue(found.isPresent());
		assertEquals(bucket.getCurrentBytes(), found.get().getCurrentBytes());
		assertEquals(bucket.getBucketCapacityBytes(), found.get().getBucketCapacityBytes());
	}

	@Test
	@DisplayName("should update existing bucket without duplicating")
	void shouldUpdateExistingBucketWithoutDuplicating() {
		TokenBucket bucket = new TokenBucket(DEFAULT_CAPACITY);
		bucketStore.saveBucket(IP, ENDPOINT, bucket);

		bucket.allowRequest(500);
		bucketStore.saveBucket(IP, ENDPOINT, bucket);

		assertEquals(1, requestRepository.count());
		Optional<TokenBucket> found = bucketStore.findBucket(IP, ENDPOINT);
		assertTrue(found.isPresent());
		assertEquals(bucket.getCurrentBytes(), found.get().getCurrentBytes());
	}

	@Test
	@DisplayName("should keep buckets separate by endpoint")
	void shouldKeepBucketsSeparateByEndpoint() {
		TokenBucket bucket1 = new TokenBucket(DEFAULT_CAPACITY);
		bucket1.allowRequest(100);
		bucketStore.saveBucket(IP, "/api/a", bucket1);

		TokenBucket bucket2 = new TokenBucket(DEFAULT_CAPACITY);
		bucket2.allowRequest(200);
		bucketStore.saveBucket(IP, "/api/b", bucket2);

		assertEquals(2, requestRepository.count());
		assertNotEquals(
			bucketStore.findBucket(IP, "/api/a").get().getCurrentBytes(),
			bucketStore.findBucket(IP, "/api/b").get().getCurrentBytes()
		);
	}

	@Test
	@DisplayName("should keep buckets separate by IP")
	void shouldKeepBucketsSeparateByIp() {
		bucketStore.saveBucket("10.0.0.1", ENDPOINT, new TokenBucket(DEFAULT_CAPACITY));
		bucketStore.saveBucket("10.0.0.2", ENDPOINT, new TokenBucket(DEFAULT_CAPACITY));
		assertEquals(2, requestRepository.count());
	}

	// --- Capacity override operations ---

	@Test
	@DisplayName("should return empty when no capacity override exists")
	void shouldReturnEmptyWhenNoOverride() {
		Optional<Capacity> result = bucketStore.findCapacityByIp(IP);
		assertTrue(result.isEmpty());
	}

	@Test
	@DisplayName("should save and retrieve capacity override")
	void shouldSaveAndRetrieveCapacityOverride() {
		bucketStore.saveCapacityOverride(IP, OVERRIDE_CAPACITY);

		Optional<Capacity> found = bucketStore.findCapacityByIp(IP);
		assertTrue(found.isPresent());
		assertEquals(OVERRIDE_CAPACITY.capacityBytes(), found.get().capacityBytes());
		assertEquals(OVERRIDE_CAPACITY.refillRateBytesPerSecond(), found.get().refillRateBytesPerSecond());
		assertEquals(OVERRIDE_CAPACITY.burstMultiplier(), found.get().burstMultiplier());
	}

	@Test
	@DisplayName("should update existing capacity override without duplicating")
	void shouldUpdateExistingOverrideWithoutDuplicating() {
		bucketStore.saveCapacityOverride(IP, DEFAULT_CAPACITY);
		bucketStore.saveCapacityOverride(IP, OVERRIDE_CAPACITY);

		assertEquals(1, clientJpaRepository.count());
		Optional<Capacity> found = bucketStore.findCapacityByIp(IP);
		assertTrue(found.isPresent());
		assertEquals(OVERRIDE_CAPACITY.capacityBytes(), found.get().capacityBytes());
	}

	@Test
	@DisplayName("should keep overrides separate by IP")
	void shouldKeepOverridesSeparateByIp() {
		bucketStore.saveCapacityOverride("10.0.0.1", DEFAULT_CAPACITY);
		bucketStore.saveCapacityOverride("10.0.0.2", OVERRIDE_CAPACITY);

		assertEquals(2, clientJpaRepository.count());
		assertEquals(DEFAULT_CAPACITY.capacityBytes(), bucketStore.findCapacityByIp("10.0.0.1").get().capacityBytes());
		assertEquals(OVERRIDE_CAPACITY.capacityBytes(), bucketStore.findCapacityByIp("10.0.0.2").get().capacityBytes());
	}

	// --- Blocked client operations ---

	@Test
	@DisplayName("should return false when IP is not blocked")
	void shouldReturnFalseWhenIpNotBlocked() {
		assertFalse(bucketStore.isBlocked(IP));
	}

	@Test
	@DisplayName("should block IP and verify it is blocked")
	void shouldBlockIpAndVerifyBlocked() {
		bucketStore.blockIp(IP, "suspicious activity");
		assertTrue(bucketStore.isBlocked(IP));
		assertEquals(1, blockedClientRepository.count());
	}

	@Test
	@DisplayName("should not duplicate when blocking already blocked IP")
	void shouldNotDuplicateWhenBlockingAlreadyBlockedIp() {
		bucketStore.blockIp(IP, "first reason");
		bucketStore.blockIp(IP, "second reason");
		assertEquals(1, blockedClientRepository.count());
	}

	@Test
	@DisplayName("should unblock a blocked IP")
	void shouldUnblockBlockedIp() {
		bucketStore.blockIp(IP, "test");
		assertTrue(bucketStore.isBlocked(IP));

		boolean result = bucketStore.unblockIp(IP);
		assertTrue(result);
		assertFalse(bucketStore.isBlocked(IP));
		assertEquals(0, blockedClientRepository.count());
	}

	@Test
	@DisplayName("should return false when unblocking a non-blocked IP")
	void shouldReturnFalseWhenUnblockingNonBlockedIp() {
		assertFalse(bucketStore.unblockIp(IP));
	}

	@Test
	@DisplayName("should keep blocked IPs independent")
	void shouldKeepBlockedIpsIndependent() {
		bucketStore.blockIp("10.0.0.1", "reason1");
		bucketStore.blockIp("10.0.0.2", "reason2");

		assertTrue(bucketStore.isBlocked("10.0.0.1"));
		assertTrue(bucketStore.isBlocked("10.0.0.2"));
		assertFalse(bucketStore.isBlocked("10.0.0.3"));

		bucketStore.unblockIp("10.0.0.1");
		assertFalse(bucketStore.isBlocked("10.0.0.1"));
		assertTrue(bucketStore.isBlocked("10.0.0.2"));
	}
}
