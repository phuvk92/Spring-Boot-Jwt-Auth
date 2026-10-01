package com.example.svgmanager.controller;

import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.SvgResponse;
import com.example.svgmanager.service.SvgService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/svg")
@Tag(name = "SVG Patterns & Part Files", description = "Endpoints for managing SVG pattern and part files")
@SecurityRequirement(name = "Bearer Authentication")
public class SvgController {

    private final SvgService svgService;

    public SvgController(SvgService svgService) {
        this.svgService = svgService;
    }

    @PostMapping(value = {"", "/upload"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Upload single SVG file (legacy)", description = "Uploads a new SVG file. ADMIN or AGENT.")
    public ResponseEntity<SvgResponse> uploadSvg(
            @Parameter(description = "SVG file to upload", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        SvgResponse response = svgService.uploadSvg(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "List SVG files with pagination and filters", description = "Returns paginated list of SVG files. AGENT sees own uploads only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SVG files retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<SvgResponse>> getSvgFiles(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "DESC") String sortDirection
    ) {
        PageResponse<SvgResponse> response = svgService.getSvgFiles(
                keyword, status, page, size, sortBy, sortDirection
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Get SVG file metadata by ID", description = "Retrieves SVG file details.")
    public ResponseEntity<SvgResponse> getSvgById(@PathVariable Long id) {
        SvgResponse response = svgService.getSvgFileById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update SVG metadata", description = "Updates status for an SVG file. ADMIN only.")
    public ResponseEntity<SvgResponse> updateSvg(
            @PathVariable Long id,
            @Parameter(description = "New Status") @RequestParam("status") String status
    ) {
        SvgResponse response = svgService.updateSvg(id, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = {"/{id}/preview", "/{id}/content"})
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Preview SVG file content", description = "Streams SVG content for browser inline preview.")
    public ResponseEntity<Resource> previewSvg(
            @PathVariable Long id,
            @Parameter(description = "Bản bố cục: nested (đã xếp) hoặc raw (chưa xếp)")
            @RequestParam(value = "layout", required = false) String layout) {
        Resource resource = svgService.previewSvg(id, layout);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/svg+xml"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Download SVG file", description = "Downloads SVG file as attachment.")
    public ResponseEntity<Resource> downloadSvg(
            @PathVariable Long id,
            @Parameter(description = "Bản bố cục: nested (đã xếp) hoặc raw (chưa xếp)")
            @RequestParam(value = "layout", required = false) String layout) {
        Resource resource = svgService.downloadSvg(id, layout);
        String originalFilename = svgService.getOriginalFilename(id, layout);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFilename + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @GetMapping("/{id}/thumbnail")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Ảnh xem trước của file", description = "Trả thumbnail nếu file có; 404 khi chưa gắn.")
    public ResponseEntity<Resource> thumbnailSvg(@PathVariable Long id) {
        Resource resource = svgService.thumbnailSvg(id);
        return ResponseEntity.ok()
                .contentType(svgService.thumbnailContentType(id))
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Delete SVG file", description = "Deletes SVG record and underlying file from disk. ADMIN or AGENT owning file.")
    public ResponseEntity<Void> deleteSvg(@PathVariable Long id) {
        svgService.deleteSvg(id);
        return ResponseEntity.noContent().build();
    }
}
