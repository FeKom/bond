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
    @DisplayName("Criação")
    class Creation {

        @Test
        @DisplayName("deve criar bucket com capacidade correta para FREE capacity")
        void shouldCreateWithCorrectCapacityForFreeTier() {
            TokenBucket bucket = new TokenBucket(FREE_CAPACITY);

            assertEquals(32_768, bucket.getBucketCapacityBytes());
            assertEquals(32_768, bucket.getCurrentBytes()); // Começa cheio
            assertEquals(0, bucket.getUsedBytes());
            assertEquals(0.0, bucket.getUsagePercentage(), 0.01);
        }

        @Test
        @DisplayName("deve criar bucket com capacidade correta para STARTUP capacity")
        void shouldCreateWithCorrectCapacityForStartupTier() {
            TokenBucket bucket = new TokenBucket(STARTUP_CAPACITY);

            assertEquals(10 * 1024 * 1024, bucket.getBucketCapacityBytes());
        }

        @Test
        @DisplayName("deve criar bucket com capacidade correta para ENTERPRISE capacity")
        void shouldCreateWithCorrectCapacityForEnterpriseTier() {
            TokenBucket bucket = new TokenBucket(ENTERPRISE_CAPACITY);

            assertEquals(1L * 1024 * 1024 * 1024, bucket.getBucketCapacityBytes());
        }

        @Test
        @DisplayName("deve configurar burst multiplier corretamente")
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
        @DisplayName("deve permitir request quando há capacidade suficiente")
        void shouldAllowRequestWhenSufficientCapacity() {
            assertTrue(bucket.allowRequest(100));
            assertEquals(100, bucket.getUsedBytes());
        }

        @Test
        @DisplayName("deve permitir múltiplas requests até esgotar capacidade")
        void shouldAllowMultipleRequestsUntilCapacityExhausted() {
            // 32KB = 32768 bytes
            assertTrue(bucket.allowRequest(10_000));
            assertTrue(bucket.allowRequest(10_000));
            assertTrue(bucket.allowRequest(10_000));
            assertFalse(bucket.allowRequest(5_000)); // Não cabe mais
        }

        @Test
        @DisplayName("deve rejeitar request quando não há capacidade")
        void shouldRejectRequestWhenInsufficientCapacity() {
            bucket.allowRequest(32_000); // Consome quase tudo
            assertFalse(bucket.allowRequest(1_000)); // Não cabe
        }

        @Test
        @DisplayName("deve rejeitar request maior que capacidade total")
        void shouldRejectRequestLargerThanTotalCapacity() {
            assertFalse(bucket.allowRequest(50_000)); // Maior que 32KB
        }

        @Test
        @DisplayName("deve permitir request de tamanho zero")
        void shouldAllowZeroSizeRequest() {
            assertTrue(bucket.allowRequest(0));
            assertEquals(0, bucket.getUsedBytes());
        }

        @Test
        @DisplayName("deve atualizar usagePercentage corretamente")
        void shouldUpdateUsagePercentageCorrectly() {
            bucket.allowRequest(16_384); // Metade da capacidade
            assertEquals(50.0, bucket.getUsagePercentage(), 0.01);
        }
    }

    @Nested
    @DisplayName("refill")
    class Refill {

        @Test
        @DisplayName("deve reabastecer tokens ao longo do tempo")
        void shouldRefillTokensOverTime() throws InterruptedException {
            TokenBucket bucket = new TokenBucket(FREE_CAPACITY);

            // Consome todos os tokens
            bucket.allowRequest(32_768);
            assertEquals(0, bucket.getCurrentBytes());

            // Espera um pouco para refill (FREE = ~9 bytes/segundo)
            Thread.sleep(1100); // 1.1 segundos

            // Força refill
            bucket.refill();

            // Deve ter reabastecido alguns bytes
            assertTrue(bucket.getCurrentBytes() > 0);
        }

        @Test
        @DisplayName("não deve exceder capacidade máxima no refill")
        void shouldNotExceedMaxCapacityOnRefill() throws InterruptedException {
            TokenBucket bucket = new TokenBucket(FREE_CAPACITY);

            // Consome poucos tokens
            bucket.allowRequest(100);

            // Espera e força refill
            Thread.sleep(100);
            bucket.refill();

            // Não deve exceder capacidade
            assertTrue(bucket.getCurrentBytes() <= bucket.getBucketCapacityBytes());
        }
    }

    @Nested
    @DisplayName("getWaitTime")
    class GetWaitTime {

        @Test
        @DisplayName("deve retornar 0 quando há capacidade suficiente")
        void shouldReturnZeroWhenSufficientCapacity() {
            TokenBucket bucket = new TokenBucket(FREE_CAPACITY);
            assertEquals(0, bucket.getWaitTime(100));
        }

        @Test
        @DisplayName("deve calcular tempo de espera quando capacidade insuficiente")
        void shouldCalculateWaitTimeWhenInsufficientCapacity() {
            TokenBucket bucket = new TokenBucket(FREE_CAPACITY);

            // Esgota o bucket
            bucket.allowRequest(32_768);

            // Calcula tempo para conseguir 100 bytes
            // FREE refill rate = ~9 bytes/segundo
            // 100 bytes / 9 bytes/s = ~11 segundos = ~11000ms
            long waitTime = bucket.getWaitTime(100);
            assertTrue(waitTime > 0);
            assertTrue(waitTime > 10_000); // Mais de 10 segundos
        }
    }

    @Nested
    @DisplayName("Serialização JSON")
    class JsonSerialization {

        @Test
        @DisplayName("deve preservar estado após serialização/deserialização")
        void shouldPreserveStateAfterSerialization() {
            TokenBucket original = new TokenBucket(STARTUP_CAPACITY);
            original.allowRequest(5000);

            // Simula deserialização usando o construtor JSON
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
