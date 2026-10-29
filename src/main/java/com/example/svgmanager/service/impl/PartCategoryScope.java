package com.example.svgmanager.service.impl;

import com.example.svgmanager.entity.Role;
import com.example.svgmanager.repository.UserPartCategoryPermissionRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.security.CurrentUserService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Danh mục kho mẫu (part_library_categories) mà người gọi được thấy trong Data Center của app thợ.
 * Admin gán quyền cho thợ theo danh mục kho mẫu (UserModal → partCategoryIds), nên bộ lọc
 * Danh mục và danh sách file phải đi theo đúng tập đó — bug client #21/#30.
 *
 * Chỉ ROLE_USER bị giới hạn, giống InternalSvgServiceImpl; ADMIN/AGENT thấy toàn kho.
 * Tra user theo sub của JWT, KHÔNG qua CurrentUserService.getCurrentUser(): hàm đó tự tạo
 * user khi chưa có, không chạy được trong giao dịch chỉ-đọc của các endpoint catalog.
 */
@Component
public class PartCategoryScope {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final UserPartCategoryPermissionRepository permissionRepository;

    public PartCategoryScope(CurrentUserService currentUserService,
                             UserRepository userRepository,
                             UserPartCategoryPermissionRepository permissionRepository) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
    }

    /**
     * Rỗng = không giới hạn. Có giá trị = chỉ các id này (có thể là danh sách rỗng: thợ chưa
     * được gán danh mục nào, hoặc chưa có bản ghi user).
     */
    public Optional<List<Long>> permittedCategoryIds() {
        if (currentUserService.getCurrentRole() != Role.USER) {
            return Optional.empty();
        }
        String sub = currentUserService.getCurrentKeycloakUserId();
        if (sub == null) {
            return Optional.of(List.of());
        }
        return Optional.of(userRepository.findByKeycloakUserId(sub)
                .map(u -> permissionRepository.findPartCategoryIdsByUserId(u.getId()))
                .orElse(List.of()));
    }
}
