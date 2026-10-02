package com.example.svgmanager.svg;

import com.example.svgmanager.svg.PathShape.Fig;
import com.example.svgmanager.svg.PathShape.Seg;
import com.example.svgmanager.svg.SvgGeom.Affine;
import com.example.svgmanager.svg.SvgGeom.Box;
import com.example.svgmanager.svg.SvgGeom.Pt;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tách part khi upload SVG — SA-DanhMucXe-v2 §4. Port của
 * {@code Pcut.Client.Domain/Design/SvgImport.cs} sang Java, bỏ phần đếm "đối
 * tượng bỏ qua" (server không hiện bảng DS-103) nhưng giữ nguyên quy tắc hình
 * học để part server và part client tự nạp ra giống nhau:
 *
 * - Quy đơn vị ra mm từ width/height + viewBox. File không khai đơn vị được thì
 *   ném {@link SvgImportException} (server không hỏi lại được thợ → 400).
 * - Mỗi hình kín (kèm lỗ trong cùng path) = một part; pathData trong hệ toạ độ
 *   riêng, gốc ở góc trên-trái hộp bao; vị trí vào xMm/yMm.
 * - Tên part lấy từ thuộc tính {@code id} (giải mã {@code _xHHHH_} như
 *   {@code _x0020_} → khoảng trắng); thiếu id thì {@code Part {n}}.
 */
public final class SvgImport {

    private SvgImport() {
    }

    /** Phần tử là hình học. So sánh theo local name — namespace của file không quan trọng. */
    private static final Set<String> GEOMETRY_TAGS =
            Set.of("path", "rect", "circle", "ellipse", "line", "polyline", "polygon");

    /** Thẻ chứa hình bên trong — đi xuyên qua, mang theo ma trận. */
    private static final Set<String> CONTAINER_TAGS = Set.of("g", "svg", "a", "switch");

    /** Thẻ không bao giờ là hình vẽ — bỏ im lặng. */
    private static final Set<String> IGNORED_TAGS =
            Set.of("defs", "style", "title", "desc", "metadata", "script", "animate", "set");

    /** Thẻ có thể mang hình nhưng chưa đọc được — bỏ, không vào part. */
    private static final Set<String> UNSUPPORTED_TAGS =
            Set.of("use", "clipPath", "mask", "marker", "symbol", "pattern", "filter", "foreignObject");

