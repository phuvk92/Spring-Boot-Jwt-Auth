package com.example.svgmanager.svg;

import java.util.List;

/**
 * Nguyên thủy hình học cho việc tách part từ SVG — bản Java của
 * {@code Pcut.Client.Domain/Design/Affine.cs} và {@code PathShape.cs}.
 *
 * Đây là port 1:1 có chủ đích: SA-DanhMucXe-v2 §4 đòi part do server tách phải
 * trùng với part mà client tự nạp từ cùng một file, nên sai một nhánh điều kiện
 * là lệch con số so sánh. Mọi kích thước tính bằng mm (DS-86), ngưỡng gộp điểm
 * 0,01 mm (DS-92).
 */
public final class SvgGeom {

    private SvgGeom() {
    }

    /** Điểm 2D trong hệ toạ độ hiện hành (đơn vị người dùng hoặc mm). */
    public record Pt(double x, double y) {
        public double distanceTo(Pt o) {
            return Math.hypot(x - o.x, y - o.y);
        }
    }

    /** Phép biến đổi affine 2D — nhân theo thứ tự "áp a trước, rồi b". */
    public record Affine(double m11, double m12, double m21, double m22, double dx, double dy) {
        public static final Affine IDENTITY = new Affine(1, 0, 0, 1, 0, 0);

        public static Affine translate(double dx, double dy) {
            return new Affine(1, 0, 0, 1, dx, dy);
        }

        public static Affine scale(double sx, double sy) {
            return new Affine(sx, 0, 0, sy, 0, 0);
        }

        public static Affine rotate(double degrees) {
            double r = Math.toRadians(degrees);
            double sin = Math.sin(r);
            double cos = Math.cos(r);
            return new Affine(cos, sin, -sin, cos, 0, 0);
        }

        /** {@code a.mul(b)} = áp {@code a} trước, rồi {@code b} — như operator * bên client. */
        public Affine mul(Affine b) {
            return new Affine(
                    m11 * b.m11 + m12 * b.m21,
                    m11 * b.m12 + m12 * b.m22,
                    m21 * b.m11 + m22 * b.m21,
                    m21 * b.m12 + m22 * b.m22,
                    dx * b.m11 + dy * b.m21 + b.dx,
                    dx * b.m12 + dy * b.m22 + b.dy);
        }

        public Pt apply(Pt p) {
            return new Pt(p.x * m11 + p.y * m21 + dx, p.x * m12 + p.y * m22 + dy);
        }
    }

    /** Hộp bao trục-song-song. */
    public record Box(double x, double y, double width, double height) {
        public double right() {
            return x + width;
        }

        public double bottom() {
            return y + height;
        }

        public static Box around(List<Pt> points) {
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            for (Pt p : points) {
                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);
                maxX = Math.max(maxX, p.x);
                maxY = Math.max(maxY, p.y);
            }
            return minX > maxX ? new Box(0, 0, 0, 0) : new Box(minX, minY, maxX - minX, maxY - minY);
        }
    }
}
