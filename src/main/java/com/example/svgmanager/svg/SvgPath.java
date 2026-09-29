package com.example.svgmanager.svg;

import com.example.svgmanager.svg.PathShape.Fig;
import com.example.svgmanager.svg.PathShape.Seg;
import com.example.svgmanager.svg.SvgGeom.Pt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Đọc/viết path data SVG — port 1:1 của {@code SvgPath.cs} (parse + write giữ
 * đường cong; server không làm phẳng nên phần flatten không port).
 *
 * Lệnh đủ: M L H V C S Q T A Z kể cả dạng tương đối. {@code A} (cung elip) và
 * {@code Q}/{@code T} (bậc hai) chuyển thành cubic ngay tại đây — sai số xấp xỉ
 * cung bằng cubic ≤90° dưới 0,02% bán kính.
 */
public final class SvgPath {

    private SvgPath() {
    }

    /** DS-92 — hai điểm gần hơn ngưỡng này coi là một (đơn vị người dùng, trước quy mm). */
    public static final double MERGE_TOLERANCE = 0.01;

    public static PathShape parse(String pathData) {
        List<Fig> figures = new ArrayList<>();
        List<Seg> segments = new ArrayList<>();

        Pt cur = new Pt(0, 0);
        Pt start = new Pt(0, 0);
        Pt lastCubicCtrl = null;
        Pt lastQuadCtrl = null;
        boolean hasStart = false;

        Scanner scan = new Scanner(pathData);
        char cmd = '\0';
        char[] cmdBox = new char[1];

        while (true) {
            cmdBox[0] = cmd;
            if (!scan.tryReadCommand(cmdBox)) {
                break;
            }
            cmd = cmdBox[0];
            boolean rel = Character.isLowerCase(cmd);
            char c = Character.toUpperCase(cmd);

            switch (c) {
                case 'M' -> {
                    if (hasStart && !segments.isEmpty()) {
                        figures.add(new Fig(start, List.copyOf(segments), false));
                    }
                    segments.clear();
                    cur = offset(rel, cur, scan.point());
                    start = cur;
                    hasStart = true;
                    lastCubicCtrl = lastQuadCtrl = null;
                    // Toạ độ tiếp theo sau M là các lệnh L ngầm
                    while (scan.hasNumber()) {
                        cur = offset(rel, cur, scan.point());
                        segments.add(Seg.lineTo(cur));
                    }
                }
                case 'L' -> {
                    while (scan.hasNumber()) {
                        cur = offset(rel, cur, scan.point());
                        segments.add(Seg.lineTo(cur));
                    }
                    lastCubicCtrl = lastQuadCtrl = null;
                }
                case 'H' -> {
                    while (scan.hasNumber()) {
                        double x = rel ? cur.x() + scan.number() : scan.number();
                        cur = new Pt(x, cur.y());
                        segments.add(Seg.lineTo(cur));
                    }
                    lastCubicCtrl = lastQuadCtrl = null;
                }
                case 'V' -> {
                    while (scan.hasNumber()) {
                        double y = rel ? cur.y() + scan.number() : scan.number();
                        cur = new Pt(cur.x(), y);
                        segments.add(Seg.lineTo(cur));
                    }
                    lastCubicCtrl = lastQuadCtrl = null;
                }
                case 'C' -> {
                    while (scan.hasNumber()) {
                        Pt c1 = offset(rel, cur, scan.point());
                        Pt c2 = offset(rel, cur, scan.point());
                        cur = offset(rel, cur, scan.point());
                        segments.add(Seg.cubic(c1, c2, cur));
                        lastCubicCtrl = c2;
                        lastQuadCtrl = null;
                    }
                }
                case 'S' -> {
                    while (scan.hasNumber()) {
                        Pt c1 = reflect(cur, lastCubicCtrl);
                        Pt c2 = offset(rel, cur, scan.point());
                        cur = offset(rel, cur, scan.point());
                        segments.add(Seg.cubic(c1, c2, cur));
                        lastCubicCtrl = c2;
                        lastQuadCtrl = null;
                    }
                }
                case 'Q' -> {
                    while (scan.hasNumber()) {
                        Pt ctrl = offset(rel, cur, scan.point());
                        Pt end = offset(rel, cur, scan.point());
                        segments.add(quadToCubic(cur, ctrl, end));
                        cur = end;
                        lastQuadCtrl = ctrl;
                        lastCubicCtrl = null;
                    }
                }
                case 'T' -> {
                    while (scan.hasNumber()) {
                        Pt ctrl = reflect(cur, lastQuadCtrl);
                        Pt end = offset(rel, cur, scan.point());
                        segments.add(quadToCubic(cur, ctrl, end));
                        cur = end;
                        lastQuadCtrl = ctrl;
                        lastCubicCtrl = null;
                    }
                }
                case 'A' -> {
                    while (scan.hasNumber()) {
                        double rx = scan.number();
                        double ry = scan.number();
                        double rot = scan.number();
                        boolean large = scan.flag();
                        boolean sweep = scan.flag();
                        Pt end = offset(rel, cur, scan.point());
                        segments.addAll(arcToCubic(cur, rx, ry, rot, large, sweep, end));
                        cur = end;
                        lastCubicCtrl = lastQuadCtrl = null;
                    }
                }
                case 'Z' -> {
                    // Khép đường: điểm cuối chưa trùng điểm đầu thì thêm đoạn nối về.
                    if (hasStart && !segments.isEmpty()
                            && cur.distanceTo(start) >= MERGE_TOLERANCE) {
                        segments.add(Seg.lineTo(start));
                    }
                    if (hasStart && !segments.isEmpty()) {
                        figures.add(new Fig(start, List.copyOf(segments), true));
                    }
                    segments.clear();
                    cur = start;
                }
                default -> {
                    // Lệnh không thuộc bộ đọc (ví dụ dữ liệu hỏng) — bỏ qua như client.
                }
            }
        }

        if (hasStart && !segments.isEmpty()) {
            figures.add(new Fig(start, List.copyOf(segments), false));
        }
        return new PathShape(figures);
    }

