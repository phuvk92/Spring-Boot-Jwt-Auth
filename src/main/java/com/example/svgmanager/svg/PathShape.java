package com.example.svgmanager.svg;

import com.example.svgmanager.svg.SvgGeom.Affine;
import com.example.svgmanager.svg.SvgGeom.Box;
import com.example.svgmanager.svg.SvgGeom.Pt;

import java.util.ArrayList;
import java.util.List;

/**
 * Biên dạng một part: danh sách figure kín/hở, mỗi figure là chuỗi đoạn —
 * luôn cubic bezier, đoạn thẳng chỉ là trường hợp riêng có cờ {@code line}.
 * Port 1:1 từ {@code PathShape.cs}/{@code Figure}/{@code Segment} của client.
 */
public final class PathShape {

    /** Một đoạn của đường: thẳng (chỉ có end) hoặc cubic (c1, c2, end). */
    public record Seg(boolean line, Pt c1, Pt c2, Pt end) {
        public static Seg lineTo(Pt end) {
            return new Seg(true, null, null, end);
        }

        public static Seg cubic(Pt c1, Pt c2, Pt end) {
            return new Seg(false, c1, c2, end);
        }
    }

    /** Một đường liên tục. Nhiều figure ghép lại thành một hình có lỗ khoét. */
    public record Fig(Pt start, List<Seg> segments, boolean closed) {

        /** Số điểm neo — đường kín không đếm điểm khép lại thành neo riêng. */
        public int anchorCount() {
            return closed ? segments.size() : segments.size() + 1;
        }

        public Pt anchor(int i) {
            return i == 0 ? start : segments.get(i - 1).end();
        }

        public List<Pt> anchors() {
            List<Pt> pts = new ArrayList<>(anchorCount());
            for (int i = 0; i < anchorCount(); i++) {
                pts.add(anchor(i));
            }
            return pts;
        }

        public Fig transform(Affine m) {
            List<Seg> segs = new ArrayList<>(segments.size());
            for (Seg s : segments) {
                segs.add(s.line()
                        ? Seg.lineTo(m.apply(s.end()))
                        : Seg.cubic(m.apply(s.c1()), m.apply(s.c2()), m.apply(s.end())));
            }
            return new Fig(m.apply(start), segs, closed);
        }
    }

    public static final PathShape EMPTY = new PathShape(List.of());

    private final List<Fig> figures;

    public PathShape(List<Fig> figures) {
        this.figures = figures;
    }

    public List<Fig> figures() {
        return figures;
    }

    public int nodeCount() {
        return figures.stream().mapToInt(Fig::anchorCount).sum();
    }

    /**
     * Số lỗ khoét: figure kín nằm lọt trong figure kín khác — nhận biết bằng bao hàm
     * hình học chứ không bằng chiều quay, vì file xuất từ trình vẽ không giữ chiều.
     */
    public int holeCount() {
        List<Box> boxes = new ArrayList<>(figures.size());
        for (Fig f : figures) {
            boxes.add(Box.around(f.anchors()));
        }
        int n = 0;
        for (int i = 0; i < boxes.size(); i++) {
            for (int j = 0; j < boxes.size(); j++) {
                if (i != j && encloses(boxes.get(j), boxes.get(i))) {
                    n++;
                    break;
                }
            }
        }
        return n;
    }

    private static boolean encloses(Box outer, Box inner) {
        return outer.width() * outer.height() > inner.width() * inner.height()
                && outer.x() <= inner.x() && outer.y() <= inner.y()
                && outer.right() >= inner.right() && outer.bottom() >= inner.bottom();
    }

    public PathShape transform(Affine m) {
        List<Fig> out = new ArrayList<>(figures.size());
        for (Fig f : figures) {
            out.add(f.transform(m));
        }
        return new PathShape(out);
    }

    /** Hộp bao theo neo, không theo tay nắm — cùng quy ước với client. */
    public Box bounds() {
        List<Pt> pts = new ArrayList<>();
        for (Fig f : figures) {
            pts.addAll(f.anchors());
        }
        return Box.around(pts);
    }
}
