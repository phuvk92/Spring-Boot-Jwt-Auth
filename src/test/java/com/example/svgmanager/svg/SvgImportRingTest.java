package com.example.svgmanager.svg;

import com.example.svgmanager.service.impl.SvgSanitizerServiceImpl;
import com.example.svgmanager.svg.SvgImport.ImportedPart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gom part theo ring kín chẵn/lẻ — SA-Nesting §8.4 (board 08/10).
 * CorelDRAW xuất mỗi đường cắt thành một &lt;path&gt; riêng, nên lỗ khoét từng bị tách thành
 * part rời và SuperNesting dời chúng ra khỏi part chứa.
 */
class SvgImportRingTest {

    /** Đọc fixture qua bộ khử độc như luồng upload thật — file A3 là UTF-16LE (CorelDRAW 2021). */
    private static List<ImportedPart> partsOf(String name) throws IOException {
        try (InputStream in = SvgImportRingTest.class.getResourceAsStream("/fixtures/" + name)) {
            assertNotNull(in, "Thiếu fixture " + name);
            byte[] clean = new SvgSanitizerServiceImpl().sanitizeAndValidateSvg(in.readAllBytes());
            return SvgImport.parseParts(new String(clean, StandardCharsets.UTF_8));
        }
    }

    private static String mm(String body) {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1000mm\" height=\"1000mm\" viewBox=\"0 0 1000 1000\">"
                + body + "</svg>";
    }

    @Test
    @DisplayName("A3 chưa xếp: 38 path → 30 part; bệ cần số có 3 lỗ; đĩa trong vòng lỗ là part riêng")
    void audiA3Raw() throws Exception {
        List<ImportedPart> parts = partsOf("audi-a3-raw.svg");
        assertEquals(30, parts.size());
        assertEquals(1, parts.stream().filter(p -> p.holeCount() == 3).count(), "đúng một part có 3 lỗ");
        ImportedPart console = parts.stream().filter(p -> p.holeCount() == 3).findFirst().orElseThrow();
        assertEquals(183, console.widthMm(), 1);
        assertEquals(224, console.heightMm(), 1);
        // Đĩa 23×23 mm nằm trong vòng lỗ #7 — miếng phim rời, không bị nuốt vào bệ cần số
        assertTrue(parts.stream().anyMatch(p -> Math.abs(p.widthMm() - 23) < 1
                && Math.abs(p.heightMm() - 23) < 1 && p.holeCount() == 0));
        assertEquals(8, parts.stream().mapToInt(ImportedPart::holeCount).sum(), "8 lỗ khoét");
    }

    @Test
    @DisplayName("A3 đã xếp: cũng 30 part — part admin đặt trong lỗ khung lớn vẫn là part rời")
    void audiA3NestedMatchesRaw() throws Exception {
        List<ImportedPart> nested = partsOf("audi-a3-nested.svg");
        assertEquals(30, nested.size(), "khớp bản chưa xếp — không vướng LAYOUT_PART_MISMATCH");
        assertEquals(8, nested.stream().mapToInt(ImportedPart::holeCount).sum());
    }

    @Test
    @DisplayName("Audi Q6: 177 hình → 139 part")
    void audiQ6() throws Exception {
        assertEquals(139, partsOf("audi-q6-2024-req.svg").size());
    }

    @Test
    @DisplayName("Lỗ tách path riêng = lỗ trong cùng path (Combine) — cùng một part có 1 lỗ")
    void separatePathsEqualCombined() {
        List<ImportedPart> separate = SvgImport.parseParts(mm(
                "<path d=\"M0 0 H100 V100 H0 Z\"/><path fill=\"#FEFEFE\" d=\"M20 20 H40 V40 H20 Z\"/>"));
        List<ImportedPart> combined = SvgImport.parseParts(mm(
                "<path d=\"M0 0 H100 V100 H0 Z M20 20 H40 V40 H20 Z\"/>"));
        assertEquals(1, separate.size());
        assertEquals(1, combined.size());
        assertEquals(1, separate.get(0).holeCount());
        assertEquals(1, combined.get(0).holeCount());
    }

    @Test
    @DisplayName("Đảo trong lỗ (độ sâu 2) là part riêng; lỗ của đảo (độ sâu 3) thuộc đảo")
    void islandInsideHole() {
        List<ImportedPart> parts = SvgImport.parseParts(mm(
                "<path d=\"M0 0 H300 V300 H0 Z\"/>"           // 0: miếng phim
                + "<path d=\"M50 50 H250 V250 H50 Z\"/>"       // 1: lỗ
                + "<path d=\"M100 100 H200 V200 H100 Z\"/>"    // 2: đảo — part riêng
                + "<path d=\"M140 140 H160 V160 H140 Z\"/>")); // 3: lỗ của đảo
        assertEquals(2, parts.size());
        assertEquals(1, parts.get(0).holeCount());
        assertEquals(1, parts.get(1).holeCount());
        assertEquals(100, parts.get(1).widthMm(), 0.001);
    }

    @Test
    @DisplayName("Hai hình cắt nhau (không bên nào chứa trọn) → hai part")
    void crossingShapesStaySeparate() {
        List<ImportedPart> parts = SvgImport.parseParts(mm(
                "<path d=\"M0 0 H100 V100 H0 Z\"/><path d=\"M50 50 H150 V150 H50 Z\"/>"));
        assertEquals(2, parts.size());
        assertEquals(0, parts.get(0).holeCount() + parts.get(1).holeCount());
    }

    @Test
    @DisplayName("Hai part đứng cạnh nhau chung mép → không part nào thành lỗ của part kia")
    void touchingNeighboursStaySeparate() {
        List<ImportedPart> parts = SvgImport.parseParts(mm(
                "<path d=\"M0 0 H100 V100 H0 Z\"/><path d=\"M100 0 H200 V100 H100 Z\"/>"));
        assertEquals(2, parts.size());
    }
}
