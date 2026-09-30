package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.response.BrandingResponse;
import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.entity.DealerBranding;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.repository.DealerBrandingRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.BrandingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.Set;

/**
 * BR-10/BR-30/BR-32 — trả thương hiệu của đúng đại lý của user; mọi trường thiếu hoặc
 * sai đều rơi về mặc định Pcut thay vì ném lỗi (BR-13: lỗi thương hiệu không được chặn đăng nhập).
 */
@Service
public class BrandingServiceImpl implements BrandingService {

    private static final Logger log = LoggerFactory.getLogger(BrandingServiceImpl.class);

    /** Trần asset pha 2 (BR-12). DB đã có CHECK — đây là lớp phòng thủ thứ hai. */
    private static final int MAX_LOGO_BYTES = 1024 * 1024;

    private static final Set<String> PRIMARY_COLORS =
            Set.of("#2563C9", "#7C3AED", "#2E7D5B", "#C2452D", "#35342F");
    private static final Set<String> THEMES = Set.of("light", "dark", "system");

    /** Bản mặc định Pcut (BR-02/BR-30/BR-32): công tắc bảo mật mặc định bật. */
    private static final BrandingResponse PCUT_DEFAULT = new BrandingResponse(
            "Pcut", null, null,
            "#2563C9", null, "system",
            false, true, true, null);

    private final CurrentUserService currentUserService;
    private final DealerBrandingRepository dealerBrandingRepository;

    public BrandingServiceImpl(CurrentUserService currentUserService,
                               DealerBrandingRepository dealerBrandingRepository) {
        this.currentUserService = currentUserService;
        this.dealerBrandingRepository = dealerBrandingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BrandingResponse getBrandingForCurrentUser() {
        User user = currentUserService.getCurrentUser();
        Dealer dealer = user.getDealer();
        if (dealer == null) {
            // BR-30 — user độc lập
            return PCUT_DEFAULT;
        }

        DealerBranding b = dealerBrandingRepository.findById(dealer.getId()).orElse(null);
        if (b == null) {
            // BR-32 — đại lý chưa cấu hình
            return PCUT_DEFAULT;
        }

        String logoPng = encodeLogo(b, dealer.getId());
        return new BrandingResponse(
                StringUtils.hasText(b.getDisplayName()) ? b.getDisplayName() : PCUT_DEFAULT.displayName(),
                b.getSlogan(),
                b.getHotline(),
                b.getPrimaryColor() != null && PRIMARY_COLORS.contains(b.getPrimaryColor())
                        ? b.getPrimaryColor() : PCUT_DEFAULT.primaryColor(),
                b.getSecondaryColor(),
                b.getTheme() != null && THEMES.contains(b.getTheme())
                        ? b.getTheme() : PCUT_DEFAULT.theme(),
                b.getAllowWorkerThemeToggle() != null
                        ? b.getAllowWorkerThemeToggle() : PCUT_DEFAULT.allowWorkerThemeToggle(),
                b.getShowDealerNameNextToLogo() != null
                        ? b.getShowDealerNameNextToLogo() : PCUT_DEFAULT.showDealerNameNextToLogo(),
                // Công tắc bảo mật phải nghiêng về phía an toàn khi không ai nói gì (BR-13 phủ RB-06)
                b.getProtectScreenCapture() != null
                        ? b.getProtectScreenCapture() : PCUT_DEFAULT.protectScreenCapture(),
                logoPng);
    }

    private String encodeLogo(DealerBranding b, Long dealerId) {
        byte[] png = b.getLogoPng();
        if (png == null || png.length == 0) {
            return null;
        }
        if (png.length > MAX_LOGO_BYTES) {
            // BR-12 — vượt trần thì bỏ asset và dùng mặc định, không trả lỗi
            log.warn("[BRANDING] dealer {} logo vượt 1MB ({} bytes) — bỏ qua, trả mặc định", dealerId, png.length);
            return null;
        }
        return Base64.getEncoder().encodeToString(png);
    }
}
