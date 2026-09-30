package com.example.svgmanager.svg;

import com.example.svgmanager.svg.PathShape.Fig;
import com.example.svgmanager.svg.PathShape.Seg;
import com.example.svgmanager.svg.SvgGeom.Pt;

import java.util.List;

/**
 * Hình cơ bản → biên dạng bezier — port của {@code ShapeFactory.cs}.
 * Sinh thẳng ở dạng bezier, không sinh đa giác.
 */
final class SvgShapes {

    private SvgShapes() {
    }

    /** Hệ số tay nắm để bốn cung cubic ghép thành đường tròn — sai số < 0,02% bán kính. */
    private static final double CIRCLE_K = 0.5522847498307936;

    static PathShape rectangle(double w, double h) {
        w = Math.abs(w);
        h = Math.abs(h);
        return new PathShape(List.of(new Fig(new Pt(0, 0), List.of(
                Seg.lineTo(new Pt(w, 0)),
                Seg.lineTo(new Pt(w, h)),
                Seg.lineTo(new Pt(0, h)),
                Seg.lineTo(new Pt(0, 0))), true)));
    }

    /**
     * Chữ nhật bo góc — rect có rx/ry. Bán kính kẹp về nửa cạnh đúng chuẩn SVG.
     */
    static PathShape roundedRectangle(double w, double h, double rx, double ry) {
        w = Math.abs(w);
        h = Math.abs(h);
        rx = clamp(Math.abs(rx), 0, w / 2);
        ry = clamp(Math.abs(ry), 0, h / 2);

        if (rx <= 0 || ry <= 0) {
            return rectangle(w, h);
        }

        double kx = rx * CIRCLE_K;
        double ky = ry * CIRCLE_K;
        Pt start = new Pt(rx, 0);

        return new PathShape(List.of(new Fig(start, List.of(
                Seg.lineTo(new Pt(w - rx, 0)),
                Seg.cubic(new Pt(w - rx + kx, 0), new Pt(w, ry - ky), new Pt(w, ry)),
                Seg.lineTo(new Pt(w, h - ry)),
                Seg.cubic(new Pt(w, h - ry + ky), new Pt(w - rx + kx, h), new Pt(w - rx, h)),
                Seg.lineTo(new Pt(rx, h)),
                Seg.cubic(new Pt(rx - kx, h), new Pt(0, h - ry + ky), new Pt(0, h - ry)),
                Seg.lineTo(new Pt(0, ry)),
                Seg.cubic(new Pt(0, ry - ky), new Pt(rx - kx, 0), start)), true)));
    }

    /** Elip nội tiếp hộp w×h. Hai cạnh bằng nhau thì ra hình tròn. */
    static PathShape ellipse(double w, double h) {
        w = Math.abs(w);
        h = Math.abs(h);
        double rx = w / 2;
        double ry = h / 2;
        double kx = rx * CIRCLE_K;
        double ky = ry * CIRCLE_K;

        Pt top = new Pt(rx, 0);
        Pt right = new Pt(w, ry);
        Pt bottom = new Pt(rx, h);
        Pt left = new Pt(0, ry);

        return new PathShape(List.of(new Fig(top, List.of(
                Seg.cubic(new Pt(rx + kx, 0), new Pt(w, ry - ky), right),
                Seg.cubic(new Pt(w, ry + ky), new Pt(rx - kx, h), bottom),
                Seg.cubic(new Pt(rx - kx, h), new Pt(0, ry + ky), left),
                Seg.cubic(new Pt(0, ry - ky), new Pt(rx - kx, 0), top)), true)));
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
