package com.example.svgmanager.exception;

import com.example.svgmanager.config.WebMvcConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerLocalizationTest {

    @RestController
    static class TestController {
        @GetMapping("/test/bad-credentials")
        public void badCredentials() {
            throw new BadCredentialsException("Invalid user or pass");
        }

        @GetMapping("/test/account-disabled")
        public void accountDisabled() {
            throw new DisabledException("Disabled");
        }

        @GetMapping("/test/one-vehicle")
        public void oneVehicle() {
            throw new BadRequestException("Mỗi file chỉ chứa 1 xe", ErrorCodes.ONE_VEHICLE_PER_FILE);
        }

        @GetMapping("/test/session-limit")
        public void sessionLimit() {
            throw new BadRequestException("Session limit", ErrorCodes.SESSION_LIMIT);
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        WebMvcConfig config = new WebMvcConfig(null);
        MessageSource messageSource = config.messageSource();
        GlobalExceptionHandler handler = new GlobalExceptionHandler(messageSource);

        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(handler)
                .setLocaleResolver(config.localeResolver())
                .build();
    }

    @Test
    @DisplayName("Accept-Language: vi resolves Vietnamese error messages and codes")
    void viLocale_ResolvesVietnameseMessage() throws Exception {
        mockMvc.perform(get("/test/bad-credentials")
                        .header("Accept-Language", "vi")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("BAD_CREDENTIALS")))
                .andExpect(jsonPath("$.message", is("Tên đăng nhập hoặc mật khẩu không chính xác.")));

        mockMvc.perform(get("/test/one-vehicle")
                        .header("Accept-Language", "vi")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(ErrorCodes.ONE_VEHICLE_PER_FILE)))
                .andExpect(jsonPath("$.message", is("Mỗi file chỉ được chứa mẫu của một loại xe.")));
    }

    @Test
    @DisplayName("Accept-Language: en resolves English error messages and codes")
    void enLocale_ResolvesEnglishMessage() throws Exception {
        mockMvc.perform(get("/test/bad-credentials")
                        .header("Accept-Language", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("BAD_CREDENTIALS")))
                .andExpect(jsonPath("$.message", is("Invalid username or password.")));

        mockMvc.perform(get("/test/one-vehicle")
                        .header("Accept-Language", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(ErrorCodes.ONE_VEHICLE_PER_FILE)))
                .andExpect(jsonPath("$.message", is("Each SVG file may only contain patterns for one vehicle model.")));

        mockMvc.perform(get("/test/session-limit")
                        .header("Accept-Language", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(ErrorCodes.SESSION_LIMIT)))
                .andExpect(jsonPath("$.message", is("Account has reached the maximum number of active devices.")));
    }
}
