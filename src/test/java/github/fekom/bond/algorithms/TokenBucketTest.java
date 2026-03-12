package github.fekom.bond.algorithms;

import github.fekom.bond.domain.Capacity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenBucket")
class TokenBucketTest {

    private static final Capacity FREE_CAPACITY = new Capacity(
        32_768, // 32KB capacity
        32_768 / 3600, // 32KB/hour refill
        1.5 // 150% burst
    );

    private static final Capacity STARTUP_CAPACITY = new Capacity(
        10 * 1024 * 1024, // 10MB capacity
        (10 * 1024 * 1024) / 3600, // 10MB/hour
        2.0 // 200% burst
    );

    private static final Capacity ENTERPRISE_CAPACITY = new Capacity(
        1L * 1024 * 1024 * 1024, // 1GB capacity
        (1L * 1024 * 1024 * 1024) / 3600, // 1GB/hour
        3.0 // 300% burst
    );

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create bucket with correct capacity for FREE capacity")
        void shouldCreateWithCorrectCapacityForFreeTier() {
            TokenBucket bucket = new TokenBucket(FREE_CAPACITY);

            assertEquals(32_768, bucket.getBucketCapacityBytes());
            assertEquals(32_768, bucket.getCurrentBytes()); // Starts full
            assertEquals(0, bucket.getUsedBytes());
            assertEquals(0.0, bucket.getUsagePercentage(), 0.01);
        }

        @Test
        @DisplayName("should create bucket with correct capacity for STARTUP capacity")
        void shouldCreateWithCorrectCapacityForStartupTier() {
            TokenBucket bucket = new TokenBucket(STARTUP_CAPACITY);

            assertEquals(10 * 1024 * 1024, bucket.getBucketCapacityBytes());
        }

        @Test
        @DisplayName("should create bucket with correct capacity for ENTERPRISE capacity")
        void shouldCreateWithCorrectCapacityForEnterpriseTier() {
            TokenBucket bucket = new TokenBucket(ENTERPRISE_CAPACITY);

            assertEquals(1L * 1024 * 1024 * 1024, bucket.getBucketCapacityBytes());
        }

        @Test
        @DisplayName("should set burst multiplier correctly")
        void shouldSetBurstMultiplierCorrectly() {
            TokenBucket freeBucket = new TokenBucket(FREE_CAPACITY);
            TokenBucket startupBucket = new TokenBucket(STARTUP_CAPACITY);
            TokenBucket enterpriseBucket = new TokenBucket(ENTERPRISE_CAPACITY);

            assertEquals(1.5, freeBucket.getBurstMultiplier());
            assertEquals(2.0, startupBucket.getBurstMultiplier());
            assertEquals(3.0, enterpriseBucket.getBurstMultiplier());
        }
    }

    @Nested
    @DisplayName("allowRequest")
    class AllowRequest {

        private TokenBucket bucket;

        @BeforeEach
        void setUp() {
            bucket = new TokenBucket(FREE_CAPACITY); // 32KB capacity
        }

        @Test
        @DisplayName("should allow request when sufficient capacity")
        void shouldAllowRequestWhenSufficientCapacity() {
            assertTrue(bucket.allowRequest(100));
            assertEquals(100, bucket.getUsedBytes());
        }

        @Test
        @DisplayName("should allow multiple requests until capacity exhausted")
        void shouldAllowMultipleRequestsUntilCapacityExhausted() {
            // 32KB = 32768 bytes
            assertTrue(bucket.allowRequest(10_000));
            assertTrue(bucket.allowRequest(10_000));
            assertTrue(bucket.allowRequest(10_000));
            assertFalse(bucket.allowRequest(5_000)); // No room left
        }

        @Test
        @DisplayName("should reject request when insufficient capacity")
        void shouldRejectRequestWhenInsufficientCapacity() {
            bucket.allowRequest(32_000); // Consume almost everything
            assertFalse(bucket.allowRequest(1_000)); // No room
        }

        @Test
        @DisplayName("should reject request larger than total capacity")
        void shouldRejectRequestLargerThanTotalCapacity() {
            assertFalse(bucket.allowRequest(50_000)); // Larger than 32KB
        }

        @Test
        @DisplayName("should allow zero size request")
        void shouldAllowZeroSizeRequest() {
            assertTrue(bucket.allowRequest(0));
            assertEquals(0, bucket.getUsedBytes());
        }

        @Test
        @DisplayName("should update usagePercentage correctly")
        void shouldUpdateUsagePercentageCorrectly() {
            bucket.allowRequest(16_384); // Half of capacity
            assertEquals(50.0, bucket.getUsagePercentage(), 0.01);
        }
    }

    @Nested
    @DisplayName("refill")
    class Refill {

        @Test
        @DisplayName("should refill tokens over time")
        void shouldRefillTokensOverTime() throws InterruptedException {
            TokenBucket bucket = new TokenBucket(FREE_CAPACITY);

            // Consume all tokens
            bucket.allowRequest(32_768);
            assertEquals(0, bucket.getCurrentBytes());

            // Wait for refill (FREE = ~9 bytes/second)
            Thread.sleep(1100); // 1.1 seconds

            // Force refill
            bucket.refill();

            // Should have refilled some bytes
            assertTrue(bucket.getCurrentBytes() > 0);
        }

        @Test
        @DisplayName("should not exceed max capacity on refill")
        void shouldNotExceedMaxCapacityOnRefill() throws InterruptedException {
            TokenBucket bucket = new TokenBucket(FREE_CAPACITY);

            // Consume few tokens
            bucket.allowRequest(100);

            // Wait and force refill
            Thread.sleep(100);
            bucket.refill();

            // Should not exceed capacity
            assertTrue(bucket.getCurrentBytes() <= bucket.getBucketCapacityBytes());
        }
    }

    @Nested
    @DisplayName("getWaitTime")
    class GetWaitTime {

        @Test
        @DisplayName("should return 0 when sufficient capacity")
        void shouldReturnZeroWhenSufficientCapacity() {
            TokenBucket bucket = new TokenBucket(FREE_CAPACITY);
            assertEquals(0, bucket.getWaitTime(100));
        }

        @Test
        @DisplayName("should calculate wait time when insufficient capacity")
        void shouldCalculateWaitTimeWhenInsufficientCapacity() {
            TokenBucket bucket = new TokenBucket(FREE_CAPACITY);

            // Exhaust the bucket
            bucket.allowRequest(32_768);

            // Calculate time to get 100 bytes
            // FREE refill rate = ~9 bytes/second
            // 100 bytes / 9 bytes/s = ~11 seconds = ~11000ms
            long waitTime = bucket.getWaitTime(100);
            assertTrue(waitTime > 0);
            assertTrue(waitTime > 10_000); // More than 10 seconds
        }
    }

    @Nested
    @DisplayName("JSON Serialization")
    class JsonSerialization {

        @Test
        @DisplayName("should preserve state after serialization/deserialization")
        void shouldPreserveStateAfterSerialization() {
            TokenBucket original = new TokenBucket(STARTUP_CAPACITY);
            original.allowRequest(5000);

            // Simulate deserialization using the JSON constructor
            TokenBucket deserialized = new TokenBucket(
                original.getBucketCapacityBytes(),
                original.getRefillRateBytesPerSecond(),
                original.getBurstMultiplier(),
                original.getMaxBurstBytes(),
                original.getCurrentBytes(),
                original.getLastRefillTime(),
                original.getCreatedAt()
            );

            assertEquals(original.getCurrentBytes(), deserialized.getCurrentBytes());
            assertEquals(original.getUsedBytes(), deserialized.getUsedBytes());
        }
    }
}
