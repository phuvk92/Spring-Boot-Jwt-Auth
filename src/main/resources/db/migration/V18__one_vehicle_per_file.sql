-- Board 30/09 bỏ quyết định Q5 (một file nhiều mẫu xe): mỗi part file gắn ĐÚNG một mẫu xe.
-- Giữ bảng liên kết (xoá node vẫn chỉ gỡ liên kết — Q4), chỉ khoá mỗi file tối đa một dòng.
-- File đang gắn nhiều mẫu thì giữ liên kết có node id nhỏ nhất.
DELETE FROM svg_file_vehicle_nodes a
USING svg_file_vehicle_nodes b
WHERE a.svg_file_id = b.svg_file_id
  AND a.vehicle_node_id > b.vehicle_node_id;

ALTER TABLE svg_file_vehicle_nodes
    ADD CONSTRAINT uq_sfvn_one_vehicle_per_file UNIQUE (svg_file_id);
