-- Migration V29: kho part file chỉ lưu SVG — app tự tách part (board 08/10, SA-Nesting §8).
--
-- Trước đây server tách part lúc upload (svg_file_parts) và app mở file qua /geometry. Cách tách
-- cũ coi mỗi <path> là một part nên lỗ khoét CorelDRAW thành part rời. Board chốt: server chỉ
-- giữ file SVG, app tải SVG (/api/v1/files/{id}/svg) rồi tự tách.
--
-- Đang phát triển, board cho XOÁ TOÀN BỘ kho part file cũ (local + prod) — admin tải lên lại.
-- File SVG vật lý trên đĩa của các bản ghi này thành file mồ côi, không ảnh hưởng gì.

-- 1. Xoá kho cũ. Các bảng trỏ vào svg_files đều ON DELETE CASCADE (svg_file_parts,
--    svg_file_vehicle_nodes, gán quyền V9).
DELETE FROM svg_files;

-- Bản thợ lưu nhớ file_key của part file gốc (V24, V28) — không có khoá ngoại; xoá tham chiếu
-- tới file không còn tồn tại.
UPDATE user_svg_files SET source_file_key = NULL WHERE source_file_key IS NOT NULL;
UPDATE user_svg_file_v2 SET source_file_key = NULL WHERE source_file_key IS NOT NULL;

-- 2. Bỏ bảng part.
DROP TABLE IF EXISTS svg_file_parts;

-- 3. Số part tính một lần lúc upload để danh sách file hiện số.
ALTER TABLE svg_files
    ADD COLUMN IF NOT EXISTS nested_part_count INTEGER,
    ADD COLUMN IF NOT EXISTS raw_part_count INTEGER;
