package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.CreateDealerRequest;
import com.example.svgmanager.dto.request.CreateUserRequest;
import com.example.svgmanager.dto.request.UpdateDealerRequest;
import com.example.svgmanager.dto.request.UpdateUserRequest;
import com.example.svgmanager.dto.response.DealerResponse;
import com.example.svgmanager.dto.response.UserResponse;
import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.entity.DealerPackageHistory;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ConflictException;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.mapper.UserMapper;
import com.example.svgmanager.repository.DealerPackageHistoryRepository;
import com.example.svgmanager.repository.DealerRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.impl.DealerServiceImpl;
import com.example.svgmanager.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DealerAccountPackageTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DealerRepository dealerRepository;
    @Mock
    private SvgFileRepository svgFileRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private KeycloakUserService keycloakUserService;
    @Mock
    private UserDeviceService userDeviceService;
    @Mock
    private DealerPackageHistoryRepository dealerPackageHistoryRepository;

    private UserMapper userMapper;
    private UserServiceImpl userService;
    private DealerServiceImpl dealerService;

    private Dealer dealer;
    private User agentA;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        userService = new UserServiceImpl(
                userRepository,
                dealerRepository,
                svgFileRepository,
                userMapper,
                currentUserService,
                keycloakUserService,
                userDeviceService,
                null,
                dealerPackageHistoryRepository
        );
        dealerService = new DealerServiceImpl(
                dealerRepository,
                dealerPackageHistoryRepository,
                currentUserService,
                userRepository,
                keycloakUserService,
                userMapper
        );

        dealer = Dealer.builder()
                .id(1L)
                .code("DL01")
                .name("Đại lý Miền Bắc")
                .status("ACTIVE")
                .maxAccounts(10)
                .totalAllocatedDays(3600)
                .usedAllocatedDays(0)
                .packageStartDate(LocalDate.now().minusDays(10))
                .packageExpiryDate(LocalDate.now().plusYears(2))
                .packageStatus("ACTIVE")
                .deleted(false)
                .build();

        agentA = User.builder()
                .id(100L)
                .username("agent_north")
                .email("agent_north@test.com")
                .role(Role.AGENT)
                .dealer(dealer)
                .enabled(true)
                .deleted(false)
                .build();
    }

    @Test
    @DisplayName("Case 1 & 3: Gói 10 account, 3.600 ngày; tạo account USER hợp lệ thành công")
    void testCreateUser_ValidQuotaAllocation_Success() {
        when(currentUserService.getCurrentUser()).thenReturn(agentA);
        when(currentUserService.isAgent()).thenReturn(true);
        when(currentUserService.isAdmin()).thenReturn(false);
        when(dealerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(dealer));
        when(dealerRepository.countActiveUserAccountsByDealerId(1L)).thenReturn(0L);
        when(keycloakUserService.createUser(any(), any(), any(), any(), anyBoolean(), any())).thenReturn("kc-uuid-1");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(501L);
            return u;
        });

        CreateUserRequest request = CreateUserRequest.builder()
                .username("client01")
                .email("client01@test.com")
                .password("Password123!")
                .role(Role.USER)
                .allocatedDays(360)
                .build();

        UserResponse response = userService.createUser(request);

        assertThat(response).isNotNull();
        assertThat(response.getAllocatedDays()).isEqualTo(360);
        assertThat(dealer.getUsedAllocatedDays()).isEqualTo(360);
        assertThat(dealer.getRemainingAllocatedDays()).isEqualTo(3240);
        verify(dealerRepository).save(dealer);
        verify(dealerPackageHistoryRepository).save(any(DealerPackageHistory.class));
    }

    @Test
    @DisplayName("Case 2: Tạo account USER thứ 11 khi gói chỉ có tối đa 10 account bị từ chối")
    void testCreateUser_ExceedMaxAccounts_ThrowsBadRequest() {
        when(currentUserService.getCurrentUser()).thenReturn(agentA);
        when(currentUserService.isAgent()).thenReturn(true);
        when(currentUserService.isAdmin()).thenReturn(false);
        when(dealerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(dealer));
        when(dealerRepository.countActiveUserAccountsByDealerId(1L)).thenReturn(10L); // Already 10

        CreateUserRequest request = CreateUserRequest.builder()
                .username("client11")
                .email("client11@test.com")
                .password("Password123!")
                .role(Role.USER)
                .allocatedDays(180)
                .build();

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Đại lý đã đạt giới hạn tối đa 10 tài khoản");

        verify(keycloakUserService, never()).createUser(any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    @DisplayName("Case 4 & 5: Phân bổ vượt quá quỹ ngày còn lại bị từ chối")
    void testCreateUser_ExceedRemainingDays_ThrowsBadRequest() {
        dealer.setUsedAllocatedDays(3500); // Only 100 days remaining
        when(currentUserService.getCurrentUser()).thenReturn(agentA);
        when(currentUserService.isAgent()).thenReturn(true);
        when(currentUserService.isAdmin()).thenReturn(false);
        when(dealerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(dealer));
        when(dealerRepository.countActiveUserAccountsByDealerId(1L)).thenReturn(5L);

        CreateUserRequest request = CreateUserRequest.builder()
                .username("client_over")
                .email("client_over@test.com")
                .password("Password123!")
                .role(Role.USER)
                .allocatedDays(180) // 180 > 100
                .build();

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Quỹ ngày còn lại của đại lý không đủ (còn lại: 100 ngày, yêu cầu: 180 ngày)");

        verify(keycloakUserService, never()).createUser(any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    @DisplayName("Case 6: Thay đổi thời hạn USER được tính lại delta đúng")
    void testUpdateUser_ChangeAllocatedDays_AdjustsQuotaCorrectly() {
        dealer.setUsedAllocatedDays(360);
        User existingUser = User.builder()
                .id(501L)
                .username("client01@test.com")
                .email("client01@test.com")
                .role(Role.USER)
                .agent(agentA)
                .dealer(dealer)
                .allocatedDays(360)
                .expirationDate(LocalDate.now().plusDays(360))
                .enabled(true)
                .deleted(false)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(agentA);
        when(currentUserService.isAgent()).thenReturn(true);
        when(currentUserService.isAdmin()).thenReturn(false);
        when(userRepository.findById(501L)).thenReturn(Optional.of(existingUser));
        when(dealerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(dealer));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Tăng từ 360 lên 540 ngày (+180 ngày)
        UpdateUserRequest extendRequest = UpdateUserRequest.builder()
                .email("client01@test.com")
                .allocatedDays(540)
                .build();

        UserResponse res1 = userService.updateUser(501L, extendRequest);
        assertThat(res1.getAllocatedDays()).isEqualTo(540);
        assertThat(dealer.getUsedAllocatedDays()).isEqualTo(540); // 360 + 180

        // Giảm từ 540 xuống 180 ngày (-360 ngày)
        UpdateUserRequest reduceRequest = UpdateUserRequest.builder()
                .email("client01@test.com")
                .allocatedDays(180)
                .build();

        UserResponse res2 = userService.updateUser(501L, reduceRequest);
        assertThat(res2.getAllocatedDays()).isEqualTo(180);
        assertThat(dealer.getUsedAllocatedDays()).isEqualTo(180); // 540 - 360
    }

    @Test
    @DisplayName("Case 7: Gói hết hạn không được tạo account mới")
    void testCreateUser_PackageExpired_ThrowsBadRequest() {
        dealer.setPackageExpiryDate(LocalDate.now().minusDays(1)); // Expired yesterday
        when(currentUserService.getCurrentUser()).thenReturn(agentA);
        when(currentUserService.isAgent()).thenReturn(true);
        when(currentUserService.isAdmin()).thenReturn(false);
        when(dealerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(dealer));

        CreateUserRequest request = CreateUserRequest.builder()
                .username("client_exp")
                .email("client_exp@test.com")
                .password("Password123!")
                .role(Role.USER)
                .allocatedDays(360)
                .build();

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Gói đại lý đã hết hạn");
    }

    @Test
    @DisplayName("Case 8: Gói bị đình chỉ (SUSPENDED) không được tạo account")
    void testCreateUser_PackageSuspended_ThrowsBadRequest() {
        dealer.setPackageStatus("SUSPENDED");
        when(currentUserService.getCurrentUser()).thenReturn(agentA);
        when(currentUserService.isAgent()).thenReturn(true);
        when(currentUserService.isAdmin()).thenReturn(false);
        when(dealerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(dealer));

        CreateUserRequest request = CreateUserRequest.builder()
                .username("client_sus")
                .email("client_sus@test.com")
                .password("Password123!")
                .role(Role.USER)
                .allocatedDays(360)
                .build();

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Gói đại lý đang ở trạng thái không hoạt động (SUSPENDED)");
    }

    @Test
    @DisplayName("Case 9: Agent A không thể sử dụng gói của Agent B")
    void testCreateUser_AgentUsingDifferentDealer_ThrowsForbidden() {
        when(currentUserService.getCurrentUser()).thenReturn(agentA);
        when(currentUserService.isAgent()).thenReturn(true);
        when(currentUserService.isAdmin()).thenReturn(false);

        CreateUserRequest request = CreateUserRequest.builder()
                .username("client_diff")
                .email("client_diff@test.com")
                .password("Password123!")
                .role(Role.USER)
                .dealerId(999L)
                .allocatedDays(360)
                .build();

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Đại lý không được phép thêm người dùng thuộc đại lý khác");
    }

    @Test
    @DisplayName("Case 10: Request không thể truyền số ngày âm hoặc 0")
    void testCreateUser_InvalidAllocatedDays_ThrowsBadRequest() {
        when(currentUserService.getCurrentUser()).thenReturn(agentA);
        when(currentUserService.isAgent()).thenReturn(true);
        when(currentUserService.isAdmin()).thenReturn(false);
        when(dealerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(dealer));

        CreateUserRequest requestZero = CreateUserRequest.builder()
                .username("client_zero")
                .email("client_zero@test.com")
                .password("Password123!")
                .role(Role.USER)
                .allocatedDays(0)
                .build();

        assertThatThrownBy(() -> userService.createUser(requestZero))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Số ngày phân bổ phải lớn hơn 0");
    }

    @Test
    @DisplayName("Case 11: Tạo đại lý mới tự động tạo tài khoản AGENT và liên kết 1-1")
    void testCreateDealer_AutoCreatesAgent_Success() {
        when(dealerRepository.existsByCodeAndDeletedFalse(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(keycloakUserService.createUser(any(), any(), any(), eq(Role.AGENT), eq(true), any())).thenReturn("kc-agent-01");

        when(dealerRepository.save(any(Dealer.class))).thenAnswer(inv -> {
            Dealer d = inv.getArgument(0);
            if (d.getId() == null) d.setId(99L);
            return d;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) u.setId(777L);
            return u;
        });

        CreateDealerRequest req = new CreateDealerRequest();
        req.setCode("DL-NEW");
        req.setName("Đại lý Mới Toanh");
        req.setEmail("contact@newdealer.com");
        req.setAgentUsername("agent_new");
        req.setAgentEmail("agent_new@dealer.com");
        req.setAgentPassword("AgentPass123!");
        req.setMaxAccounts(15);
        req.setTotalAllocatedDays(5400);

        DealerResponse res = dealerService.createDealer(req);

        assertThat(res).isNotNull();
        assertThat(res.getAgentUsername()).isEqualTo("agent_new");
        assertThat(res.getAgentEmail()).isEqualTo("agent_new@dealer.com");
        verify(keycloakUserService).createUser("agent_new", "agent_new@dealer.com", "AgentPass123!", Role.AGENT, true, "Đại lý Mới Toanh");
        verify(dealerPackageHistoryRepository).save(any(DealerPackageHistory.class));
    }

    @Test
    @DisplayName("Case 12: Khi lỗi DB, rollback Keycloak và không báo thành công giả")
    void testCreateUser_DatabaseFailure_RollsBackKeycloakUser() {
        when(currentUserService.getCurrentUser()).thenReturn(agentA);
        when(currentUserService.isAgent()).thenReturn(true);
        when(currentUserService.isAdmin()).thenReturn(false);
        when(dealerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(dealer));
        when(keycloakUserService.createUser(any(), any(), any(), any(), anyBoolean(), any())).thenReturn("kc-test-fail");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB Connection Timeout"));

        CreateUserRequest request = CreateUserRequest.builder()
                .username("client_fail")
                .email("client_fail@test.com")
                .password("Password123!")
                .role(Role.USER)
                .allocatedDays(180)
                .build();

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB Connection Timeout");

        verify(keycloakUserService).deleteUser("kc-test-fail");
    }

    @Test
    @DisplayName("Case 13: Không cho phép tạo tài khoản AGENT thứ hai cho cùng đại lý")
    void testCreateUser_RoleAgent_CannotCreateDuplicateAgent() {
        User adminUser = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(currentUserService.isAdmin()).thenReturn(true);
        when(dealerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(dealer));
        when(userRepository.existsByDealerIdAndRoleAndDeletedFalse(1L, Role.AGENT)).thenReturn(true);

        CreateUserRequest request = CreateUserRequest.builder()
                .username("agent_second")
                .email("agent_second@test.com")
                .password("Password123!")
                .role(Role.AGENT)
                .dealerId(1L)
                .build();

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("đã có tài khoản AGENT liên kết");
    }

    @Test
    @DisplayName("Case 14: Tạo tài khoản AGENT không được tính vào quota account hoặc Quỹ ngày")
    void testCreateUser_RoleAgent_DoesNotConsumeQuotaOrDays() {
        User adminUser = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        Dealer freshDealer = Dealer.builder()
                .id(2L)
                .code("DL02")
                .name("Đại lý Chưa có Agent")
                .maxAccounts(10)
                .totalAllocatedDays(3600)
                .usedAllocatedDays(0)
                .packageStatus("ACTIVE")
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(currentUserService.isAdmin()).thenReturn(true);
        when(dealerRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(freshDealer));
        when(userRepository.existsByDealerIdAndRoleAndDeletedFalse(2L, Role.AGENT)).thenReturn(false);
        when(keycloakUserService.createUser(any(), any(), any(), eq(Role.AGENT), eq(true), any())).thenReturn("kc-agent-fresh");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateUserRequest request = CreateUserRequest.builder()
                .username("fresh_agent")
                .email("fresh_agent@dealer.com")
                .password("Password123!")
                .role(Role.AGENT)
                .dealerId(2L)
                .allocatedDays(500) // Even if sent, must NOT consume
                .build();

        UserResponse res = userService.createUser(request);

        assertThat(res).isNotNull();
        assertThat(res.getAllocatedDays()).isEqualTo(0);
        assertThat(freshDealer.getUsedAllocatedDays()).isEqualTo(0); // Quỹ ngày KHÔNG bị trừ
        assertThat(freshDealer.getAgentUser()).isNotNull();
    }

    @Test
    @DisplayName("Admin cập nhật gói không được giảm xuống dưới số lượng hoặc số ngày đã sử dụng")
    void testAdminUpdateDealerPackage_CannotReduceBelowUsage() {
        dealer.setUsedAllocatedDays(1500);
        when(dealerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(dealer));
        when(dealerRepository.countActiveUserAccountsByDealerId(1L)).thenReturn(7L);

        UpdateDealerRequest invalidAccountReq = new UpdateDealerRequest();
        invalidAccountReq.setCode("DL01");
        invalidAccountReq.setName("Đại lý Miền Bắc");
        invalidAccountReq.setMaxAccounts(5); // 5 < 7 already created
        invalidAccountReq.setTotalAllocatedDays(3600);

        assertThatThrownBy(() -> dealerService.updateDealer(1L, invalidAccountReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không thể giảm số lượng account tối đa xuống 5 vì đại lý đã tạo 7 account Thợ cắt");

        UpdateDealerRequest invalidDaysReq = new UpdateDealerRequest();
        invalidDaysReq.setCode("DL01");
        invalidDaysReq.setName("Đại lý Miền Bắc");
        invalidDaysReq.setMaxAccounts(10);
        invalidDaysReq.setTotalAllocatedDays(1000); // 1000 < 1500 already used

        assertThatThrownBy(() -> dealerService.updateDealer(1L, invalidDaysReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không thể giảm tổng quỹ ngày xuống 1000 ngày vì đại lý đã phân bổ 1500 ngày cho Thợ cắt");
    }

    @Test
    @DisplayName("Tạo đại lý: packageStartDate trong quá khứ -> ném PAST_DATE_NOT_ALLOWED")
    void testCreateDealer_PastStartDate_ThrowsBadRequest() {
        CreateDealerRequest req = new CreateDealerRequest();
        req.setCode("DL-PAST-START");
        req.setName("Đại lý Ngày Quá Khứ");
        req.setEmail("contact@paststart.com");
        req.setPackageStartDate(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> dealerService.createDealer(req))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getCode()).isEqualTo("PAST_DATE_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("Tạo đại lý: packageExpiryDate trước packageStartDate -> ném INVALID_DATE_RANGE")
    void testCreateDealer_ExpiryBeforeStart_ThrowsBadRequest() {
        CreateDealerRequest req = new CreateDealerRequest();
        req.setCode("DL-EXP-BEFORE-START");
        req.setName("Đại lý Hạn Ngược");
        req.setEmail("contact@expbeforestart.com");
        req.setPackageStartDate(LocalDate.now().plusDays(10));
        req.setPackageExpiryDate(LocalDate.now().plusDays(5));

        assertThatThrownBy(() -> dealerService.createDealer(req))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getCode()).isEqualTo("INVALID_DATE_RANGE"));
    }

    @Test
    @DisplayName("Cập nhật đại lý: đổi packageExpiryDate sang ngày quá khứ -> ném PAST_DATE_NOT_ALLOWED")
    void testUpdateDealer_PastExpiryDate_ThrowsBadRequest() {
        dealer.setPackageExpiryDate(LocalDate.now().plusDays(30));
        when(dealerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(dealer));

        UpdateDealerRequest req = new UpdateDealerRequest();
        req.setCode("DL01");
        req.setName("Đại lý Miền Bắc");
        req.setPackageExpiryDate(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> dealerService.updateDealer(1L, req))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getCode()).isEqualTo("PAST_DATE_NOT_ALLOWED"));
    }
}
