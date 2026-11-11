package com.example.svgmanager.integration;

import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserSvgFile;
import com.example.svgmanager.entity.v2.UserSvgFileV2;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.repository.UserSvgFileRepository;
import com.example.svgmanager.repository.internal.v2.UserSvgFileV2Repository;
import com.example.svgmanager.service.FileStorageService;
import com.example.svgmanager.service.SvgPreviewCacheService;
import com.example.svgmanager.service.internal.v2.EncryptedFileResult;
import com.example.svgmanager.service.internal.v2.FileEncryptionV2Service;
import com.example.svgmanager.service.internal.v2.FileStorageV2Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PreviewPerformanceBenchmarkTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSvgFileRepository v1Repository;

    @Autowired
    private UserSvgFileV2Repository v2Repository;

    @Autowired
    private FileStorageService v1StorageService;

    @Autowired
    private FileStorageV2Service v2StorageService;

    @Autowired
    private FileEncryptionV2Service encryptionV2Service;

    @Autowired
    private SvgPreviewCacheService previewCacheService;

    private User testUser;
    private User otherUser;
    private Long v1FileId;
    private Long v2FileId;

    private static final String SAMPLE_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1000 800" width="1000mm" height="800mm">
              <g transform="translate(50, 50)">
                <path d="M10,20 L100,20 L100,200 L10,200 Z" fill="#2563EB" data-cut="door-panel" data-part="D1"/>
                <circle cx="300" cy="300" r="150" fill="none" stroke="#DC2626" stroke-width="2"/>
              </g>
            </svg>
            """;

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUser(User u) {
        return jwt().jwt(j -> j.subject(u.getKeycloakUserId()).claim("preferred_username", u.getUsername()))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @BeforeEach
    void setUp() {
        previewCacheService.clear();

        testUser = userRepository.save(User.builder()
                .username("bench_user_" + UUID.randomUUID())
                .email("bench_" + UUID.randomUUID() + "@example.com")
                .keycloakUserId("kc-bench-" + UUID.randomUUID())
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        otherUser = userRepository.save(User.builder()
                .username("other_user_" + UUID.randomUUID())
                .email("other_" + UUID.randomUUID() + "@example.com")
                .keycloakUserId("kc-other-" + UUID.randomUUID())
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        // 1. Setup V1 test file
        byte[] v1Bytes = SAMPLE_SVG.getBytes(StandardCharsets.UTF_8);
        String v1Path = v1StorageService.storeFile(v1Bytes, "bench_v1_" + UUID.randomUUID() + ".svg");
        UserSvgFile v1Entity = UserSvgFile.builder()
                .fileName("bench_v1.svg")
                .originalFileName("bench_v1.svg")
                .storedFileName("stored_v1.svg")
                .filePath(v1Path)
                .fileSize((long) v1Bytes.length)
                .mimeType("image/svg+xml")
                .checksum("sha256:v1benchhash")
                .status("ACTIVE")
                .user(testUser)
                .build();
        v1Entity = v1Repository.save(v1Entity);
        v1FileId = v1Entity.getId();

        // 2. Setup V2 test file with envelope encryption
        EncryptedFileResult encResult = encryptionV2Service.encrypt(v1Bytes);
        String v2StorageKey = v2StorageService.storeEncryptedFile(encResult.ciphertext(), testUser.getId(), UUID.randomUUID().toString());

        UserSvgFileV2 v2Entity = UserSvgFileV2.builder()
                .fileName("bench_v2.svg")
                .originalFileName("bench_v2.svg")
                .storageKey(v2StorageKey)
                .filePath(v2StorageKey)
                .fileSize((long) v1Bytes.length)
                .mimeType("image/svg+xml")
                .plaintextChecksum(encResult.plaintextChecksum())
                .encryptedChecksum(encResult.encryptedChecksum())
                .encryptionAlgorithm(encResult.algorithm())
                .encryptionKeyVersion(encResult.keyVersion())
                .encryptedDek(encResult.encryptedDek())
                .encryptionIv(encResult.iv())
                .status("ACTIVE")
                .user(testUser)
                .build();
        v2Entity = v2Repository.save(v2Entity);
        v2FileId = v2Entity.getId();
    }

    @Test
    @DisplayName("Verify Contract: V1 & V2 Preview response headers and payload match specification exactly")
    void testPreviewContracts() throws Exception {
        // V1 Preview contract verification
        MvcResult v1Res = mockMvc.perform(get("/api/internal/user-files/" + v1FileId + "/preview")
                        .with(asUser(testUser)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("image/svg+xml")))
                .andExpect(header().string("X-Content-Type-Options", is("nosniff")))
                .andExpect(header().string("Content-Security-Policy", is("default-src 'none'; img-src 'self' data:; style-src 'unsafe-inline'; sandbox")))
                .andExpect(header().string("Cache-Control", is("no-cache, no-store, must-revalidate")))
                .andReturn();

        byte[] v1Bytes = v1Res.getResponse().getContentAsByteArray();
        assertEquals(SAMPLE_SVG.trim(), new String(v1Bytes, StandardCharsets.UTF_8).trim());

        // V2 Preview contract verification
        MvcResult v2Res = mockMvc.perform(get("/api/internal/v2/user-files/" + v2FileId + "/preview")
                        .with(asUser(testUser)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("image/svg+xml")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, is("inline")))
                .andExpect(header().string("X-Content-Type-Options", is("nosniff")))
                .andExpect(header().string("Content-Security-Policy", is("default-src 'none'; img-src 'self' data:; style-src 'unsafe-inline'; sandbox")))
                .andExpect(header().string("Cache-Control", is("private, no-store")))
                .andReturn();

        byte[] v2Bytes = v2Res.getResponse().getContentAsByteArray();
        assertEquals(SAMPLE_SVG.trim(), new String(v2Bytes, StandardCharsets.UTF_8).trim());

        // Verify unauthorized access rejected
        mockMvc.perform(get("/api/internal/user-files/" + v1FileId + "/preview")
                        .with(asUser(otherUser)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/internal/v2/user-files/" + v2FileId + "/preview")
                        .with(asUser(otherUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Benchmark: Multi-threaded load test on V1 and V2 preview APIs measuring latency, TPS, and error rate")
    void testConcurrentPreviewPerformance() throws Exception {
        int threads = 20;
        int requestsPerThread = 25;
        int totalRequests = threads * requestsPerThread; // 500 requests per endpoint

        // Measure V1 Performance
        BenchmarkResult v1Result = runBenchmark("/api/internal/user-files/" + v1FileId + "/preview", testUser, threads, requestsPerThread);
        System.out.printf("[BENCHMARK V1] Requests: %d | Throughput: %.1f req/s | P50: %.2f ms | P95: %.2f ms | P99: %.2f ms | Error Rate: %.2f%%%n",
                totalRequests, v1Result.throughput, v1Result.p50, v1Result.p95, v1Result.p99, v1Result.errorRate);

        // Measure V2 Performance
        BenchmarkResult v2Result = runBenchmark("/api/internal/v2/user-files/" + v2FileId + "/preview", testUser, threads, requestsPerThread);
        System.out.printf("[BENCHMARK V2] Requests: %d | Throughput: %.1f req/s | P50: %.2f ms | P95: %.2f ms | P99: %.2f ms | Error Rate: %.2f%%%n",
                totalRequests, v2Result.throughput, v2Result.p50, v2Result.p95, v2Result.p99, v2Result.errorRate);

        // Assert SLO targets: P95 < 500ms, Error Rate < 1%
        assertTrue(v1Result.p95 < 500.0, "V1 P95 must be < 500ms");
        assertEquals(0.0, v1Result.errorRate, 0.01, "V1 error rate must be < 1%");

        assertTrue(v2Result.p95 < 500.0, "V2 P95 must be < 500ms");
        assertEquals(0.0, v2Result.errorRate, 0.01, "V2 error rate must be < 1%");
    }

    private BenchmarkResult runBenchmark(String url, User user, int threads, int requestsPerThread) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Long> latenciesMs = new CopyOnWriteArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        long startBenchmarkTime = System.nanoTime();

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < requestsPerThread; i++) {
                        long reqStart = System.nanoTime();
                        try {
                            MvcResult res = mockMvc.perform(get(url).with(asUser(user))).andReturn();
                            long reqDurationMs = (System.nanoTime() - reqStart) / 1_000_000L;
                            latenciesMs.add(reqDurationMs);
                            if (res.getResponse().getStatus() == 200) {
                                successCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                            }
                        } catch (Exception ex) {
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);
        long totalElapsedNs = System.nanoTime() - startBenchmarkTime;
        executor.shutdown();

        assertTrue(completed, "Benchmark should complete within 60s");

        List<Long> sorted = new ArrayList<>(latenciesMs);
        Collections.sort(sorted);

        double p50 = sorted.isEmpty() ? 0 : sorted.get((int) (sorted.size() * 0.50));
        double p95 = sorted.isEmpty() ? 0 : sorted.get((int) (sorted.size() * 0.95));
        double p99 = sorted.isEmpty() ? 0 : sorted.get((int) (sorted.size() * 0.99));

        double totalSeconds = totalElapsedNs / 1_000_000_000.0;
        double throughput = (successCount.get() + errorCount.get()) / totalSeconds;
        double errorRate = ((double) errorCount.get() / (successCount.get() + errorCount.get())) * 100.0;

        return new BenchmarkResult(p50, p95, p99, throughput, errorRate, successCount.get(), errorCount.get());
    }

    private record BenchmarkResult(double p50, double p95, double p99, double throughput, double errorRate, int success, int errors) {}
}
