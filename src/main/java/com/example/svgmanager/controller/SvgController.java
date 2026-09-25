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
@Tag(name = "SVG Management", description = "Operations for managing SVG files.")
public class SvgController {

    private final SvgService svgService;

    public SvgController(SvgService svgService) {
        this.svgService = svgService;
    }

    @PostMapping(value = {"", "/upload"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Upload and sanitize SVG file", description = "Uploads a new SVG file associated with a category. ADMIN or AGENT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "SVG file uploaded and sanitized successfully",
                    content = @Content(schema = @Schema(implementation = SvgResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or category",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or AGENT role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SvgResponse> uploadSvg(
            @Parameter(description = "SVG file to upload", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Category ID from catalog", required = true)
            @RequestParam("categoryId") Long categoryId
    ) {
        SvgResponse response = svgService.uploadSvg(file, categoryId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "List SVG files with pagination, keyword and category filters", description = "Returns paginated list of SVG files.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SVG files retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<SvgResponse>> getSvgFiles(
            @Parameter(description = "Search keyword in filename") @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by Category ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter by uploader user ID") @RequestParam(required = false) Long uploadedBy,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC, DESC)") @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        PageResponse<SvgResponse> response = svgService.getSvgFiles(keyword, categoryId, uploadedBy, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Get SVG file metadata by ID", description = "Retrieves SVG file details including category.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SVG file details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SvgResponse.class))),
            @ApiResponse(responseCode = "404", description = "SVG file not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SvgResponse> getSvgById(@PathVariable Long id) {
        SvgResponse response = svgService.getSvgFileById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update SVG metadata", description = "Updates category for an SVG file. ADMIN only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SVG file updated successfully",
                    content = @Content(schema = @Schema(implementation = SvgResponse.class))),
            @ApiResponse(responseCode = "404", description = "SVG file not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SvgResponse> updateSvg(
            @PathVariable Long id,
            @Parameter(description = "New Category ID") @RequestParam("categoryId") Long categoryId
    ) {
        SvgResponse response = svgService.updateSvg(id, categoryId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = {"/{id}/preview", "/{id}/content"})
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Preview SVG file content", description = "Streams SVG content for browser inline preview.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SVG content stream",
                    content = @Content(mediaType = "image/svg+xml")),
            @ApiResponse(responseCode = "404", description = "SVG file not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Resource> previewSvg(@PathVariable Long id) {
        Resource resource = svgService.previewSvg(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/svg+xml"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + svgService.getOriginalFilename(id) + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Download SVG file", description = "Downloads SVG file as attachment.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SVG file attachment binary",
                    content = @Content(mediaType = "image/svg+xml")),
            @ApiResponse(responseCode = "404", description = "SVG file not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Resource> downloadSvg(@PathVariable Long id) {
        Resource resource = svgService.downloadSvg(id);
        String originalFilename = svgService.getOriginalFilename(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/svg+xml"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFilename + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Delete SVG file", description = "Deletes SVG record and underlying file from disk. ADMIN or AGENT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "SVG file deleted successfully"),
            @ApiResponse(responseCode = "404", description = "SVG file not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or AGENT role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteSvg(@PathVariable Long id) {
        svgService.deleteSvg(id);
        return ResponseEntity.noContent().build();
    }
}
