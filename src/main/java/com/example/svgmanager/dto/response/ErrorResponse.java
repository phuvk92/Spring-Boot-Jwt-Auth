package com.example.svgmanager.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Standardized error response")
public class ErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(example = "2026-09-13T10:00:00")
    private LocalDateTime timestamp;

    @Schema(example = "400")
    private int status;

    @Schema(example = "Bad Request")
    private String error;

    @Schema(example = "Only SVG files are allowed")
    private String message;

    @Schema(example = "/api/svg/upload")
    private String path;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Mã lỗi máy đọc được — chỉ có khi client cần phân nhánh", example = "SESSION_LIMIT")
    private String code;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Trường dữ liệu gây ra lỗi (nếu có, vd username, email, password)", example = "username")
    private String field;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Tham số bổ sung cho lỗi")
    private java.util.Map<String, Object> params;

    public ErrorResponse() {
    }

    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path, String code) {
        this(timestamp, status, error, message, path);
        this.code = code;
    }

    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path, String code, String field) {
        this(timestamp, status, error, message, path, code);
        this.field = field;
    }

    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path, String code, String field, java.util.Map<String, Object> params) {
        this(timestamp, status, error, message, path, code, field);
        this.params = params;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private String code;
        private String field;
        private java.util.Map<String, Object> params;

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder field(String field) {
            this.field = field;
            return this;
        }

        public Builder params(java.util.Map<String, Object> params) {
            this.params = params;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(timestamp, status, error, message, path, code, field, params);
        }
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public java.util.Map<String, Object> getParams() {
        return params;
    }

    public void setParams(java.util.Map<String, Object> params) {
        this.params = params;
    }
}
