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

@RestController
@RequestMapping("/api/svg")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "SVG File Management", description = "Endpoints for uploading, listing, previewing, downloading, and deleting SVG files")
public class SvgController {

    private final SvgService svgService;

    public SvgController(SvgService svgService) {
        this.svgService = svgService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Upload SVG file", description = "Uploads and sanitizes a valid SVG file. Accessible by ADMIN and AGENT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "SVG uploaded successfully",
                    content = @Content(schema = @Schema(implementation = SvgResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or malicious SVG content detected",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or AGENT role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SvgResponse> uploadSvg(
            @Parameter(description = "SVG file to upload (.svg)", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        SvgResponse response = svgService.uploadSvg(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "List SVG files", description = "Retrieves a paginated list of SVG files with optional filtering and sorting.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SVG files retrieved successfully")
    })
    public ResponseEntity<PageResponse<SvgResponse>> getSvgFiles(
            @Parameter(description = "Filter by filename keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by uploader user ID") @RequestParam(required = false) Long uploadedBy,
            @Parameter(description = "Page index (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC, DESC)") @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        PageResponse<SvgResponse> response = svgService.getSvgFiles(keyword, uploadedBy, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Get SVG metadata detail", description = "Retrieves metadata for a specific SVG file.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SVG metadata retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SvgResponse.class))),
            @ApiResponse(responseCode = "404", description = "SVG file not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SvgResponse> getSvgById(@PathVariable Long id) {
        SvgResponse response = svgService.getSvgFileById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Preview SVG inline", description = "Returns sanitized SVG content with inline Content-Disposition and security headers.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SVG content returned for preview"),
            @ApiResponse(responseCode = "404", description = "SVG file not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Resource> previewSvg(@PathVariable Long id) {
        Resource resource = svgService.previewSvg(id);
        String filename = svgService.getOriginalFilename(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/svg+xml; charset=utf-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Download SVG file", description = "Streams SVG file download with attachment Content-Disposition.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File streamed for download"),
            @ApiResponse(responseCode = "404", description = "SVG file not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Resource> downloadSvg(@PathVariable Long id) {
        Resource resource = svgService.downloadSvg(id);
        String filename = svgService.getOriginalFilename(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Delete SVG file", description = "Deletes physical file and database metadata. ADMIN or AGENT (scoped).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "SVG file deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or AGENT role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "SVG file not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteSvg(@PathVariable Long id) {
        svgService.deleteSvg(id);
        return ResponseEntity.noContent().build();
    }
}