    /** {@code _xHHHH_} — cách trình vẽ mã hoá ký tự đặc biệt trong id (điển hình _x0020_ = space). */
    private static final Pattern XML_ESCAPED_CHAR = Pattern.compile("_x([0-9A-Fa-f]{4})_");

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})$");
    private static final Pattern RGB_FUNC_PATTERN = Pattern.compile("^rgb\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)$", Pattern.CASE_INSENSITIVE);

    /** Một part tách được từ file — đủ cột hình học của svg_file_parts (V13 + V21). */
    public record ImportedPart(String name, String pathData, double widthMm, double heightMm,
                               double xMm, double yMm, int nodeCount, int holeCount, String color) {

        /** Constructor tương thích cho các mã gọi cũ không truyền color. */
        public ImportedPart(String name, String pathData, double widthMm, double heightMm,
                            double xMm, double yMm, int nodeCount, int holeCount) {
            this(name, pathData, widthMm, heightMm, xMm, yMm, nodeCount, holeCount, null);
        }
    }

    /**
     * Đọc nội dung SVG, tách thành danh sách part theo thứ tự trong file.
     * Ném {@link SvgImportException} khi file hỏng, gốc không phải svg, hoặc
     * không quy ra mm được.
     */
    public static List<ImportedPart> parseParts(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new SvgImportException("File rỗng — không có nội dung SVG nào.", SvgImportException.FORMAT);
        }

        Document doc;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Không nạp DTD ngoài: vừa chống XXE, vừa cho file CorelDRAW (có DOCTYPE)
            // đi qua mà không tải DTD thật từ w3.org.
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new SvgImportException("Cú pháp XML sai — " + e.getMessage(), SvgImportException.FORMAT);
        }

        Element root = doc.getDocumentElement();
        if (root == null) {
            throw new SvgImportException("File không có phần tử gốc nào.", SvgImportException.FORMAT);
        }
        if (!"svg".equals(localName(root))) {
            throw new SvgImportException("Phần tử gốc là <" + localName(root) + ">, không phải <svg> — "
                    + "đây không phải file SVG.", SvgImportException.FORMAT);
        }

        Units units = resolveUnits(root);
        if (units.mmPerUnit == null) {
            // Client có hộp thoại hỏi thợ; server không hỏi được nên từ chối — đoán sai
            // đơn vị là sai tỉ lệ toàn bộ bản vẽ (DS-108).
            throw new SvgImportException(
                    "File không khai đơn vị (cần width/height kèm đơn vị quy ra mm và viewBox) — "
                            + "mở trong phần mềm thiết kế, lưu lại với đơn vị rõ ràng rồi nạp lại.",
                    SvgImportException.UNITS_MISSING);
        }

        // Gốc viewBox dời về 0 trước khi đổi đơn vị — như client.
        List<Pending> collected = new ArrayList<>();
        String rootFill = resolveElementFill(root, null);
        walk(root, Affine.translate(-units.origin.x(), -units.origin.y()), rootFill, collected);

        Affine toMm = Affine.scale(units.mmPerUnit, units.mmPerUnit);
        List<ImportedPart> parts = new ArrayList<>();

        for (Pending p : collected) {
            PathShape mm = p.shape.transform(toMm);
            Box box = mm.bounds();

            // Hình thu về một điểm thì không có gì để cắt — đường nằm ngang (H=0, W>0) vẫn vào.
            if (box.width() <= 0 && box.height() <= 0) {
                continue;
            }

            PathShape local = mm.transform(Affine.translate(-box.x(), -box.y()));
            String name = p.rawId != null ? decodeXmlName(p.rawId) : "Part " + (parts.size() + 1);

            parts.add(new ImportedPart(name, SvgPath.write(local),
                    Math.max(box.width(), 0.001), Math.max(box.height(), 0.001),
                    box.x(), box.y(), local.nodeCount(), local.holeCount(), p.color()));
        }

        return parts;
    }

    // ── Đơn vị — DS-108 ─────────────────────────────────────────────────

    private record Units(Double mmPerUnit, Pt origin) {
    }

    /**
     * Quy đổi một đơn vị người dùng ra mm từ width + viewBox. Trả mmPerUnit=null
     * khi thiếu viewBox, thiếu width, hoặc width mang đơn vị không quy ra mm (100%…).
     */
    private static Units resolveUnits(Element root) {
        List<Double> vb = SvgPath.numbers(attr(root, "viewBox"));
        boolean hasBox = vb.size() >= 4 && vb.get(2) > 0 && vb.get(3) > 0;
        Pt origin = hasBox ? new Pt(vb.get(0), vb.get(1)) : new Pt(0, 0);

        if (!hasBox) {
            return new Units(null, origin);
        }
        Length w = splitLength(attr(root, "width"));
        if (w == null) {
            return new Units(null, origin);
        }
        Double widthMm = toMm(w.value, w.unit);
        if (w.value <= 0 || widthMm == null) {
            return new Units(null, origin);
        }

        double byWidth = widthMm / vb.get(2);

        // preserveAspectRatio mặc định xMidYMid meet: tỉ lệ NHỎ HƠN của hai trục —
        // lấy thẳng tỉ lệ bề rộng là sai khi height và viewBox không cùng tỉ lệ.
        Length h = splitLength(attr(root, "height"));
        if (h != null && h.value > 0) {
            Double heightMm = toMm(h.value, h.unit);
            if (heightMm != null) {
                return new Units(Math.min(byWidth, heightMm / vb.get(3)), origin);
            }
        }
        return new Units(byWidth, origin);
    }

    private record Length(double value, String unit) {
    }

    /** Tách "210mm" thành số và đơn vị. null khi phần số không đọc được. */
    private static Length splitLength(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String s = text.trim();
        int i = 0;
        if (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '+')) {
            i++;
        }
        while (i < s.length() && isDigit(s.charAt(i))) {
            i++;
        }
        if (i < s.length() && s.charAt(i) == '.') {
            i++;
            while (i < s.length() && isDigit(s.charAt(i))) {
                i++;
            }
        }
        try {
            double v = Double.parseDouble(s.substring(0, i));
            return new Length(v, s.substring(i).trim().toLowerCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    /** Quy một chiều dài ra mm; null khi đơn vị không quy được (%, em, vw…). */
    private static Double toMm(double value, String unit) {
        return switch (unit) {
            case "", "px" -> value * 25.4 / 96;
            case "mm" -> value;
            case "cm" -> value * 10.0;
            case "in" -> value * 25.4;
            case "pt" -> value * 25.4 / 72;
            case "pc" -> value * 25.4 / 6;
            // q (1/4 mm) — CSS có, quy ra mm chính xác.
            case "q" -> value * 25.4 / 101.6;
            default -> null;
        };
    }

    // ── Duyệt cây ───────────────────────────────────────────────────────

    /** Shape kèm id và màu của phần tử — id làm tên part (§4), color là màu tô (NGO-415). */
    private record Pending(PathShape shape, String rawId, String color) {
    }

    private static void walk(Element parent, Affine parentMat, String inheritedColor, List<Pending> shapes) {
        NodeList children = parent.getChildNodes();
        for (int k = 0; k < children.getLength(); k++) {
            Node node = children.item(k);
            if (!(node instanceof Element el)) {
                continue;
            }
            String tag = localName(el);

            // Nhánh ẩn cắt tận gốc — display:none của cha che hết con cháu.
            if (isHidden(el)) {
                continue;
            }

            // Con áp trước, cha áp sau.
            Affine mat = parseTransform(attr(el, "transform")).mul(parentMat);

            if (GEOMETRY_TAGS.contains(tag)) {
                PathShape shape = buildShape(el, tag);
                if (!shape.figures().isEmpty()) {
                    String id = attr(el, "id");
                    String color = resolveElementFill(el, inheritedColor);
                    shapes.add(new Pending(shape.transform(mat),
                            id == null || id.isEmpty() ? null : id, color));
                }
                continue;
            }
            if (CONTAINER_TAGS.contains(tag)) {
                String groupColor = resolveElementFill(el, inheritedColor);
                walk(el, mat, groupColor, shapes);
                continue;
            }
            if (IGNORED_TAGS.contains(tag)) {
                continue;
            }
            if (UNSUPPORTED_TAGS.contains(tag)) {
                continue;
            }
            if ("text".equals(tag) || "image".equals(tag)) {
                continue;
            }
            // Thẻ lạ: đi xuyên qua thay vì bỏ — nó thường là bọc quanh hình thật.
            String unknownColor = resolveElementFill(el, inheritedColor);
            walk(el, mat, unknownColor, shapes);
        }
    }

    /** Nhánh này có bị ẩn không — chỉ xét thuộc tính trên chính phần tử. */
    private static boolean isHidden(Element el) {
        String display = attr(el, "display");
        if ("none".equals(display != null ? display.trim() : null)) {
            return true;
        }
        String vis = attr(el, "visibility");
        if ("hidden".equals(vis != null ? vis.trim() : null)) {
            return true;
        }
        String style = attr(el, "style");
        if (style == null) {
            return false;
        }
        String s = style.replace(" ", "").replace("\t", "").toLowerCase(Locale.ROOT);
        return s.contains("display:none") || s.contains("visibility:hidden");
    }

    // ── Hình cơ bản → biên dạng bezier ──────────────────────────────────

    private static PathShape buildShape(Element el, String tag) {
        return switch (tag) {
            case "path" -> {
                String d = attr(el, "d");
                yield d != null && !d.isEmpty() ? SvgPath.parse(d) : PathShape.EMPTY;
            }
            case "rect" -> buildRect(el);
            case "circle" -> buildCircle(el);
            case "ellipse" -> buildEllipse(el);
            case "line" -> buildLine(el);
            case "polyline" -> buildPoly(el, false);
            case "polygon" -> buildPoly(el, true);
            default -> PathShape.EMPTY;
        };
    }

    private static PathShape buildRect(Element el) {
        double w = num(el, "width");
        double h = num(el, "height");
        if (w <= 0 || h <= 0) {
            return PathShape.EMPTY;
        }
        // Chuẩn SVG: ghi một trong hai bán kính thì cái kia lấy theo.
        double rx = num(el, "rx", Double.NaN);
        double ry = num(el, "ry", Double.NaN);
        if (Double.isNaN(rx) && Double.isNaN(ry)) {
            rx = ry = 0;
        } else if (Double.isNaN(rx)) {
            rx = ry;
        } else if (Double.isNaN(ry)) {
            ry = rx;
        }
        return SvgShapes.roundedRectangle(w, h, rx, ry)
                .transform(Affine.translate(num(el, "x"), num(el, "y")));
    }

    private static PathShape buildCircle(Element el) {
        double r = num(el, "r");
        if (r <= 0) {
            return PathShape.EMPTY;
        }
        return SvgShapes.ellipse(r * 2, r * 2)
                .transform(Affine.translate(num(el, "cx") - r, num(el, "cy") - r));
    }

    private static PathShape buildEllipse(Element el) {
        double rx = num(el, "rx");
        double ry = num(el, "ry");
        if (rx <= 0 || ry <= 0) {
            return PathShape.EMPTY;
        }
        return SvgShapes.ellipse(rx * 2, ry * 2)
                .transform(Affine.translate(num(el, "cx") - rx, num(el, "cy") - ry));
    }

    private static PathShape buildLine(Element el) {
        return new PathShape(List.of(new Fig(
                new Pt(num(el, "x1"), num(el, "y1")),
                List.of(Seg.lineTo(new Pt(num(el, "x2"), num(el, "y2")))),
                false)));
    }

    private static PathShape buildPoly(Element el, boolean closed) {
        List<Double> n = SvgPath.numbers(attr(el, "points"));
        if (n.size() < 4) {
            return PathShape.EMPTY;
        }
        Pt start = new Pt(n.get(0), n.get(1));
        List<Seg> segments = new ArrayList<>();
        for (int i = 2; i + 1 < n.size(); i += 2) {
            segments.add(Seg.lineTo(new Pt(n.get(i), n.get(i + 1))));
        }
        // Đoạn khép lại viết HẲN ra — cùng quy ước Z của SvgPath.
        if (closed && !segments.isEmpty()
                && segments.get(segments.size() - 1).end().distanceTo(start) >= SvgPath.MERGE_TOLERANCE) {
            segments.add(Seg.lineTo(start));
        }
        return new PathShape(List.of(new Fig(start, segments, closed)));
    }

    /** Đọc thuộc tính số — lấy phần số, bỏ đuôi đơn vị (như client). */
    private static double num(Element el, String name) {
        return num(el, name, 0);
    }

    private static double num(Element el, String name, double fallback) {
        Length l = splitLength(attr(el, name));
        return l != null ? l.value : fallback;
    }

    // ── transform ───────────────────────────────────────────────────────

    /**
     * Đọc transform: translate · scale · rotate (cả dạng có tâm) · skewX · skewY ·
     * matrix, nhân dồn theo thứ tự SVG — hàm viết sau được áp TRƯỚC.
     */
    public static Affine parseTransform(String text) {
        if (text == null || text.isBlank()) {
            return Affine.IDENTITY;
        }
        Affine result = Affine.IDENTITY;
        int i = 0;
        while (i < text.length()) {
            while (i < text.length() && (isWhitespaceT(text.charAt(i)) || text.charAt(i) == ',')) {
                i++;
            }
            int nameStart = i;
            while (i < text.length() && isLetterT(text.charAt(i))) {
                i++;
            }
            if (i >= text.length() || nameStart == i) {
                break;
            }
            String fn = text.substring(nameStart, i);
            int open = text.indexOf('(', i);
            int close = open < 0 ? -1 : text.indexOf(')', open);
            if (open < 0 || close < 0) {
                break;
            }
            List<Double> a = SvgPath.numbers(text.substring(open + 1, close));
            i = close + 1;
            result = function(fn, a).mul(result);
        }
        return result;
    }

    private static Affine function(String name, List<Double> a) {
        return switch (name) {
            case "matrix" -> a.size() >= 6
                    ? new Affine(a.get(0), a.get(1), a.get(2), a.get(3), a.get(4), a.get(5))
                    : Affine.IDENTITY;
            case "translate" -> a.size() >= 1
                    ? Affine.translate(a.get(0), a.size() > 1 ? a.get(1) : 0)
                    : Affine.IDENTITY;
            // scale(2) là hai trục cùng 2 — thiếu quy tắc này thì trục Y nhân 0.
            case "scale" -> a.size() >= 1
                    ? Affine.scale(a.get(0), a.size() > 1 ? a.get(1) : a.get(0))
                    : Affine.IDENTITY;
            case "rotate" -> a.size() >= 3
                    ? Affine.translate(-a.get(1), -a.get(2)).mul(Affine.rotate(a.get(0)))
                            .mul(Affine.translate(a.get(1), a.get(2)))
                    : a.size() >= 1 ? Affine.rotate(a.get(0)) : Affine.IDENTITY;
            case "skewx", "skewX" -> a.size() >= 1
                    ? new Affine(1, 0, Math.tan(Math.toRadians(a.get(0))), 1, 0, 0)
                    : Affine.IDENTITY;
            case "skewy", "skewY" -> a.size() >= 1
                    ? new Affine(1, Math.tan(Math.toRadians(a.get(0))), 0, 1, 0, 0)
                    : Affine.IDENTITY;
            default -> Affine.IDENTITY;
        };
    }

    // ── tiện ích DOM ────────────────────────────────────────────────────

    private static String localName(Element el) {
        String ln = el.getLocalName();
        return ln != null ? ln : el.getNodeName();
    }

    /** Thuộc tính hoặc null khi không có — DOM trả "" cho thuộc tính thiếu. */
    private static String attr(Element el, String name) {
        return el.hasAttribute(name) ? el.getAttribute(name) : null;
    }

    /** Giải mã id trình vẽ: {@code _x0020_} → space và mọi mã _xHHHH_ khác. */
    static String decodeXmlName(String id) {
        Matcher m = XML_ESCAPED_CHAR.matcher(id);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(
                    String.valueOf((char) Integer.parseInt(m.group(1), 16))));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isWhitespaceT(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    private static boolean isLetterT(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    // ── Xử lý màu tô — NGO-415 ──────────────────────────────────────────

    /**
     * Xác định màu tô hiệu lực của phần tử:
     * - inline style (fill: ...) có độ ưu tiên cao nhất
     * - thuộc tính fill="..."
     * - kế thừa từ cha nếu phần tử không khai báo fill
     * - chuẩn hoá #RRGGBB viết hoa, null nếu không có màu hoặc không hợp lệ.
     */
    private static String resolveElementFill(Element el, String inheritedColor) {
        String styleFill = parseInlineStyleFill(attr(el, "style"));
        if (styleFill != null) {
            return normalizeColor(styleFill);
        }

        String fillAttr = attr(el, "fill");
        if (fillAttr != null) {
            return normalizeColor(fillAttr);
        }

        return inheritedColor;
    }

    /** Trích giá trị fill trong thuộc tính style="..." */
    private static String parseInlineStyleFill(String style) {
        if (style == null || style.isBlank()) {
            return null;
        }
        String[] declarations = style.split(";");
        for (String decl : declarations) {
            int colon = decl.indexOf(':');
            if (colon > 0) {
                String prop = decl.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                if ("fill".equals(prop)) {
                    return decl.substring(colon + 1).trim();
                }
            }
        }
        return null;
    }

    /**
     * Chuẩn hoá chuỗi màu sang #RRGGBB (viết hoa):
     * - Hỗ trợ #RGB (#abc -> #AABBCC)
     * - Hỗ trợ #RRGGBB
     * - Hỗ trợ rgb(r, g, b)
     * - Các giá trị none, transparent, currentColor, url(...) hoặc không hợp lệ -> null
     */
    static String normalizeColor(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        String lower = s.toLowerCase(Locale.ROOT);
        if ("none".equals(lower) || "transparent".equals(lower) || "currentcolor".equals(lower)
                || lower.startsWith("url(")) {
            return null;
        }

        Matcher hexMatcher = HEX_COLOR_PATTERN.matcher(s);
        if (hexMatcher.matches()) {
            String hex = hexMatcher.group(1);
            if (hex.length() == 3) {
                char r = hex.charAt(0);
                char g = hex.charAt(1);
                char b = hex.charAt(2);
                return ("#" + r + r + g + g + b + b).toUpperCase(Locale.ROOT);
            }
            return ("#" + hex).toUpperCase(Locale.ROOT);
        }

        Matcher rgbMatcher = RGB_FUNC_PATTERN.matcher(s);
        if (rgbMatcher.matches()) {
            try {
                int r = Integer.parseInt(rgbMatcher.group(1));
                int g = Integer.parseInt(rgbMatcher.group(2));
                int b = Integer.parseInt(rgbMatcher.group(3));
                if (r >= 0 && r <= 255 && g >= 0 && g <= 255 && b >= 0 && b <= 255) {
                    return String.format(Locale.ROOT, "#%02X%02X%02X", r, g, b);
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }
}
