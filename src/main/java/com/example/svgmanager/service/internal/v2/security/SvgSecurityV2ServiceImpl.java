package com.example.svgmanager.service.internal.v2.security;

import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.SvgPayloadTooLargeException;
import com.example.svgmanager.exception.SvgSecurityException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SvgSecurityV2ServiceImpl implements SvgSecurityV2Service {

    private static final Logger log = LoggerFactory.getLogger(SvgSecurityV2ServiceImpl.class);

    private final SvgSecurityProperties properties;
    private final FileSecurityScanner fileSecurityScanner;

    /**
     * Danh sách phần tử độc hại cấm tuyệt đối (gây XSS, code execution, HTML injection, etc.)
     */
    private static final Set<String> PROHIBITED_ELEMENTS = new HashSet<>(Arrays.asList(
            "script", "foreignobject", "iframe", "object", "embed", "applet",
            "frame", "frameset", "form", "input", "button", "textarea", "select",
            "meta", "link", "base", "audio", "video", "animate", "set",
            "animatemotion", "animatetransform", "handler", "listener", "html", "body"
    ));

    /**
     * Allowlist các phần tử SVG hợp lệ phục vụ máy cắt và hiển thị vector an toàn
     */
    private static final Set<String> ALLOWED_SVG_ELEMENTS = new HashSet<>(Arrays.asList(
            "svg", "g", "path", "rect", "circle", "ellipse", "line", "polyline",
            "polygon", "text", "tspan", "defs", "use", "symbol", "clippath",
            "mask", "pattern", "lineargradient", "radialgradient", "stop",
            "marker", "style", "desc", "title", "metadata"
    ));

    /** Pattern phát hiện khai báo DOCTYPE hoặc Entity (chống XXE, billion laughs, DTD injection) */
    private static final Pattern XXE_PATTERNS = Pattern.compile(
            "(?i)(<!DOCTYPE|<!ENTITY|\\bSYSTEM\\b|\\bPUBLIC\\b|<\\?xml-stylesheet)"
    );

    /** Pattern phát hiện CSS expressions hoặc protocols độc hại trong CSS/style */
    private static final Pattern DANGEROUS_CSS_PATTERNS = Pattern.compile(
            "(?i)(expression\\s*\\(|javascript\\s*:|behavior\\s*:|vbscript\\s*:|url\\s*\\(\\s*['\"]?\\s*(javascript|data|http|https|file):)"
    );

    /** Pattern kiểm tra các protocol URL nguy hiểm trong href/src */
    private static final Pattern DANGEROUS_URI_PROTOCOLS = Pattern.compile(
            "(?i)^\\s*(javascript|data|vbscript|file|http|https|ftp|php|expect|blob):"
    );

    public SvgSecurityV2ServiceImpl(
            SvgSecurityProperties properties,
            FileSecurityScanner fileSecurityScanner
    ) {
        this.properties = properties;
        this.fileSecurityScanner = fileSecurityScanner;
    }

    @Override
    public byte[] validateAndSanitize(byte[] rawSvgBytes, String filename, String mimeType)
            throws SvgSecurityException, SvgPayloadTooLargeException {

        // 1. Kiểm tra rỗng
        if (rawSvgBytes == null || rawSvgBytes.length == 0) {
            throw new SvgSecurityException("Nội dung file rỗng", ErrorCodes.SVG_SECURITY_VALIDATION_FAILED);
        }

        // 2. Kiểm tra giới hạn dung lượng file
        long maxSizeBytes = properties.getMaxFileSize().toBytes();
        if (rawSvgBytes.length > maxSizeBytes) {
            log.warn("[SECURITY_REJECTED] File '{}' exceeds max allowed size: {} bytes > {} bytes",
                    filename, rawSvgBytes.length, maxSizeBytes);
            throw new SvgPayloadTooLargeException(
                    "Dung lượng file (" + rawSvgBytes.length + " bytes) vượt quá giới hạn cho phép (" + properties.getMaxFileSize() + ")",
                    ErrorCodes.SVG_SIZE_LIMIT_EXCEEDED
            );
        }

        // 3. Không trust file extension
        if (StringUtils.hasText(filename)) {
            String lower = filename.trim().toLowerCase(Locale.ROOT);
            if (!lower.endsWith(".svg")) {
                log.warn("[SECURITY_REJECTED] Invalid extension '{}' for file '{}'", lower, filename);
                throw new SvgSecurityException("Định dạng file không được hỗ trợ: phần mở rộng bắt buộc phải là .svg", ErrorCodes.UNSUPPORTED_FORMAT);
            }
        }

        // 4. Kiểm tra MIME type nếu client truyền lên
        if (StringUtils.hasText(mimeType)) {
            String cleanMime = mimeType.split(";")[0].trim().toLowerCase(Locale.ROOT);
            if (!isAllowedMimeType(cleanMime)) {
                log.warn("[SECURITY_REJECTED] Disallowed MIME type '{}' for file '{}'", mimeType, filename);
                throw new SvgSecurityException("MIME type không hợp lệ (" + mimeType + "). Chỉ chấp nhận file SVG.", ErrorCodes.UNSUPPORTED_FORMAT);
            }
        }

        // 5. Quét malware / magic bytes nhị phân
        fileSecurityScanner.scan(rawSvgBytes, filename);

        // 6. Chống XXE: Kiểm tra DOCTYPE / Entity definitions thô
        String rawContent = new String(rawSvgBytes, StandardCharsets.UTF_8);
        if (XXE_PATTERNS.matcher(rawContent).find()) {
            log.warn("[XXE_BLOCKED] Prohibited DOCTYPE/Entity/Stylesheet instruction detected in '{}'", filename);
            throw new SvgSecurityException("Phát hiện cấu trúc DOCTYPE hoặc Entity bị cấm trong SVG (XXE Protection)", ErrorCodes.SVG_SECURITY_VALIDATION_FAILED);
        }

        // 7. Strict XML parsing: Chống cú pháp XML sai, unclosed tags, và tăng cường chống XXE
        validateStrictXmlStructure(rawContent, filename);

        // 8. Parse XML AST bằng Jsoup XML parser
        Document doc;
        try {
            doc = Jsoup.parse(rawContent, "", Parser.xmlParser());
            doc.outputSettings()
                    .prettyPrint(false)
                    .syntax(Document.OutputSettings.Syntax.xml)
                    .charset(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("[MALFORMED_XML] Could not parse XML for '{}': {}", filename, e.getMessage());
            throw new SvgSecurityException("File XML/SVG không hợp lệ hoặc bị lỗi cú pháp", ErrorCodes.SVG_SECURITY_VALIDATION_FAILED);
        }

        // 9. Kiểm tra thẻ gốc <svg>
        Element root = doc.selectFirst("svg");
        if (root == null) {
            log.warn("[STRUCTURE_REJECTED] Missing root <svg> element in '{}'", filename);
            throw new SvgSecurityException("Tệp tải lên không chứa thẻ gốc <svg> hợp lệ", ErrorCodes.SVG_SECURITY_VALIDATION_FAILED);
        }

        // 10. Giới hạn số lượng phần tử và độ sâu XML (XML bomb protection)
        int elementCount = root.getAllElements().size();
        if (elementCount > properties.getMaxElements()) {
            log.warn("[XML_BOMB_BLOCKED] Elements count {} exceeds maximum {}", elementCount, properties.getMaxElements());
            throw new SvgPayloadTooLargeException("Số lượng phần tử XML vượt quá giới hạn an toàn (" + properties.getMaxElements() + ")", ErrorCodes.SVG_SIZE_LIMIT_EXCEEDED);
        }

        int maxDepth = calculateMaxDepth(root, 1);
        if (maxDepth > properties.getMaxXmlDepth()) {
            log.warn("[XML_DEPTH_BLOCKED] XML depth {} exceeds maximum {}", maxDepth, properties.getMaxXmlDepth());
            throw new SvgPayloadTooLargeException("Độ sâu cây XML vượt quá giới hạn an toàn (" + properties.getMaxXmlDepth() + ")", ErrorCodes.SVG_SIZE_LIMIT_EXCEEDED);
        }

        // 11. Duyệt toàn bộ cây phần tử: Áp dụng Allowlist và chặn Active Content / External Resources
        sanitizeTree(root);

        // 12. Canonicalize & chuyển đổi sang UTF-8 bytes
        String sanitizedXml = root.outerHtml();
        if (!sanitizedXml.trim().startsWith("<svg")) {
            throw new SvgSecurityException("Cấu trúc SVG không hợp lệ sau khi làm sạch", ErrorCodes.SVG_SECURITY_VALIDATION_FAILED);
        }

        return sanitizedXml.getBytes(StandardCharsets.UTF_8);
    }

    private void validateStrictXmlStructure(String content, String filename) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            dbf.setNamespaceAware(true);

            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setErrorHandler(new org.xml.sax.helpers.DefaultHandler());
            db.parse(new InputSource(new StringReader(content)));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("maxElementDepth") || msg.contains("entityExpansionLimit") || msg.contains("totalEntitySizeLimit")) {
                log.warn("[XML_LIMIT_EXCEEDED] XML resource limit exceeded in '{}': {}", filename, msg);
                throw new SvgPayloadTooLargeException("Độ sâu hoặc tài nguyên XML vượt quá giới hạn an toàn: " + msg, ErrorCodes.SVG_SIZE_LIMIT_EXCEEDED);
            }
            log.warn("[STRICT_XML_VALIDATION_FAILED] Malformed XML structure in '{}': {}", filename, msg);
            throw new SvgSecurityException("File XML/SVG không hợp lệ hoặc bị lỗi cú pháp: " + msg, ErrorCodes.SVG_SECURITY_VALIDATION_FAILED);
        }
    }

    private boolean isAllowedMimeType(String mime) {
        return "image/svg+xml".equals(mime)
                || "image/svg".equals(mime)
                || "text/xml".equals(mime)
                || "application/xml".equals(mime);
    }

    private int calculateMaxDepth(Element element, int currentDepth) {
        int max = currentDepth;
        for (Element child : element.children()) {
            int childDepth = calculateMaxDepth(child, currentDepth + 1);
            if (childDepth > max) {
                max = childDepth;
            }
        }
        return max;
    }

    private void sanitizeTree(Element element) {
        String tag = element.tagName().toLowerCase(Locale.ROOT);

        // 1. Chặn thẻ <image> hoặc các resource bên ngoài (chống SSRF)
        if ("image".equals(tag)) {
            for (Attribute attr : element.attributes()) {
                String key = attr.getKey().toLowerCase(Locale.ROOT);
                if (key.endsWith("href") || "src".equals(key)) {
                    String val = attr.getValue();
                    if (val != null && DANGEROUS_URI_PROTOCOLS.matcher(val.trim()).find()) {
                        log.warn("[SSRF_BLOCKED] External resource in <image>: {}", val);
                        throw new SvgSecurityException("Phát hiện liên kết tài nguyên bên ngoài nguy hiểm trong thẻ <image>: " + val, ErrorCodes.SVG_SECURITY_VALIDATION_FAILED);
                    }
                }
            }
            throw new SvgSecurityException("Thẻ <image> không được phép trong SVG máy cắt (chống SSRF / External Resource)", ErrorCodes.SVG_SECURITY_VALIDATION_FAILED);
        }

        // 2. Chặn tuyệt đối các phần tử active/dangerous
        if (PROHIBITED_ELEMENTS.contains(tag)) {
            log.warn("[ACTIVE_CONTENT_BLOCKED] Dangerous SVG element detected: <{}>", tag);
            throw new SvgSecurityException("Phát hiện phần tử có nguy cơ thực thi mã độc: <" + tag + ">", ErrorCodes.SVG_SECURITY_VALIDATION_FAILED);
        }

        // 3. Kiểm tra các thuộc tính của phần tử
        for (Attribute attr : element.attributes()) {
            String attrName = attr.getKey().toLowerCase(Locale.ROOT);
            String attrVal = attr.getValue();

            // Chặn tất cả Event Handlers (onclick, onload, onerror, onmouseover, ...)
            if (attrName.startsWith("on")) {
                log.warn("[XSS_EVENT_BLOCKED] Event handler detected: {}='{}'", attrName, attrVal);
                throw new SvgSecurityException("Phát hiện event handler nguy hiểm: " + attrName, ErrorCodes.SVG_SECURITY_VALIDATION_FAILED);
            }

            // Kiểm tra độ dài thuộc tính
            if (attrVal != null && attrVal.length() > properties.getMaxAttributeLength()) {
                throw new SvgSecurityException("Thuộc tính " + attrName + " có độ dài vượt quá giới hạn an toàn", ErrorCodes.SVG_SECURITY_VALIDATION_FAILED);
            }

            // Chặn External Resources / Dangerous URI Protocols (javascript:, data:, http:, https:, file:)
            if (attrName.endsWith("href") || "src".equals(attrName)) {
                if (attrVal != null) {
                    String trimmed = attrVal.trim();
                    if (DANGEROUS_URI_PROTOCOLS.matcher(trimmed).find()) {
                        log.warn("[SSRF_EXTERNAL_RESOURCE_BLOCKED] Unsafe external URI detected in {}='{}'", attrName, trimmed);
                        throw new SvgSecurityException("Liên kết URI bên ngoài hoặc giao thức không an toàn bị cấm: " + trimmed, ErrorCodes.SVG_SECURITY_VALIDATION_FAILED);
                    }
                    // Chỉ cho phép tham chiếu fragment nội bộ (#...) hoặc ID nội bộ
                    if (!trimmed.startsWith("#") && trimmed.contains(":")) {
                        log.warn("[EXTERNAL_RESOURCE_BLOCKED] External resource reference blocked: {}", trimmed);
                        throw new SvgSecurityException("Chỉ cho phép tham chiếu fragment nội bộ trong cùng file SVG", ErrorCodes.SVG_SECURITY_VALIDATION_FAILED);
                    }
                }
            }

            // Kiểm tra inline CSS trong style attribute
            if ("style".equals(attrName) && attrVal != null) {
                if (DANGEROUS_CSS_PATTERNS.matcher(attrVal).find()) {
                    log.warn("[MALICIOUS_CSS_BLOCKED] Malicious CSS expression detected in style='{}'", attrVal);
                    throw new SvgSecurityException("Phát hiện mã CSS hoặc liên kết không an toàn trong thuộc tính style", ErrorCodes.SVG_SECURITY_VALIDATION_FAILED);
                }
            }
        }

        // 4. Chỉ cho phép các phần tử trong Allowlist
        if (!ALLOWED_SVG_ELEMENTS.contains(tag)) {
            log.info("[UNAPPROVED_ELEMENT_REMOVED] Removing non-whitelisted SVG tag: <{}>", tag);
            element.remove();
            return;
        }

        // 5. Giới hạn số lượng thuộc tính trên một phần tử
        if (element.attributes().size() > properties.getMaxAttributesPerElement()) {
            throw new SvgSecurityException("Số lượng thuộc tính trên thẻ <" + tag + "> vượt quá giới hạn", ErrorCodes.SVG_SECURITY_VALIDATION_FAILED);
        }

        // 6. Kiểm tra nội dung bên trong thẻ <style>
        if ("style".equals(tag)) {
            String styleText = element.data();
            if (styleText != null && DANGEROUS_CSS_PATTERNS.matcher(styleText).find()) {
                log.warn("[MALICIOUS_CSS_BLOCKED] Malicious CSS expression detected in <style> block");
                throw new SvgSecurityException("Phát hiện mã CSS hoặc liên kết bên ngoài trong thẻ <style>", ErrorCodes.SVG_SECURITY_VALIDATION_FAILED);
            }
        }

        // 7. Đệ quy xử lý các phần tử con
        for (Element child : new java.util.ArrayList<>(element.children())) {
            sanitizeTree(child);
        }
    }
}
