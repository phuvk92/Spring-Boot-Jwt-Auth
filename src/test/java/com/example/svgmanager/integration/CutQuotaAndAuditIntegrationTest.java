package com.example.svgmanager.integration;

import com.example.svgmanager.dto.request.AdjustQuotaRequest;
import com.example.svgmanager.dto.request.RefundQuotaRequest;
import com.example.svgmanager.entity.*;
import com.example.svgmanager.repository.*;
import com.example.svgmanager.service.CutQuotaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CutQuotaAndAuditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private DealerCutQuotaRepository dealerCutQuotaRepository;

    @Autowired
    private CutQuotaLedgerRepository cutQuotaLedgerRepository;

    @Autowired
    private CutJobRepository cutJobRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private CutQuotaService cutQuotaService;

    private Dealer dealer;
    private User mappedUser;
    private User unmappedUser;
    private UserDevice device;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        cutQuotaLedgerRepository.deleteAll();
        cutJobRepository.deleteAll();
        dealerCutQuotaRepository.deleteAll();
        userDeviceRepository.deleteAll();

        dealer = dealerRepository.save(Dealer.builder()
                .code("DL-HA-NOI-" + System.currentTimeMillis())
                .name("Đại lý Hà Nội")
                .status("ACTIVE")
                .build());

        // Khởi tạo quota ban đầu 100 lượt
        DealerCutQuota initialQuota = new DealerCutQuota(dealer, 100);
        dealerCutQuotaRepository.save(initialQuota);

        mappedUser = userRepository.save(User.builder()
                .username("worker_hanoi")
                .email("worker@hanoi.vn")
                .keycloakUserId("kc-worker-hanoi")
                .role(Role.USER)
                .enabled(true)
                .dealer(dealer)
                .build());

        unmappedUser = userRepository.save(User.builder()
                .username("unmapped_worker")
                .email("unmapped@nowhere.vn")
                .keycloakUserId("kc-unmapped")
                .role(Role.USER)
                .enabled(true)
                .build());

        device = userDeviceRepository.save(UserDevice.register(
                mappedUser, "dev-hanoi-01", "Máy Cắt HN #1", "windows", "192.168.1.50", "sid-hanoi-1", LocalDateTime.now()));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtUser(User user, String sid) {
        return jwt().jwt(j -> j.subject(user.getKeycloakUserId())
                        .claim("preferred_username", user.getUsername())
                        .claim("sid", sid))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtAdmin() {
        return jwt().jwt(j -> j.subject("kc-admin")
                        .claim("preferred_username", "admin"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("1. User chưa thuộc đại lý hợp lệ bị từ chối lệnh Cut (403 Forbidden)")
    void unmappedUser_CutRejected() throws Exception {
        UserDevice unmappedDevice = userDeviceRepository.save(UserDevice.register(
                unmappedUser, "dev-unmapped", "Máy Vô Gia Cư", "windows", "10.0.0.99", "sid-unmapped", LocalDateTime.now()));

        String cutJson = """
                {
                    "cutAt": "2026-11-14T09:00:00",
                    "partLabel": "Nắp capo",
                    "vehicleLabel": "Hyundai Tucson",
                    "filmUsage": "1.2 m",
                    "filmUsageMeters": 1.2
                }
                """;

        mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtUser(unmappedUser, "sid-unmapped"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cutJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("2. User có đại lý thực hiện Cut thành công: trừ 1 quota, ghi nhận Sổ cái và Audit Log")
    void mappedUser_CutSuccess_QuotaDeductedAndAudited() throws Exception {
        String cutJson = """
                {
                    "cutAt": "2026-11-14T10:00:00",
                    "partLabel": "Cản trước",
                    "vehicleLabel": "Mazda CX-5",
                    "filmUsage": "0.8 m",
                    "filmUsageMeters": 0.8
                }
                """;

        mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtUser(mappedUser, "sid-hanoi-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cutJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.partLabel").value("Cản trước"));

        // Kiểm tra Quota sau khi trừ
        DealerCutQuota quota = dealerCutQuotaRepository.findByDealerId(dealer.getId()).orElseThrow();
        assertThat(quota.getRemainingCuts()).isEqualTo(99);
        assertThat(quota.getConsumedCuts()).isEqualTo(1);
        assertThat(quota.getSuccessCuts()).isEqualTo(1);

        // Kiểm tra Sổ cái biến động
        var ledgers = cutQuotaLedgerRepository.findByCutJobId(cutJobRepository.findAll().get(0).getId());
        assertThat(ledgers).hasSize(1);
        CutQuotaLedger ledger = ledgers.get(0);
        assertThat(ledger.getTransactionType()).isEqualTo("CONSUME");
        assertThat(ledger.getDelta()).isEqualTo(-1);
        assertThat(ledger.getBalanceBefore()).isEqualTo(100);
        assertThat(ledger.getBalanceAfter()).isEqualTo(99);
        assertThat(ledger.getDealerName()).isEqualTo("Đại lý Hà Nội");
        assertThat(ledger.getUsername()).isEqualTo("worker_hanoi");

        // Kiểm tra Audit Log có thông tin đại lý và quota delta
        var audits = auditLogRepository.findAll();
        AuditLog cutAudit = audits.stream()
                .filter(a -> "CUT_RECORDED".equals(a.getAction()))
                .reduce((first, second) -> second)
                .orElseThrow();
        assertThat(cutAudit.getDealerId()).isEqualTo(dealer.getId());
        assertThat(cutAudit.getDealerName()).isEqualTo("Đại lý Hà Nội");
        assertThat(cutAudit.getCutDelta()).isEqualTo(-1);
        assertThat(cutAudit.getBalanceAfter()).isEqualTo(99);
    }

    @Test
    @DisplayName("3. Idempotency-Key chống trừ lặp Quota khi gửi lại cùng request")
    void idempotentCut_DoesNotDeductTwice() throws Exception {
        String cutJson = """
                {
                    "cutAt": "2026-11-14T10:15:00",
                    "partLabel": "Gương chiếu hậu",
                    "vehicleLabel": "Toyota Camry",
                    "filmUsage": "0.3 m",
                    "filmUsageMeters": 0.3
                }
                """;

        String idempotencyKey = "unique-key-xyz-789";

        // Lần 1
        mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtUser(mappedUser, "sid-hanoi-1"))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cutJson))
                .andExpect(status().isCreated());

        DealerCutQuota quotaAfterFirst = dealerCutQuotaRepository.findByDealerId(dealer.getId()).orElseThrow();
        assertThat(quotaAfterFirst.getRemainingCuts()).isEqualTo(99);

        // Lần 2 (cùng key)
        mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtUser(mappedUser, "sid-hanoi-1"))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cutJson))
                .andExpect(status().isCreated());

        // Số dư Quota không được bị trừ thêm lần nữa!
        DealerCutQuota quotaAfterSecond = dealerCutQuotaRepository.findByDealerId(dealer.getId()).orElseThrow();
        assertThat(quotaAfterSecond.getRemainingCuts()).isEqualTo(99);
        assertThat(cutJobRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("4. Hết Quota (remaining = 0) thì bị từ chối lệnh Cut (400 QUOTA_EXCEEDED)")
    void exhaustedQuota_CutRejected() throws Exception {
        // Cập nhật số dư về 0
        DealerCutQuota quota = dealerCutQuotaRepository.findByDealerId(dealer.getId()).orElseThrow();
        quota.setRemainingCuts(0);
        quota.setConsumedCuts(100);
        dealerCutQuotaRepository.save(quota);

        String cutJson = """
                {
                    "cutAt": "2026-11-14T11:00:00",
                    "partLabel": "Cửa xe",
                    "vehicleLabel": "Honda CR-V",
                    "filmUsage": "0.5 m",
                    "filmUsageMeters": 0.5
                }
                """;

        mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtUser(mappedUser, "sid-hanoi-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cutJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUOTA_EXCEEDED"));
    }

    @Test
    @DisplayName("5. Admin điều chỉnh Quota (ADD, SUBTRACT, SET) và ghi nhận Sổ cái giao dịch")
    void adminAdjustQuota_WorksCorrectly() throws Exception {
        // Cộng thêm 50 lượt
        AdjustQuotaRequest addReq = new AdjustQuotaRequest(50, "ADD", "Thưởng chỉ tiêu tháng 11");
        mockMvc.perform(post("/api/admin/cut-quota/dealers/" + dealer.getId() + "/adjust")
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCuts").value(150));

        // Trừ bớt 20 lượt
        AdjustQuotaRequest subReq = new AdjustQuotaRequest(20, "SUBTRACT", "Điều chỉnh giảm do đổi gói");
        mockMvc.perform(post("/api/admin/cut-quota/dealers/" + dealer.getId() + "/adjust")
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCuts").value(130));

        // Thiết lập cố định 200 lượt
        AdjustQuotaRequest setReq = new AdjustQuotaRequest(200, "SET", "Reset đầu kỳ năm mới");
        mockMvc.perform(post("/api/admin/cut-quota/dealers/" + dealer.getId() + "/adjust")
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCuts").value(200));
    }

    @Test
    @DisplayName("6. Admin hoàn trả Quota cho lệnh cắt hỏng và cập nhật trạng thái CutJob sang REFUNDED")
    void adminRefundQuota_RestoresBalance() throws Exception {
        // Thực hiện 1 lệnh cắt hợp lệ
        String cutJson = """
                {
                    "cutAt": "2026-11-14T14:00:00",
                    "partLabel": "Đèn pha",
                    "vehicleLabel": "Kia Carnival",
                    "filmUsage": "0.4 m",
                    "filmUsageMeters": 0.4
                }
                """;

        MvcResult cutRes = mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtUser(mappedUser, "sid-hanoi-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cutJson))
                .andExpect(status().isCreated())
                .andReturn();

        Long jobId = objectMapper.readTree(cutRes.getResponse().getContentAsString()).get("id").asLong();

        DealerCutQuota quotaAfterCut = dealerCutQuotaRepository.findByDealerId(dealer.getId()).orElseThrow();
        assertThat(quotaAfterCut.getRemainingCuts()).isEqualTo(99);

        // Admin hoàn trả
        RefundQuotaRequest refundReq = new RefundQuotaRequest("Máy kẹt dao gãy lưỡi cắt");
        mockMvc.perform(post("/api/admin/cut-quota/dealers/" + dealer.getId() + "/refund/" + jobId)
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("REFUND"))
                .andExpect(jsonPath("$.delta").value(1))
                .andExpect(jsonPath("$.balanceAfter").value(100));

        // Kiểm tra CutJob đã đổi sang REFUNDED
        CutJob job = cutJobRepository.findById(jobId).orElseThrow();
        assertThat(job.getStatus()).isEqualTo("REFUNDED");

        // Hoàn trả lại lần nữa phải báo Conflict
        mockMvc.perform(post("/api/admin/cut-quota/dealers/" + dealer.getId() + "/refund/" + jobId)
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundReq)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("7. Đối soát số dư Quota và Sổ cái giao dịch (Reconciliation)")
    void quotaReconciliation_ChecksBalance() throws Exception {
        mockMvc.perform(get("/api/admin/cut-quota/reconciliation/" + dealer.getId())
                        .with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanced").value(true))
                .andExpect(jsonPath("$.difference").value(0));
    }
}
