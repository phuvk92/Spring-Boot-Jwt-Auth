package com.example.svgmanager.svg;

import com.example.svgmanager.svg.SvgImport.ImportedPart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tách part từ SVG — SA v2 §4, port của SvgImport.cs.
 * Ca chốt: audi-q6-2024.svg so với kết quả client (fixtures/…client-parts.txt,
 * sinh bằng SvgImport.Read + ToParts trên chính file đó — sai số ≤ 0,05 mm).
 */
class SvgImportTest {

    private static String fixture(String name) throws IOException {
        try (InputStream in = SvgImportTest.class.getResourceAsStream("/fixtures/" + name)) {
            assertNotNull(in, "Thiếu fixture " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Path + rect + polygon → part, tên từ id (giải mã _x0020_), hệ toạ độ riêng")
    void basicShapes_toParts() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100mm" height="50mm" viewBox="0 0 100 50">
                  <rect id="Nap_x0020_capo" x="10" y="5" width="30" height="20"/>
                  <polygon points="60,10 90,10 90,40"/>
                  <path d="M0 0 L10 0 L10 10 Z" transform="translate(5 40)"/>
                </svg>""";
        List<ImportedPart> parts = SvgImport.parseParts(svg);
        assertEquals(3, parts.size());
        assertEquals("Nap capo", parts.get(0).name());
        assertEquals(30, parts.get(0).widthMm(), 0.001);
        assertEquals(20, parts.get(0).heightMm(), 0.001);
        assertEquals(10, parts.get(0).xMm(), 0.001);
        assertEquals(5, parts.get(0).yMm(), 0.001);
        // Part thứ ba không có id → "Part 3"
        assertEquals("Part 3", parts.get(2).name());
        assertEquals(5, parts.get(2).xMm(), 0.001);
        assertEquals(40, parts.get(2).yMm(), 0.001);
    }

    @Test
    @DisplayName("Đơn vị inch + viewBox phóng đại (kiểu CorelDRAW) quy đúng ra mm")
    void inchUnits() {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="10in" viewBox="0 0 1000 1000">
                  <rect x="0" y="0" width="1000" height="1000"/>
                </svg>""";
        List<ImportedPart> parts = SvgImport.parseParts(svg);
        assertEquals(1, parts.size());
        assertEquals(254, parts.get(0).widthMm(), 0.01);
    }

    @Test
    @DisplayName("File không khai đơn vị → lỗi SVG_UNITS_MISSING (server không hỏi lại được thợ)")
    void missingUnits_rejected() {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <rect x="0" y="0" width="10" height="10"/>
                </svg>""";
        SvgImportException ex = assertThrows(SvgImportException.class, () -> SvgImport.parseParts(svg));
        assertEquals(SvgImportException.UNITS_MISSING, ex.getKind());
    }

    @Test
    @DisplayName("width=\"100%\" không quy ra mm → SVG_UNITS_MISSING, không đoán px")
    void percentWidth_rejected() {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100%" height="100%" viewBox="0 0 10 10">
                  <rect x="0" y="0" width="10" height="10"/>
                </svg>""";
        SvgImportException ex = assertThrows(SvgImportException.class, () -> SvgImport.parseParts(svg));
        assertEquals(SvgImportException.UNITS_MISSING, ex.getKind());
    }

    @Test
    @DisplayName("Nhánh ẩn (display:none) không sinh part")
    void hiddenSkipped() {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100mm" height="50mm" viewBox="0 0 100 50">
                  <g display="none"><rect x="0" y="0" width="10" height="10"/></g>
                  <rect x="0" y="0" width="20" height="10"/>
                </svg>""";
        List<ImportedPart> parts = SvgImport.parseParts(svg);
        assertEquals(1, parts.size());
    }

    @Test
    @DisplayName("Path nhiều figure lồng nhau → một part, hole_count đếm lỗ khoét")
    void holesCounted() {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100mm" height="100mm" viewBox="0 0 100 100">
                  <path d="M0 0 L80 0 L80 80 L0 80 Z M10 10 L30 10 L30 30 L10 30 Z"/>
                </svg>""";
        List<ImportedPart> parts = SvgImport.parseParts(svg);
        assertEquals(1, parts.size());
        assertEquals(1, parts.get(0).holeCount());
    }

    @Test
    @DisplayName("CA CHỐT: Audi Q6 2024.svg — số part và kích thước khớp client (≤ 0,05 mm)")
    void audiQ6_matchesClient() throws Exception {
        String svg = fixture("audi-q6-2024.svg");
        List<ImportedPart> parts = SvgImport.parseParts(svg);

        List<String> golden = fixture("audi-q6-2024.client-parts.txt").lines()
                .filter(l -> !l.isBlank()).toList();
        assertEquals(golden.size(), parts.size(), "Số part lệch với client");

        for (int i = 0; i < golden.size(); i++) {
            String[] c = golden.get(i).split("\\|");
            ImportedPart p = parts.get(i);
            assertEquals(Double.parseDouble(c[1]), p.widthMm(), 0.05, "width part " + (i + 1));
            assertEquals(Double.parseDouble(c[2]), p.heightMm(), 0.05, "height part " + (i + 1));
            assertEquals(Double.parseDouble(c[3]), p.xMm(), 0.05, "x part " + (i + 1));
            assertEquals(Double.parseDouble(c[4]), p.yMm(), 0.05, "y part " + (i + 1));
            assertEquals(Integer.parseInt(c[5]), p.nodeCount(), "nodeCount part " + (i + 1));
            assertEquals(Integer.parseInt(c[6]), p.holeCount(), "holeCount part " + (i + 1));
        }
    }
}
