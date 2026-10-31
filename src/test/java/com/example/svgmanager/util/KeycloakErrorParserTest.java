package com.example.svgmanager.util;

import com.example.svgmanager.exception.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakErrorParserTest {

    @Test
    @DisplayName("Should parse username invalid character error from Keycloak")
    void testUsernameInvalidCharacter() {
        String json = "{\"field\":\"username\",\"errorMessage\":\"error-username-invalid-character\",\"params\":[\"username\"]}";
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
                json.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        RuntimeException result = KeycloakErrorParser.parseException(ex, "createUser");

        assertThat(result).isInstanceOf(BadRequestException.class);
        BadRequestException badReq = (BadRequestException) result;
        assertThat(badReq.getCode()).isEqualTo(ErrorCodes.USERNAME_INVALID_FORMAT);
        assertThat(badReq.getField()).isEqualTo("username");
        assertThat(badReq.getMessage()).contains("Tên đăng nhập không hợp lệ");
    }

    @Test
    @DisplayName("Should parse username conflict (HTTP 409) from Keycloak")
    void testUsernameConflict() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.CONFLICT, "Conflict", HttpHeaders.EMPTY,
                "{\"errorMessage\":\"User exists with same username\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        RuntimeException result = KeycloakErrorParser.parseException(ex, "createUser");

        assertThat(result).isInstanceOf(ConflictException.class);
        ConflictException conflict = (ConflictException) result;
        assertThat(conflict.getCode()).isEqualTo(ErrorCodes.USERNAME_ALREADY_EXISTS);
        assertThat(conflict.getField()).isEqualTo("username");
        assertThat(conflict.getMessage()).contains("Tên đăng nhập đã tồn tại");
    }

    @Test
    @DisplayName("Should parse email conflict (HTTP 409 with email)")
    void testEmailConflict() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.CONFLICT, "Conflict", HttpHeaders.EMPTY,
                "{\"errorMessage\":\"User exists with same email\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        RuntimeException result = KeycloakErrorParser.parseException(ex, "createUser");

        assertThat(result).isInstanceOf(ConflictException.class);
        ConflictException conflict = (ConflictException) result;
        assertThat(conflict.getCode()).isEqualTo(ErrorCodes.EMAIL_ALREADY_EXISTS);
        assertThat(conflict.getField()).isEqualTo("email");
        assertThat(conflict.getMessage()).contains("Email đã được sử dụng");
    }

    @Test
    @DisplayName("Should parse email invalid format (HTTP 400)")
    void testEmailInvalidFormat() {
        String json = "{\"field\":\"email\",\"errorMessage\":\"error-email-invalid\",\"params\":[\"email\"]}";
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
                json.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        RuntimeException result = KeycloakErrorParser.parseException(ex, "updateUser");

        assertThat(result).isInstanceOf(BadRequestException.class);
        BadRequestException badReq = (BadRequestException) result;
        assertThat(badReq.getCode()).isEqualTo(ErrorCodes.EMAIL_INVALID_FORMAT);
        assertThat(badReq.getField()).isEqualTo("email");
        assertThat(badReq.getMessage()).contains("Địa chỉ email không hợp lệ");
    }

    @Test
    @DisplayName("Should parse password policy violation (HTTP 400)")
    void testPasswordPolicyViolation() {
        String json = "{\"errorMessage\":\"invalidPasswordMinLengthMessage\",\"params\":[8]}";
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
                json.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        RuntimeException result = KeycloakErrorParser.parseException(ex, "resetPassword");

        assertThat(result).isInstanceOf(BadRequestException.class);
        BadRequestException badReq = (BadRequestException) result;
        assertThat(badReq.getCode()).isEqualTo(ErrorCodes.PASSWORD_POLICY_VIOLATION);
        assertThat(badReq.getField()).isEqualTo("password");
        assertThat(badReq.getMessage()).contains("Mật khẩu không đáp ứng yêu cầu bảo mật");
    }

    @Test
    @DisplayName("Should parse network connection failure as ServiceUnavailableException")
    void testConnectionFailure() {
        ResourceAccessException ex = new ResourceAccessException("I/O error on POST request", new ConnectException("Connection refused"));

        RuntimeException result = KeycloakErrorParser.parseException(ex, "getAdminToken");

        assertThat(result).isInstanceOf(ServiceUnavailableException.class);
        ServiceUnavailableException svcEx = (ServiceUnavailableException) result;
        assertThat(svcEx.getCode()).isEqualTo(ErrorCodes.AUTH_SERVICE_UNAVAILABLE);
        assertThat(svcEx.getMessage()).contains("Hệ thống xác thực hiện đang tạm thời không khả dụng");
    }

    @Test
    @DisplayName("Should parse 503 Service Unavailable")
    void test503Unavailable() {
        HttpServerErrorException ex = HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", HttpHeaders.EMPTY,
                "".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        RuntimeException result = KeycloakErrorParser.parseException(ex, "callKeycloak");

        assertThat(result).isInstanceOf(ServiceUnavailableException.class);
        ServiceUnavailableException svcEx = (ServiceUnavailableException) result;
        assertThat(svcEx.getCode()).isEqualTo(ErrorCodes.AUTH_SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Should parse 404 User Not Found")
    void test404NotFound() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY,
                "".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        RuntimeException result = KeycloakErrorParser.parseException(ex, "findUser");

        assertThat(result).isInstanceOf(ResourceNotFoundException.class);
        ResourceNotFoundException notFound = (ResourceNotFoundException) result;
        assertThat(notFound.getCode()).isEqualTo(ErrorCodes.USER_NOT_FOUND);
    }
}
