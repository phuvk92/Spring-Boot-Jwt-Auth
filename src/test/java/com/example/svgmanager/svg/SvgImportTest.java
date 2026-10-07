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
 * Ca chốt: audi-q6-2024.svg so với kết quả client (fixtures/…client-parts.txt — 139 part theo ring chẵn/lẻ,
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

        // 4 màu miếng phim của Audi Q6 — hình trắng #FEFEFE đều là lỗ khoét, đã gộp vào part
        // chứa nó (SA-Nesting §8), nên không còn part nào mang màu trắng
        java.util.Set<String> colors = parts.stream()
                .map(ImportedPart::color)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(java.util.Set.of("#5CC6D0", "#F7ADAF", "#718FC8", "#F58634"), colors);
    }

    @Test
    @DisplayName("NGO-415: Màu tô chuẩn hoá #RRGGBB, kế thừa từ <g>, ưu tiên inline style hơn fill attribute")
    void partColors_resolutionAndInheritance() {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100mm" height="100mm" viewBox="0 0 100 100">
                  <g fill="#abc">
                    <rect id="p1" x="0" y="0" width="10" height="10"/>
                    <rect id="p2" x="10" y="0" width="10" height="10" fill="#f58634"/>
                    <rect id="p3" x="20" y="0" width="10" height="10" fill="#f58634" style="fill: #5cc6d0; stroke: #333"/>
                    <rect id="p4" x="30" y="0" width="10" height="10" fill="none"/>
                    <rect id="p5" x="40" y="0" width="10" height="10" style="fill: rgb(247, 173, 175)"/>
                    <rect id="p6" x="50" y="0" width="10" height="10" fill="transparent"/>
                    <rect id="p7" x="60" y="0" width="10" height="10" fill="url(#grad1)"/>
                  </g>
                  <rect id="p8" x="70" y="0" width="10" height="10"/>
                </svg>""";
        List<ImportedPart> parts = SvgImport.parseParts(svg);
        assertEquals(8, parts.size());
        assertEquals("#AABBCC", parts.get(0).color(), "Kế thừa #abc -> #AABBCC");
        assertEquals("#F58634", parts.get(1).color(), "Ghi đè fill=\"#f58634\"");
        assertEquals("#5CC6D0", parts.get(2).color(), "style=\"fill: #5cc6d0\" ưu tiên hơn fill attribute");
        assertNull(parts.get(3).color(), "fill=\"none\" -> null");
        assertEquals("#F7ADAF", parts.get(4).color(), "style=\"fill: rgb(247, 173, 175)\" -> #F7ADAF");
        assertNull(parts.get(5).color(), "transparent -> null");
        assertNull(parts.get(6).color(), "url(#grad1) -> null");
        assertNull(parts.get(7).color(), "Không khai báo màu -> null");
    }

    @Test
    @DisplayName("Màu từ lớp CSS trong <style> — đúng khuôn file CorelDraw (bản đã xếp vios 2025 trên prod)")
    void partColors_fromCssClasses_coreldraw() {
        // Rút gọn từ file thật: CDATA, lớp stroke (.str0) không phải màu tô, path mang hai lớp "fil0 str0".
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100mm" height="100mm" viewBox="0 0 100 100">
                 <defs>
                  <style type="text/css">
                   <![CDATA[
                    .str0 {stroke:#373435;stroke-width:7.87;stroke-miterlimit:22.9256}
                    .fil2 {fill:#FEFEFE}
                    .fil0 {fill:#F7ADAF}
                    /* chú thích */ .fil3, .fil9 {fill:#5CC6D0}
                    .fil1 {fill:#718FC8}
                    .khong {fill:none}
                   ]]>
                  </style>
                 </defs>
                 <g id="Layer_x0020_1">
                  <path id="p1" class="fil0 str0" d="M0 0 H10 V10 H0 Z"/>
                  <path id="p2" class="str0 fil3" d="M10 0 H20 V10 H10 Z"/>
                  <path id="p3" class="fil9" d="M20 0 H30 V10 H20 Z"/>
                  <path id="p4" class="fil0 fil1" d="M30 0 H40 V10 H30 Z"/>
                  <path id="p5" class="fil0" fill="#000000" d="M40 0 H50 V10 H40 Z"/>
                  <path id="p6" class="fil0" style="fill:#F58634" d="M50 0 H60 V10 H50 Z"/>
                  <path id="p7" class="str0" d="M60 0 H70 V10 H60 Z"/>
                  <path id="p8" class="khong" fill="#123456" d="M70 0 H80 V10 H70 Z"/>
                 </g>
                </svg>""";
        List<ImportedPart> parts = SvgImport.parseParts(svg);
        assertEquals(8, parts.size());
        assertEquals("#F7ADAF", parts.get(0).color(), "class=\"fil0 str0\" — lấy fill của .fil0, bỏ .str0");
        assertEquals("#5CC6D0", parts.get(1).color(), "thứ tự lớp trên phần tử không quan trọng");
        assertEquals("#5CC6D0", parts.get(2).color(), "bộ chọn danh sách .fil3, .fil9");
        assertEquals("#718FC8", parts.get(3).color(), "hai lớp cùng đặt fill: quy tắc đứng sau (.fil1) thắng");
        assertEquals("#F7ADAF", parts.get(4).color(), "lớp CSS thắng thuộc tính fill");
        assertEquals("#F58634", parts.get(5).color(), "style trên phần tử thắng lớp CSS");
        assertNull(parts.get(6).color(), "chỉ có lớp stroke — không màu tô");
        assertNull(parts.get(7).color(), "lớp đặt fill:none — không màu, không rơi về thuộc tính");
    }

    @Test
    @DisplayName("NGO-415: normalizeColor hỗ trợ #RGB, #RRGGBB, rgb() và bỏ qua giá trị đặc biệt/hỏng")
    void normalizeColor_cases() {
        assertEquals("#123456", SvgImport.normalizeColor("#123456"));
        assertEquals("#AABBCC", SvgImport.normalizeColor("#abc"));
        assertEquals("#00FF80", SvgImport.normalizeColor("rgb(0, 255, 128)"));
        assertEquals("#000000", SvgImport.normalizeColor("rgb(0,0,0)"));
        assertNull(SvgImport.normalizeColor("none"));
        assertNull(SvgImport.normalizeColor("NONE"));
        assertNull(SvgImport.normalizeColor("transparent"));
        assertNull(SvgImport.normalizeColor("currentColor"));
        assertNull(SvgImport.normalizeColor("url(#someId)"));
        assertNull(SvgImport.normalizeColor("invalid"));
        assertNull(SvgImport.normalizeColor(""));
        assertNull(SvgImport.normalizeColor(null));
        assertNull(SvgImport.normalizeColor("rgb(300, 0, 0)"));
    }
}
