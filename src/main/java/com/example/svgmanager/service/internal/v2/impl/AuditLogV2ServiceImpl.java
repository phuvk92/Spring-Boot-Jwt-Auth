package com.example.svgmanager.service.internal.v2.impl;

import com.example.svgmanager.dto.internal.v2.InternalApiV2AuditEvent;
import com.example.svgmanager.dto.response.AuditLogV2Response;
import com.example.svgmanager.dto.response.AuditLogV2StatsResponse;
import com.example.svgmanager.entity.v2.AuditLogV2;
import com.example.svgmanager.repository.internal.v2.AuditLogV2Repository;
import com.example.svgmanager.repository.internal.v2.AuditLogV2Specification;
import com.example.svgmanager.service.internal.v2.AuditLogV2Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuditLogV2ServiceImpl implements AuditLogV2Service {

    private static final Logger log = LoggerFactory.getLogger(AuditLogV2ServiceImpl.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditLogV2Repository repository;

    public AuditLogV2ServiceImpl(AuditLogV2Repository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAuditLog(InternalApiV2AuditEvent event) {
        if (event == null) {
            return;
        }

        try {
            AuditLogV2 auditLog = AuditLogV2.builder()
                    .requestId(sanitize(event.requestId(), 100, "unknown"))
                    .traceId(sanitize(event.traceId(), 100, null))
                    .clientId(sanitize(event.clientId(), 100, null))
                    .clientName(sanitize(event.clientName(), 255, null))
                    .principalType(sanitize(event.principalType(), 50, "ANONYMOUS"))
                    .userId(event.userId())
                    .keycloakUserId(sanitize(event.keycloakUserId(), 100, null))
                    .username(sanitize(event.username(), 100, null))
                    .role(sanitize(event.role(), 50, null))
                    .dealerId(event.dealerId())
                    .dealerName(sanitize(event.dealerName(), 255, null))
                    .httpMethod(sanitize(event.httpMethod(), 10, "UNKNOWN"))
                    .endpoint(sanitize(event.endpoint(), 500, "/"))
                    .apiVersion(sanitize(event.apiVersion(), 20, "V2"))
                    .httpStatus(event.httpStatus() != null ? event.httpStatus() : 500)
                    .result(sanitize(event.result(), 20, "FAIL"))
                    .errorCode(sanitize(event.errorCode(), 100, null))
                    .executionTimeMs(event.executionTimeMs() != null ? event.executionTimeMs() : 0L)
                    .clientIp(sanitize(event.clientIp(), 50, null))
                    .userAgent(sanitize(event.userAgent(), 500, null))
                    .requestSizeBytes(event.requestSizeBytes())
                    .responseSizeBytes(event.responseSizeBytes())
                    .createdAt(LocalDateTime.now())
                    .build();

            repository.save(auditLog);

            log.debug("[AUDIT_V2_SAVED] id={}, requestId='{}', client='{}', user='{}', method={}, endpoint={}, status={}, latency={}ms",
                    auditLog.getId(), auditLog.getRequestId(), auditLog.getClientId(), auditLog.getUsername(),
                    auditLog.getHttpMethod(), auditLog.getEndpoint(), auditLog.getHttpStatus(), auditLog.getExecutionTimeMs());

        } catch (Exception ex) {
            log.error("[AUDIT_LOG_V2_PERSIST_ERROR] Failed to save audit log for requestId '{}': {}",
                    event.requestId(), ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogV2Response> getAuditLogs(
            String keyword,
            String clientId,
            String clientName,
            String username,
            Long userId,
            String role,
            Long dealerId,
            String endpoint,
            String httpMethod,
            Integer httpStatus,
            String result,
            String requestId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        Specification<AuditLogV2> spec = AuditLogV2Specification.filter(
                keyword, clientId, clientName, username, userId, role,
                dealerId, endpoint, httpMethod, httpStatus, result, requestId,
                from, to
        );

        return repository.findAll(spec, pageable).map(AuditLogV2Response::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuditLogV2Response> getAuditLogById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return repository.findById(id).map(AuditLogV2Response::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogV2StatsResponse getStats(LocalDateTime from, LocalDateTime to, String clientId, Long dealerId) {
        long total = repository.countFiltered(from, to, clientId, dealerId);

        List<Map<String, Object>> resultCounts = repository.countByResultGroup(from, to, clientId, dealerId);
        long success = 0;
        long fail = 0;
        for (Map<String, Object> map : resultCounts) {
            String res = (String) map.get("result");
            Number cnt = (Number) map.get("cnt");
            if ("SUCCESS".equalsIgnoreCase(res)) {
                success = cnt != null ? cnt.longValue() : 0;
            } else if ("FAIL".equalsIgnoreCase(res)) {
                fail = cnt != null ? cnt.longValue() : 0;
            }
        }

        double successRate = total > 0 ? Math.round((success * 1000.0) / total) / 10.0 : 100.0;

        // Top Endpoints
        List<Map<String, Object>> topEpRows = repository.findTopEndpoints(from, to, clientId, dealerId, PageRequest.of(0, 5));
        List<AuditLogV2StatsResponse.EndpointStat> topEndpoints = new ArrayList<>();
        for (Map<String, Object> row : topEpRows) {
            String ep = (String) row.get("endpoint");
            String method = (String) row.get("httpMethod");
            Number cnt = (Number) row.get("cnt");
            Number avgMs = (Number) row.get("avgMs");
            topEndpoints.add(new AuditLogV2StatsResponse.EndpointStat(
                    ep,
                    method,
                    cnt != null ? cnt.longValue() : 0,
                    avgMs != null ? Math.round(avgMs.doubleValue() * 10.0) / 10.0 : 0.0
            ));
        }

        // Top Clients
        List<Map<String, Object>> topClientsRows = repository.findTopClients(from, to, clientId, dealerId, PageRequest.of(0, 5));
        List<AuditLogV2StatsResponse.ClientStat> topClients = new ArrayList<>();
        for (Map<String, Object> row : topClientsRows) {
            String cId = (String) row.get("clientId");
            String cName = (String) row.get("clientName");
            Number cnt = (Number) row.get("cnt");
            topClients.add(new AuditLogV2StatsResponse.ClientStat(
                    cId,
                    cName,
                    cnt != null ? cnt.longValue() : 0
            ));
        }

        // Top Users
        List<Map<String, Object>> topUsersRows = repository.findTopUsers(from, to, clientId, dealerId, PageRequest.of(0, 5));
        List<AuditLogV2StatsResponse.UserStat> topUsers = new ArrayList<>();
        for (Map<String, Object> row : topUsersRows) {
            String uName = (String) row.get("username");
            String uRole = (String) row.get("role");
            Number cnt = (Number) row.get("cnt");
            topUsers.add(new AuditLogV2StatsResponse.UserStat(
                    uName,
                    uRole,
                    cnt != null ? cnt.longValue() : 0
            ));
        }

        return new AuditLogV2StatsResponse(total, success, fail, successRate, topEndpoints, topClients, topUsers);
    }

    @Override
    @Transactional(readOnly = true)
    public void exportCsv(
            String keyword,
            String clientId,
            String clientName,
            String username,
            Long userId,
            String role,
            Long dealerId,
            String endpoint,
            String httpMethod,
            Integer httpStatus,
            String result,
            String requestId,
            LocalDateTime from,
            LocalDateTime to,
            PrintWriter writer
    ) {
        // Write UTF-8 BOM for Microsoft Excel compatibility
        writer.write('\uFEFF');

        // Header
        writer.println("ID,Request ID,Trace ID,Client ID,Client Name,Principal Type,User ID,Username,Role,Dealer ID,Dealer Name,HTTP Method,Endpoint,API Version,HTTP Status,Result,Error Code,Execution Time (ms),Client IP,User Agent,Request Size (bytes),Response Size (bytes),Created At");

        Specification<AuditLogV2> spec = AuditLogV2Specification.filter(
                keyword, clientId, clientName, username, userId, role,
                dealerId, endpoint, httpMethod, httpStatus, result, requestId,
                from, to
        );

        // Limit export size to 5000 records to protect heap
        Pageable pageable = PageRequest.of(0, 5000, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLogV2> page = repository.findAll(spec, pageable);

        for (AuditLogV2 log : page.getContent()) {
            StringBuilder sb = new StringBuilder();
            appendCsv(sb, log.getId()).append(",");
            appendCsv(sb, log.getRequestId()).append(",");
            appendCsv(sb, log.getTraceId()).append(",");
            appendCsv(sb, log.getClientId()).append(",");
            appendCsv(sb, log.getClientName()).append(",");
            appendCsv(sb, log.getPrincipalType()).append(",");
            appendCsv(sb, log.getUserId()).append(",");
            appendCsv(sb, log.getUsername()).append(",");
            appendCsv(sb, log.getRole()).append(",");
            appendCsv(sb, log.getDealerId()).append(",");
            appendCsv(sb, log.getDealerName()).append(",");
            appendCsv(sb, log.getHttpMethod()).append(",");
            appendCsv(sb, log.getEndpoint()).append(",");
            appendCsv(sb, log.getApiVersion()).append(",");
            appendCsv(sb, log.getHttpStatus()).append(",");
            appendCsv(sb, log.getResult()).append(",");
            appendCsv(sb, log.getErrorCode()).append(",");
            appendCsv(sb, log.getExecutionTimeMs()).append(",");
            appendCsv(sb, log.getClientIp()).append(",");
            appendCsv(sb, log.getUserAgent()).append(",");
            appendCsv(sb, log.getRequestSizeBytes()).append(",");
            appendCsv(sb, log.getResponseSizeBytes()).append(",");
            appendCsv(sb, log.getCreatedAt() != null ? log.getCreatedAt().format(ISO_FORMATTER) : null);
            writer.println(sb);
        }
        writer.flush();
    }

    private StringBuilder appendCsv(StringBuilder sb, Object val) {
        if (val == null) {
            return sb;
        }
        String s = val.toString();
        // Prevent formula injection in spreadsheet software
        if (s.startsWith("=") || s.startsWith("+") || s.startsWith("-") || s.startsWith("@")) {
            s = "'" + s;
        }
        // Escape quotes
        s = s.replace("\"", "\"\"");
        sb.append("\"").append(s).append("\"");
        return sb;
    }

    private String sanitize(String val, int maxLen, String defaultVal) {
        if (!StringUtils.hasText(val)) {
            return defaultVal;
        }
        // Remove line breaks to prevent log and header injection
        String clean = val.replaceAll("[\\r\\n]", " ").trim();
        if (clean.length() > maxLen) {
            return clean.substring(0, maxLen);
        }
        return clean;
    }
}