    /** Quét một danh sách số rời: viewBox, points, tham số transform. */
    public static List<Double> numbers(String text) {
        List<Double> list = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return list;
        }
        Scanner scan = new Scanner(text);
        while (scan.hasNumber()) {
            list.add(scan.number());
        }
        return list;
    }

    /** Viết lại biên dạng còn giữ đường cong — hợp đồng PartOutline.pathData. */
    public static String write(PathShape shape) {
        StringBuilder sb = new StringBuilder();
        for (Fig figure : shape.figures()) {
            if (figure.segments().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append("M ").append(num(figure.start().x())).append(',').append(num(figure.start().y()));

            int last = figure.segments().size() - 1;
            for (int i = 0; i < figure.segments().size(); i++) {
                Seg seg = figure.segments().get(i);
                // Đường kín: đoạn cuối quay về điểm đầu đã được Z diễn đạt, không viết lại.
                if (figure.closed() && i == last && seg.line()
                        && seg.end().distanceTo(figure.start()) < MERGE_TOLERANCE) {
                    break;
                }
                if (seg.line()) {
                    sb.append(" L ").append(num(seg.end().x())).append(',').append(num(seg.end().y()));
                } else {
                    sb.append(" C ").append(num(seg.c1().x())).append(',').append(num(seg.c1().y()))
                            .append(' ').append(num(seg.c2().x())).append(',').append(num(seg.c2().y()))
                            .append(' ').append(num(seg.end().x())).append(',').append(num(seg.end().y()));
                }
            }
            if (figure.closed()) {
                sb.append(" Z");
            }
        }
        return sb.toString();
    }

    /** Bốn chữ số thập phân = 0,1 micromet — mịn hơn thang Clipper của DS-89 mười lần. */
    static String num(double v) {
        double r = Math.round(v * 10000.0) / 10000.0;
        String s = String.format(Locale.ROOT, "%.4f", r);
        // Bỏ số 0 thừa và dấu chấm thừa — giống "0.####" của .NET.
        if (s.indexOf('.') >= 0) {
            int end = s.length();
            while (end > 0 && s.charAt(end - 1) == '0') {
                end--;
            }
            if (end > 0 && s.charAt(end - 1) == '.') {
                end--;
            }
            s = s.substring(0, end);
        }
        return s;
    }

    private static Pt offset(boolean rel, Pt cur, Pt p) {
        return rel ? new Pt(cur.x() + p.x(), cur.y() + p.y()) : p;
    }

    /** Tay nắm phản chiếu cho S / T; không có tay nắm trước thì dùng chính điểm hiện tại. */
    private static Pt reflect(Pt cur, Pt last) {
        return last != null ? new Pt(2 * cur.x() - last.x(), 2 * cur.y() - last.y()) : cur;
    }

    private static Seg quadToCubic(Pt p0, Pt c, Pt p1) {
        return Seg.cubic(
                new Pt(p0.x() + 2.0 / 3 * (c.x() - p0.x()), p0.y() + 2.0 / 3 * (c.y() - p0.y())),
                new Pt(p1.x() + 2.0 / 3 * (c.x() - p1.x()), p1.y() + 2.0 / 3 * (c.y() - p1.y())),
                p1);
    }

    /**
     * Cung elip A — chuyển từ tham số hoá theo hai đầu mút sang theo tâm, đúng phụ
     * lục F.6 của chuẩn SVG. Chia thành đoạn ≤90° để xấp xỉ cubic.
     */
    private static List<Seg> arcToCubic(Pt p0, double rx, double ry, double rotDeg,
                                        boolean largeArc, boolean sweep, Pt p1) {
        if (Math.abs(rx) < 1e-12 || Math.abs(ry) < 1e-12 || p0.distanceTo(p1) < 1e-12) {
            return List.of(Seg.lineTo(p1));
        }

        rx = Math.abs(rx);
        ry = Math.abs(ry);
        double phi = Math.toRadians(rotDeg);
        double cosP = Math.cos(phi);
        double sinP = Math.sin(phi);

        double dx2 = (p0.x() - p1.x()) / 2;
        double dy2 = (p0.y() - p1.y()) / 2;
        double x1 = cosP * dx2 + sinP * dy2;
        double y1 = -sinP * dx2 + cosP * dy2;

        // Bán kính quá nhỏ để nối hai đầu thì phóng to lên vừa đủ — chuẩn SVG yêu cầu vậy.
        double lambda = x1 * x1 / (rx * rx) + y1 * y1 / (ry * ry);
        if (lambda > 1) {
            double grow = Math.sqrt(lambda);
            rx *= grow;
            ry *= grow;
        }

        double sign = largeArc == sweep ? -1 : 1;
        double num = rx * rx * ry * ry - rx * rx * y1 * y1 - ry * ry * x1 * x1;
        double den = rx * rx * y1 * y1 + ry * ry * x1 * x1;
        double co = sign * Math.sqrt(Math.max(0, num / den));

        double cx1 = co * rx * y1 / ry;
        double cy1 = -co * ry * x1 / rx;
        double cx = cosP * cx1 - sinP * cy1 + (p0.x() + p1.x()) / 2;
        double cy = sinP * cx1 + cosP * cy1 + (p0.y() + p1.y()) / 2;

        double theta = angle(1, 0, (x1 - cx1) / rx, (y1 - cy1) / ry);
        double delta = angle((x1 - cx1) / rx, (y1 - cy1) / ry, (-x1 - cx1) / rx, (-y1 - cy1) / ry);

        if (!sweep && delta > 0) {
            delta -= 2 * Math.PI;
        } else if (sweep && delta < 0) {
            delta += 2 * Math.PI;
        }

        int count = Math.max(1, (int) Math.ceil(Math.abs(delta) / (Math.PI / 2)));
        double span = delta / count;

        // Hệ số tay nắm cho một cung góc span trên đường tròn đơn vị.
        double k = 4.0 / 3 * Math.tan(span / 4);

        double t0 = theta;
        final double fRx = rx;
        final double fRy = ry;
        final double fCx = cx;
        final double fCy = cy;

        List<Seg> segments = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double a = t0 + span * i;
            double b = a + span;

            Pt pa = onArc(a, fRx, fRy, cosP, sinP, fCx, fCy);
            Pt pb = i == count - 1 ? p1 : onArc(b, fRx, fRy, cosP, sinP, fCx, fCy);
            Pt ta = tangent(a, fRx, fRy, cosP, sinP);
            Pt tb = tangent(b, fRx, fRy, cosP, sinP);

            segments.add(Seg.cubic(
                    new Pt(pa.x() + k * ta.x(), pa.y() + k * ta.y()),
                    new Pt(pb.x() - k * tb.x(), pb.y() - k * tb.y()),
                    pb));
        }
        return segments;
    }

    private static Pt onArc(double t, double rx, double ry, double cosP, double sinP,
                            double cx, double cy) {
        double ct = Math.cos(t);
        double st = Math.sin(t);
        return new Pt(cosP * rx * ct - sinP * ry * st + cx,
                sinP * rx * ct + cosP * ry * st + cy);
    }

    private static Pt tangent(double t, double rx, double ry, double cosP, double sinP) {
        double ct = Math.cos(t);
        double st = Math.sin(t);
        return new Pt(-cosP * rx * st - sinP * ry * ct,
                -sinP * rx * st + cosP * ry * ct);
    }

    private static double angle(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy;
        double len = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
        double a = Math.acos(clamp(len < 1e-12 ? 1 : dot / len, -1, 1));
        return ux * vy - uy * vx < 0 ? -a : a;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /**
     * Quét chuỗi path. Cú pháp SVG cho phép bỏ dấu phân cách ở chỗ vẫn đọc được —
     * {@code 1.5.5} là hai số, {@code 1-2} cũng là hai số — nên quét từng ký tự.
     */
    static final class Scanner {
        private final String src;
        private int i;

        Scanner(String src) {
            this.src = src;
        }

        private void skipSeparators() {
            while (i < src.length() && (isWhitespace(src.charAt(i)) || src.charAt(i) == ',')) {
                i++;
            }
        }

        private static boolean isWhitespace(char c) {
            return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
        }

        private static boolean isLetter(char c) {
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        boolean tryReadCommand(char[] cmd) {
            skipSeparators();
            if (i >= src.length()) {
                return false;
            }
            if (isLetter(src.charAt(i))) {
                cmd[0] = src.charAt(i++);
                return true;
            }
            // Số đứng ngay sau một lệnh nghĩa là lặp lại lệnh đó. M lặp lại thành L.
            if (cmd[0] == '\0') {
                return false;
            }
            if (cmd[0] == 'M') {
                cmd[0] = 'L';
            } else if (cmd[0] == 'm') {
                cmd[0] = 'l';
            }
            return true;
        }

        boolean hasNumber() {
            skipSeparators();
            return i < src.length()
                    && (isDigit(src.charAt(i)) || src.charAt(i) == '-' || src.charAt(i) == '+' || src.charAt(i) == '.');
        }

        double number() {
            skipSeparators();
            int begin = i;
            if (i < src.length() && (src.charAt(i) == '-' || src.charAt(i) == '+')) {
                i++;
            }
            while (i < src.length() && isDigit(src.charAt(i))) {
                i++;
            }
            if (i < src.length() && src.charAt(i) == '.') {
                i++;
                while (i < src.length() && isDigit(src.charAt(i))) {
                    i++;
                }
            }
            if (i < src.length() && (src.charAt(i) == 'e' || src.charAt(i) == 'E')) {
                int save = i;
                i++;
                if (i < src.length() && (src.charAt(i) == '-' || src.charAt(i) == '+')) {
                    i++;
                }
                if (i < src.length() && isDigit(src.charAt(i))) {
                    while (i < src.length() && isDigit(src.charAt(i))) {
                        i++;
                    }
                } else {
                    i = save; // `e` không phải mũ thì trả lại
                }
            }
            if (begin == i) {
                return 0;
            }
            try {
                return Double.parseDouble(src.substring(begin, i));
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        /** Hai cờ của lệnh A là MỘT chữ số, viết dính liền số kế tiếp được. */
        boolean flag() {
            skipSeparators();
            if (i < src.length() && (src.charAt(i) == '0' || src.charAt(i) == '1')) {
                return src.charAt(i++) == '1';
            }
            return number() != 0;
        }

        Pt point() {
            return new Pt(number(), number());
        }
    }
}
