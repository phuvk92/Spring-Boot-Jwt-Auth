# -*- coding: utf-8 -*-
"""
Script to generate a comprehensive, professional Word Document (.docx)
for System API Integration Specification (Tài liệu Tích hợp API Hệ thống)
"""

import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

def set_cell_background(cell, hex_color):
    """Set background color for a table cell."""
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{hex_color}"/>')
    tc_pr.append(shd)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    """Set inner padding for a table cell in dxa (1 pt = 20 dxa)."""
    tc_pr = cell._tc.get_or_add_tcPr()
    tc_mar = parse_xml(
        f'<w:tcMar {nsdecls("w")}>'
        f'<w:top w:w="{top}" w:type="dxa"/>'
        f'<w:bottom w:w="{bottom}" w:type="dxa"/>'
        f'<w:left w:w="{left}" w:type="dxa"/>'
        f'<w:right w:w="{right}" w:type="dxa"/>'
        f'</w:tcMar>'
    )
    tc_pr.append(tc_mar)

def set_cell_border(cell, **kwargs):
    """Set cell borders: top, bottom, left, right, color, sz, val."""
    tc_pr = cell._tc.get_or_add_tcPr()
    tc_borders = parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'<w:top w:val="{kwargs.get("top_val", "single")}" w:sz="{kwargs.get("top_sz", "4")}" w:space="0" w:color="{kwargs.get("color", "CBD5E0")}"/>'
        f'<w:left w:val="{kwargs.get("left_val", "none")}" w:sz="0" w:space="0" w:color="auto"/>'
        f'<w:bottom w:val="{kwargs.get("bottom_val", "single")}" w:sz="{kwargs.get("bottom_sz", "4")}" w:space="0" w:color="{kwargs.get("color", "CBD5E0")}"/>'
        f'<w:right w:val="{kwargs.get("right_val", "none")}" w:sz="0" w:space="0" w:color="auto"/>'
        f'</w:tcBorders>'
    )
    tc_pr.append(tc_borders)

def add_header_styled(doc, text, level):
    h = doc.add_heading(text, level=level)
    h.paragraph_format.keep_with_next = True
    h.paragraph_format.space_before = Pt(14)
    h.paragraph_format.space_after = Pt(6)
    run = h.runs[0]
    if level == 1:
        run.font.name = 'Arial'
        run.font.size = Pt(16)
        run.font.bold = True
        run.font.color.rgb = RGBColor(0x1A, 0x36, 0x5D) # Dark Navy
    elif level == 2:
        run.font.name = 'Arial'
        run.font.size = Pt(13)
        run.font.bold = True
        run.font.color.rgb = RGBColor(0x2B, 0x6C, 0xB0) # Ocean Blue
    elif level == 3:
        run.font.name = 'Arial'
        run.font.size = Pt(11)
        run.font.bold = True
        run.font.color.rgb = RGBColor(0x2D, 0x37, 0x48) # Slate
    return h

def add_callout(doc, text, title="LƯU Ý QUAN TRỌNG"):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    
    cell = table.cell(0, 0)
    cell.width = Inches(6.5)
    set_cell_background(cell, "EBF8FF") # Soft Blue
    set_cell_margins(cell, top=140, bottom=140, left=200, right=140)
    
    # Left border thick blue
    tc_pr = cell._tc.get_or_add_tcPr()
    tc_borders = parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'<w:top w:val="none"/>'
        f'<w:left w:val="single" w:sz="24" w:space="0" w:color="3182CE"/>'
        f'<w:bottom w:val="none"/>'
        f'<w:right w:val="none"/>'
        f'</w:tcBorders>'
    )
    tc_pr.append(tc_borders)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(2)
    r_title = p.add_run(f"📌 {title}: ")
    r_title.bold = True
    r_title.font.name = 'Arial'
    r_title.font.size = Pt(10)
    r_title.font.color.rgb = RGBColor(0x2B, 0x6C, 0xB0)
    
    r_text = p.add_run(text)
    r_text.font.name = 'Arial'
    r_text.font.size = Pt(9.5)
    r_text.font.color.rgb = RGBColor(0x2D, 0x37, 0x48)
    
    doc.add_paragraph().paragraph_format.space_after = Pt(4)

def add_code_block(doc, code_text):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    
    cell = table.cell(0, 0)
    cell.width = Inches(6.5)
    set_cell_background(cell, "F7FAFC") # Light Gray
    set_cell_margins(cell, top=100, bottom=100, left=150, right=150)
    
    tc_pr = cell._tc.get_or_add_tcPr()
    tc_borders = parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'<w:top w:val="single" w:sz="6" w:space="0" w:color="E2E8F0"/>'
        f'<w:left w:val="single" w:sz="6" w:space="0" w:color="E2E8F0"/>'
        f'<w:bottom w:val="single" w:sz="6" w:space="0" w:color="E2E8F0"/>'
        f'<w:right w:val="single" w:sz="6" w:space="0" w:color="E2E8F0"/>'
        f'</w:tcBorders>'
    )
    tc_pr.append(tc_borders)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.line_spacing = 1.15
    
    run = p.add_run(code_text.strip())
    run.font.name = 'Courier New'
    run.font.size = Pt(8.5)
    run.font.color.rgb = RGBColor(0x2D, 0x37, 0x48)
    
    doc.add_paragraph().paragraph_format.space_after = Pt(4)

def add_custom_table(doc, headers, data, col_widths=None):
    table = doc.add_table(rows=len(data) + 1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    
    # Header row
    hdr_cells = table.rows[0].cells
    for i, header_text in enumerate(headers):
        hdr_cells[i].text = header_text
        set_cell_background(hdr_cells[i], "2B6CB0") # Ocean Blue
        set_cell_margins(hdr_cells[i], top=120, bottom=120, left=120, right=120)
        set_cell_border(hdr_cells[i], color="2B6CB0", bottom_sz="12")
        
        p = hdr_cells[i].paragraphs[0]
        p.paragraph_format.space_before = Pt(2)
        p.paragraph_format.space_after = Pt(2)
        for r in p.runs:
            r.font.name = 'Arial'
            r.font.size = Pt(9.5)
            r.font.bold = True
            r.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
            
    # Data rows
    for r_idx, row_data in enumerate(data):
        row_cells = table.rows[r_idx + 1].cells
        bg_color = "F7FAFC" if r_idx % 2 == 1 else "FFFFFF"
        for c_idx, cell_value in enumerate(row_data):
            row_cells[c_idx].text = str(cell_value)
            set_cell_background(row_cells[c_idx], bg_color)
            set_cell_margins(row_cells[c_idx], top=80, bottom=80, left=100, right=100)
            set_cell_border(row_cells[c_idx], color="E2E8F0", bottom_sz="4")
            
            p = row_cells[c_idx].paragraphs[0]
            p.paragraph_format.space_before = Pt(2)
            p.paragraph_format.space_after = Pt(2)
            for r in p.runs:
                r.font.name = 'Arial'
                r.font.size = Pt(9)
                r.font.color.rgb = RGBColor(0x2D, 0x37, 0x48)
                
    # Apply column widths
    if col_widths:
        for row in table.rows:
            for i, w in enumerate(col_widths):
                row.cells[i].width = Inches(w)
                
    doc.add_paragraph().paragraph_format.space_after = Pt(4)
    return table

def create_document():
    doc = docx.Document()
    
    # Page setup - Margins
    for section in doc.sections:
        section.top_margin = Inches(0.8)
        section.bottom_margin = Inches(0.8)
        section.left_margin = Inches(0.9)
        section.right_margin = Inches(0.9)
        
    # Styles
    style_normal = doc.styles['Normal']
    style_normal.font.name = 'Arial'
    style_normal.font.size = Pt(10)
    style_normal.font.color.rgb = RGBColor(0x2D, 0x37, 0x48)
    style_normal.paragraph_format.line_spacing = 1.2
    style_normal.paragraph_format.space_after = Pt(4)

    # ==================== COVER / TITLE SECTION ====================
    title_p = doc.add_paragraph()
    title_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    title_p.paragraph_format.space_before = Pt(36)
    title_p.paragraph_format.space_after = Pt(6)
    r_sys = title_p.add_run("HỆ THỐNG QUẢN LÝ CẮT FILM & MẪU XE (CUTTING ADMIN)")
    r_sys.font.name = 'Arial'
    r_sys.font.size = Pt(12)
    r_sys.font.bold = True
    r_sys.font.color.rgb = RGBColor(0x71, 0x80, 0x96)

    doc_p = doc.add_paragraph()
    doc_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    doc_p.paragraph_format.space_before = Pt(6)
    doc_p.paragraph_format.space_after = Pt(12)
    r_title = doc_p.add_run("TÀI LIỆU KỸ THUẬT TÍCH HỢP HỆ THỐNG API\n(SYSTEM API INTEGRATION SPECIFICATION)")
    r_title.font.name = 'Arial'
    r_title.font.size = Pt(18)
    r_title.font.bold = True
    r_title.font.color.rgb = RGBColor(0x1A, 0x36, 0x5D)

    sub_p = doc.add_paragraph()
    sub_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    sub_p.paragraph_format.space_before = Pt(0)
    sub_p.paragraph_format.space_after = Pt(24)
    r_sub = sub_p.add_run("Bao gồm: Nhóm Internal API cho Client Máy Cắt, Quản lý Cấu hình Xe, Kho Mẫu SVG, Quản Trị Bản Đã Lưu (User Saved Files), Đại Lý & Người Dùng")
    r_sub.font.name = 'Arial'
    r_sub.font.size = Pt(10.5)
    r_sub.font.italic = True
    r_sub.font.color.rgb = RGBColor(0x4A, 0x55, 0x68)

    # Info box
    info_table = doc.add_table(rows=5, cols=2)
    info_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    info_data = [
        ("Phiên bản hệ thống:", "1.0.0 (Release Enterprise)"),
        ("Ngày phát hành:", "27/09/2026"),
        ("Phạm vi tài liệu:", "Internal API (Máy cắt), Web Admin Console API, Partner Integration"),
        ("Giao thức xác thực:", "OAuth2 / OpenID Connect (Keycloak JWT Bearer Token)"),
        ("Định dạng dữ liệu:", "JSON (application/json), Binary Stream (image/svg+xml)")
    ]
    for idx, (k, v) in enumerate(info_data):
        row = info_table.rows[idx]
        row.cells[0].text = k
        row.cells[1].text = v
        row.cells[0].paragraphs[0].runs[0].bold = True
        set_cell_background(row.cells[0], "EDF2F7")
        set_cell_background(row.cells[1], "F7FAFC")
        set_cell_margins(row.cells[0], 60, 60, 100, 100)
        set_cell_margins(row.cells[1], 60, 60, 100, 100)
        set_cell_border(row.cells[0], color="CBD5E0")
        set_cell_border(row.cells[1], color="CBD5E0")
        row.cells[0].width = Inches(2.2)
        row.cells[1].width = Inches(4.3)

    doc.add_page_break()

    # ==================== CHƯƠNG 1: TỔNG QUAN KIẾN TRÚC & NGUYÊN TẮC ====================
    add_header_styled(doc, "1. Tổng quan Kiến trúc & Nguyên tắc Tích hợp", level=1)
    
    p = doc.add_paragraph()
    p.add_run(
        "Hệ thống Cutting Admin đóng vai trò là Cổng trung gian dịch vụ (API Gateway & Backend Layer) "
        "kết nối toàn diện giữa Ứng dụng Client máy cắt đặt tại các đại lý, Bảng điều khiển Web Admin, "
        "hệ thống Quản lý định danh Keycloak (SSO) và Kho lưu trữ dữ liệu SVG an toàn."
    )

    add_header_styled(doc, "1.1. Sơ đồ kiến trúc & luồng xử lý", level=2)
    p = doc.add_paragraph()
    p.add_run(
        "1. Client máy cắt (Desktop/Embedded App): Chỉ giao tiếp DUY NHẤT với backend cutting-admin thông qua nhóm Internal API.\n"
        "2. Backend cutting-admin: Đảm nhận xác thực tài khoản, kiểm tra trạng thái hoạt động của User và Đại lý trong DB, "
        "ủy quyền sang Keycloak để cấp phát JWT Bearer Token, kiểm tra ma trận quyền truy cập dữ liệu SVG và ghi nhật ký Audit Log.\n"
        "3. Keycloak SSO: Quản lý Identity Provider, Token Lifecycle (Access Token, Refresh Token).\n"
        "4. Database & Storage: PostgreSQL lưu trữ cấu hình xe, quan hệ phân quyền đại lý, audit logs; local/cloud storage lưu trữ an toàn các file SVG đã được khử độc khuẩn (Sanitized)."
    )

    add_callout(
        doc,
        "Ứng dụng Client máy cắt TUYỆT ĐỐI KHÔNG gọi trực tiếp sang Keycloak Admin API hoặc cổng cơ sở dữ liệu. "
        "Mọi tương tác từ Đăng nhập, Làm mới token, Đổi mật khẩu, Truy vấn chi tiết và Tải file SVG đều phải đi qua Backend cutting-admin.",
        title="NGUYÊN TẮC BẢO MẬT BẮT BUỘC"
    )

    add_header_styled(doc, "1.2. Môi trường kết nối (Environments)", level=2)
    env_headers = ["Môi trường", "Base URL", "Mục đích sử dụng"]
    env_data = [
        ["Local / Docker Compose", "http://localhost:8080", "Phục vụ phát triển, chạy thử nghiệm với Docker"],
        ["Staging Test Server", "https://staging-api.cuttingadmin.com", "Môi trường kiểm thử nghiệm thu nội bộ"],
        ["Production Server", "https://api.cuttingadmin.com", "Môi trường vận hành thực tế tại các xưởng cắt"]
    ]
    add_custom_table(doc, env_headers, env_data, [1.8, 2.7, 2.0])

    add_header_styled(doc, "1.3. Cơ chế Định danh Thiết bị & Địa chỉ IP (Audit Trails)", level=2)
    p = doc.add_paragraph()
    p.add_run(
        "Nhằm mục đích bảo mật, chống lạm dụng bản quyền file SVG và kiểm soát hoạt động theo từng máy cắt cụ thể, "
        "toàn bộ các API trong nhóm Internal API bắt buộc tiếp nhận 2 thông số:\n"
        "• Thiết bị (device): Tên hoặc mã định danh của máy cắt / máy tính điều khiển (ví dụ: 'Cutter-Roland-GX24', 'Graphtec-FC9000-140').\n"
        "• Địa chỉ IP (ipAddress): Địa chỉ IPv4/IPv6 của máy client (ví dụ: '192.168.1.88', '118.69.15.22').\n\n"
        "Hệ thống hỗ trợ linh hoạt các biến định danh JSON qua JsonAlias (cho phép gửi bằng tiếng Anh hoặc tiếng Việt):"
    )
    
    alias_headers = ["Thuộc tính chính", "Các Alias được hỗ trợ", "Kiểu dữ liệu", "Ghi chú"]
    alias_data = [
        ["device", "deviceName, device_name, thietBi, thiet_bi", "String", "Mã hoặc tên model thiết bị máy cắt"],
        ["ipAddress", "ip, ip_address, diaChiIp, dia_chi_ip", "String", "Địa chỉ IP client phát sinh request"]
    ]
    add_custom_table(doc, alias_headers, alias_data, [1.5, 2.5, 1.0, 1.5])

    add_header_styled(doc, "1.4. Quy chuẩn Định dạng Lỗi (Standard Error Response)", level=2)
    p = doc.add_paragraph()
    p.add_run("Mọi phản hồi lỗi từ hệ thống (HTTP 4xx, 5xx) đều tuân thủ cấu trúc JSON tiêu chuẩn:")
    
    error_sample = """{
  "timestamp": "2026-09-27T20:42:30.733Z",
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Invalid username or password",
  "path": "/api/internal/auth/login",
  "details": null
}"""
    add_code_block(doc, error_sample)

    doc.add_page_break()

    # ==================== CHƯƠNG 2: NHÓM INTERNAL API (MÁY CẮT) ====================
    add_header_styled(doc, "2. Nhóm Internal API (Dành cho Ứng dụng Client Máy Cắt)", level=1)
    
    p = doc.add_paragraph()
    p.add_run(
        "Nhóm API này là cổng giao tiếp độc quyền của phần mềm điều khiển máy cắt chạy tại các Đại lý / Workshop. "
        "User đăng nhập là nhân viên trực thuộc một Đại lý cụ thể."
    )

    # API 2.1: Login
    add_header_styled(doc, "2.1. Đăng nhập Client Máy Cắt (Internal Login)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Endpoint: ").bold = True
    p.add_run("POST /api/internal/auth/login\n")
    p.add_run("• Quyền truy cập: ").bold = True
    p.add_run("Public (Không yêu cầu Bearer token)\n")
    p.add_run("• Mục đích: ").bold = True
    p.add_run("Xác thực người dùng máy cắt, hỗ trợ đăng nhập linh hoạt bằng Gmail hoặc Username. Trả về JWT Access Token, Refresh Token và thông tin Đại lý trực thuộc.")

    p_req = doc.add_paragraph()
    p_req.add_run("Cấu trúc Payload Request (JSON):").bold = True
    login_req_table = [
        ["username", "String", "Bắt buộc", "Địa chỉ Gmail (VD: user@gmail.com) hoặc Username", "cut_op1@gmail.com"],
        ["password", "String", "Bắt buộc", "Mật khẩu tài khoản", "Password123!"],
        ["device", "String", "Tùy chọn*", "Tên/mã thiết bị máy cắt (Hỗ trợ alias: thietBi)", "Graphtec-FC9000-140"],
        ["ipAddress", "String", "Tùy chọn*", "Địa chỉ IP máy cắt (Hỗ trợ alias: diaChiIp)", "192.168.10.15"]
    ]
    add_custom_table(doc, ["Trường dữ liệu", "Kiểu", "Bắt buộc", "Mô tả", "Ví dụ"], login_req_table, [1.2, 0.8, 1.0, 2.3, 1.2])

    p_sample_req = doc.add_paragraph()
    p_sample_req.add_run("Ví dụ Request Body:")
    sample_login_body = """{
  "username": "operator1@gmail.com",
  "password": "Password123!",
  "thietBi": "Graphtec-FC9000-140",
  "diaChiIp": "192.168.10.15"
}"""
    add_code_block(doc, sample_login_body)

    p_sample_res = doc.add_paragraph()
    p_sample_res.add_run("Ví dụ Response Thành công (HTTP 200 OK):")
    sample_login_res = """{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 15,
    "username": "operator1@gmail.com",
    "fullName": "Kỹ thuật viên Cắt 01",
    "dealerId": 2,
    "dealerName": "Đại lý Hà Nội - AutoCare",
    "role": "USER"
  }
}"""
    add_code_block(doc, sample_login_res)

    # API 2.2: Refresh Token
    add_header_styled(doc, "2.2. Làm mới Access Token (Internal Refresh Token)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Endpoint: ").bold = True
    p.add_run("POST /api/internal/auth/refresh-token  (hoặc POST /api/internal/auth/refresh)\n")
    p.add_run("• Quyền truy cập: ").bold = True
    p.add_run("Public (Gửi refresh token hợp lệ trong body)\n")
    p.add_run("• Mục đích: ").bold = True
    p.add_run("Cấp lại JWT Access Token mới khi token cũ hết hạn (mặc định sau 60 phút) mà không yêu cầu người dùng phải gõ lại mật khẩu.")

    p_req2 = doc.add_paragraph()
    p_req2.add_run("Cấu trúc Payload Request (JSON):").bold = True
    ref_req_table = [
        ["refreshToken", "String", "Bắt buộc", "Chuỗi Refresh token nhận được khi login", "eyJhbGciOi..."],
        ["device", "String", "Tùy chọn*", "Tên/mã thiết bị máy cắt (hoặc thietBi)", "Graphtec-FC9000-140"],
        ["ipAddress", "String", "Tùy chọn*", "Địa chỉ IP máy client (hoặc diaChiIp)", "192.168.10.15"]
    ]
    add_custom_table(doc, ["Trường dữ liệu", "Kiểu", "Bắt buộc", "Mô tả", "Ví dụ"], ref_req_table, [1.2, 0.8, 1.0, 2.3, 1.2])

    p_sample_res2 = doc.add_paragraph()
    p_sample_res2.add_run("Ví dụ Response Thành công (HTTP 200 OK):")
    sample_ref_res = """{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.new_access_token...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.new_refresh_token...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 15,
    "username": "operator1@gmail.com",
    "fullName": "Kỹ thuật viên Cắt 01",
    "dealerId": 2,
    "dealerName": "Đại lý Hà Nội - AutoCare",
    "role": "USER"
  }
}"""
    add_code_block(doc, sample_ref_res)

    # API 2.3: Logout
    add_header_styled(doc, "2.3. Đăng xuất Client Máy Cắt (Internal Logout)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Endpoint: ").bold = True
    p.add_run("POST /api/internal/auth/logout\n")
    p.add_run("• Header: ").bold = True
    p.add_run("Authorization: Bearer <accessToken> (Khuyến nghị)\n")
    p.add_run("• Mục đích: ").bold = True
    p.add_run("Hủy phiên làm việc trên Keycloak, thu hồi Refresh Token và ghi nhận sự kiện Đăng xuất máy cắt vào cơ sở dữ liệu.")

    sample_logout_body = """{
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "thietBi": "Graphtec-FC9000-140",
  "diaChiIp": "192.168.10.15"
}"""
    add_code_block(doc, sample_logout_body)

    sample_logout_res = """{
  "success": true,
  "message": "Logged out successfully"
}"""
    add_code_block(doc, sample_logout_res)

    # API 2.4: Change Password
    add_header_styled(doc, "2.4. Đổi Mật Khẩu Người Dùng (Internal Change Password)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Endpoint: ").bold = True
    p.add_run("POST /api/internal/auth/change-password\n")
    p.add_run("• Header: ").bold = True
    p.add_run("Authorization: Bearer <accessToken> (Bắt buộc)\n")
    p.add_run("• Mục đích: ").bold = True
    p.add_run("Cho phép người dùng máy cắt tự đổi mật khẩu của chính mình. Hệ thống xác thực mật khẩu cũ qua Keycloak, đồng bộ mật khẩu mới và ghi nhận audit log.")

    p_pw_req = doc.add_paragraph()
    p_pw_req.add_run("Cấu trúc Payload Request (JSON):").bold = True
    pw_req_table = [
        ["currentPassword", "String", "Bắt buộc", "Mật khẩu hiện tại của tài khoản", "OldPassword123!"],
        ["newPassword", "String", "Bắt buộc", "Mật khẩu mới (ít nhất 6 ký tự)", "NewPassword456!"],
        ["confirmPassword", "String", "Bắt buộc", "Xác nhận lại mật khẩu mới", "NewPassword456!"],
        ["device", "String", "Tùy chọn*", "Tên/mã thiết bị máy cắt (hoặc thietBi)", "Graphtec-FC9000-140"],
        ["ipAddress", "String", "Tùy chọn*", "Địa chỉ IP máy client (hoặc diaChiIp)", "192.168.10.15"]
    ]
    add_custom_table(doc, ["Trường dữ liệu", "Kiểu", "Bắt buộc", "Mô tả", "Ví dụ"], pw_req_table, [1.3, 0.8, 1.0, 2.2, 1.2])

    sample_pw_res = """{
  "success": true,
  "message": "Password changed successfully"
}"""
    add_code_block(doc, sample_pw_res)

    # API 2.5: Get SVG Detail
    add_header_styled(doc, "2.5. Xem Chi Tiết File SVG & Cấu Hình Xe (Internal SVG Detail)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Endpoint: ").bold = True
    p.add_run("GET /api/internal/svg-files/{svgFileId}?device=...&ipAddress=...\n")
    p.add_run("• Header: ").bold = True
    p.add_run("Authorization: Bearer <accessToken> (Bắt buộc)\n")
    p.add_run("• Cơ chế phân quyền: ").bold = True
    p.add_run("Nếu User thuộc Đại lý có quyền VIEW đối với file SVG này (hoặc là ADMIN), API trả về chi tiết. Nếu Đại lý không được cấp quyền VIEW, hệ thống trả về 404 Not Found để bảo mật hoàn toàn sự tồn tại của file mẫu.")

    p_sample_svg_res = doc.add_paragraph()
    p_sample_svg_res.add_run("Ví dụ Response Thành công (HTTP 200 OK):")
    sample_svg_res = """{
  "id": 108,
  "originalFilename": "BMW_X5_G05_NgoaiThat_NapCapo.svg",
  "fileSize": 145892,
  "contentType": "image/svg+xml",
  "checksum": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "status": "ACTIVE",
  "vehicleConfigurations": [
    {
      "id": 5,
      "productGroup": "PPF_EXTERIOR",
      "productGroupDisplayName": "Ngoại thất (PPF Exterior)",
      "brandName": "BMW",
      "modelName": "X5",
      "yearFrom": 2019,
      "yearTo": 2024,
      "generationCode": "G05"
    }
  ],
  "permission": {
    "canView": true,
    "canDownload": true
  },
  "updatedAt": "2026-09-27T10:15:30+07:00"
}"""
    add_code_block(doc, sample_svg_res)

    # API 2.6: Download SVG
    add_header_styled(doc, "2.6. Tải Xuống File SVG Nhị Phân (Internal SVG Download)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Endpoint: ").bold = True
    p.add_run("GET /api/internal/svg-files/{svgFileId}/download?device=...&ipAddress=...\n")
    p.add_run("• Header: ").bold = True
    p.add_run("Authorization: Bearer <accessToken> (Bắt buộc)\n")
    p.add_run("• Quyền hạn & Phản hồi:\n").bold = True
    p.add_run(
        "  - HTTP 200 OK: Trả về Binary File Stream với Content-Type: image/svg+xml và Content-Disposition: attachment.\n"
        "  - HTTP 403 Forbidden: Đại lý của người dùng chỉ có quyền VIEW, KHÔNG có quyền DOWNLOAD ('You do not have permission to download this file').\n"
        "  - HTTP 404 Not Found: File không tồn tại hoặc Đại lý không có quyền VIEW."
    )

    # API 2.7: Save User SVG File
    add_header_styled(doc, "2.7. Lưu Bản Vẽ SVG Người Dùng (Internal Save User SVG File)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Endpoint: ").bold = True
    p.add_run("POST /api/internal/user-files\n")
    p.add_run("• Header: ").bold = True
    p.add_run("Authorization: Bearer <accessToken> (Bắt buộc - Role USER, AGENT hoặc ADMIN)\n")
    p.add_run("• Định dạng hỗ trợ: ").bold = True
    p.add_run("Hệ thống hỗ trợ 02 hình thức nạp dữ liệu linh hoạt:\n")
    p.add_run("   1) multipart/form-data: Tải file nhị phân đính kèm (param 'file') kèm metadata.\n")
    p.add_run("   2) application/json: Gửi nội dung XML chuỗi SVG trực tiếp trong body JSON (field 'svgContent').\n")
    p.add_run("• Mục đích: ").bold = True
    p.add_run("Cho phép người dùng máy cắt tại Đại lý lưu lại các bản vẽ SVG đã chỉnh sửa / xếp part trực tiếp từ phần mềm cắt. Hệ thống tự động khử độc SVG (Sanitize), tính mã băm SHA-256, lưu trữ an toàn và gắn bản lưu với tài khoản người dùng hiện tại.")

    p_save_params = doc.add_paragraph()
    p_save_params.add_run("Bảng Tham số Lưu File SVG Người Dùng:").bold = True
    user_file_param_table = [
        ["file", "Binary", "Bắt buộc*", "File SVG nhị phân đính kèm (dùng cho multipart/form-data)", "MyDesign.svg"],
        ["svgContent", "String", "Bắt buộc*", "Chuỗi nội dung XML SVG (dùng cho application/json)", "<svg viewBox=...>"],
        ["fileName", "String", "Tùy chọn", "Tên bản lưu hiển thị trên hệ thống", "BMW_X5_Door_Cut.svg"],
        ["categoryId", "Long", "Tùy chọn", "ID danh mục (PPF Exterior, Interior, Window Film...)", "1"],
        ["vehicleNodeId", "Long", "Tùy chọn", "ID node xe / cấu hình xe liên kết", "12"],
        ["brandName", "String", "Tùy chọn", "Tên hãng xe", "BMW"],
        ["modelName", "String", "Tùy chọn", "Tên dòng xe", "X5"],
        ["yearFrom / yearTo", "Integer", "Tùy chọn", "Năm sản xuất từ - đến", "2019 / 2024"],
        ["generationCode", "String", "Tùy chọn", "Mã đời / Mã khung xe", "G05"],
        ["productGroup", "String", "Tùy chọn", "Mã nhóm sản phẩm (PPF_EXTERIOR, PPF_INTERIOR, WINDOW_FILM)", "PPF_EXTERIOR"],
        ["filmWidth", "Double", "Tùy chọn", "Khổ phim chiều rộng Y (mm) thiết lập trên máy cắt", "1520.0"],
        ["rollLength", "Double", "Tùy chọn", "Chiều dài dọc cuộn X (mm) thiết lập trên máy cắt", "3500.0"],
        ["axisX / axisY", "Double", "Tùy chọn", "Tọa độ trục cắt X và Y (mm)", "3500.0 / 1520.0"],
        ["description", "String", "Tùy chọn", "Ghi chú mô tả bản lưu", "Bản cắt cửa sau trái"]
    ]
    add_custom_table(doc, ["Trường / Tham số", "Kiểu dữ liệu", "Bắt buộc", "Mô tả", "Ví dụ"], user_file_param_table, [1.3, 0.8, 1.0, 2.2, 1.2])

    p_sample_save_req = doc.add_paragraph()
    p_sample_save_req.add_run("Ví dụ Payload JSON Lưu File (POST /api/internal/user-files):").bold = True
    sample_save_json = """{
  "fileName": "BMW_X5_G05_DOOR_CUT.svg",
  "svgContent": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg viewBox=\"0 0 1520 3500\"><path d=\"M10 10 L100 100\"/></svg>",
  "categoryId": 1,
  "vehicleNodeId": 12,
  "brandName": "BMW",
  "modelName": "X5",
  "yearFrom": 2019,
  "yearTo": 2024,
  "generationCode": "G05",
  "productGroup": "PPF_EXTERIOR",
  "productGroupName": "PPF Exterior",
  "filmWidth": 1520.0,
  "filmWidthUnit": "MM",
  "rollLength": 3500.0,
  "rollLengthUnit": "MM",
  "axisX": 3500.0,
  "axisY": 1520.0,
  "description": "Bản cắt thử nghiệm cửa trước xe BMW X5"
}"""
    add_code_block(doc, sample_save_json)

    p_sample_save_res = doc.add_paragraph()
    p_sample_save_res.add_run("Ví dụ Phản hồi Thành công (HTTP 201 Created):").bold = True
    sample_save_res = """{
  "id": 5001,
  "fileName": "BMW_X5_G05_DOOR_CUT.svg",
  "originalFileName": "BMW_X5_G05_DOOR_CUT.svg",
  "category": {
    "id": 1,
    "name": "PPF Exterior"
  },
  "vehicleConfiguration": {
    "id": 12,
    "productGroup": "PPF_EXTERIOR",
    "productGroupName": "PPF Exterior",
    "brandName": "BMW",
    "modelName": "X5",
    "yearFrom": 2019,
    "yearTo": 2024,
    "generationCode": "G05"
  },
  "cutSize": {
    "filmWidth": 1520.0,
    "filmWidthUnit": "MM",
    "rollLength": 3500.0,
    "rollLengthUnit": "MM",
    "axisX": 3500.0,
    "axisY": 1520.0
  },
  "description": "Bản cắt thử nghiệm cửa trước xe BMW X5",
  "createdAt": "2026-10-03T22:00:00+07:00",
  "createdBy": {
    "id": 15,
    "username": "operator1@gmail.com",
    "displayName": "Kỹ thuật viên Cắt 01"
  },
  "dealer": {
    "id": 2,
    "name": "Đại lý Hà Nội - AutoCare"
  },
  "fileSize": 1258291,
  "mimeType": "image/svg+xml",
  "checksum": "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "status": "ACTIVE"
}"""
    add_code_block(doc, sample_save_res)

    # API 2.8: List & Detail User Saved Files
    add_header_styled(doc, "2.8. Danh Sách & Chi Tiết Bản Lưu Của Chính Người Dùng (Internal User Files)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Endpoint danh sách: ").bold = True
    p.add_run("GET /api/internal/user-files?keyword=...&categoryId=...&status=...&page=0&size=20\n")
    p.add_run("• Endpoint chi tiết: ").bold = True
    p.add_run("GET /api/internal/user-files/{id}\n")
    p.add_run("• Header: ").bold = True
    p.add_run("Authorization: Bearer <accessToken> (Bắt buộc)\n")
    p.add_run("• Nguyên tắc Bảo mật Ownership: ").bold = True
    p.add_run("Chỉ trả về danh sách và chi tiết các bản lưu do CHÍNH người dùng hiện tại tạo. Người dùng tuyệt đối không thể truy vấn hoặc xem bản lưu của người dùng khác qua nhóm endpoint này.")

    # API 2.9: Download User Saved File
    add_header_styled(doc, "2.9. Tải Xuống File SVG Bản Lưu Của Người Dùng (Internal Download User File)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Endpoint: ").bold = True
    p.add_run("GET /api/internal/user-files/{id}/download\n")
    p.add_run("• Header: ").bold = True
    p.add_run("Authorization: Bearer <accessToken> (Bắt buộc)\n")
    p.add_run("• Phản hồi: ").bold = True
    p.add_run("HTTP 200 OK trả về file stream SVG nhị phân kèm Content-Type: image/svg+xml và Content-Disposition: attachment; filename=\"...\".")

    doc.add_page_break()

    # ==================== CHƯƠNG 3: NHÓM API QUẢN LÝ CẤU HÌNH XE ====================
    add_header_styled(doc, "3. Nhóm API Quản lý Cấu hình Xe (Vehicle Configurations)", level=1)
    
    p = doc.add_paragraph()
    p.add_run(
        "Cấu hình xe là dữ liệu cốt lõi định danh chi tiết từng mẫu xe cần cắt dán film. "
        "Mỗi cấu hình xe bao gồm Nhóm sản phẩm, Hãng xe, Dòng xe, Năm sản xuất từ - đến và Mã khung / Mã đời."
    )

    add_header_styled(doc, "3.1. Danh mục 3 Nhóm Sản Phẩm Quy Chuẩn", level=2)
    pg_headers = ["Enum Key", "Tên Hiển Thị Tiếng Việt Quy Chuẩn", "Mô tả phạm vi"]
    pg_data = [
        ["PPF_EXTERIOR", "Ngoại thất (PPF Exterior)", "Film bảo vệ bề mặt sơn bên ngoài (Nắp capo, cản trước/sau, cửa...)"],
        ["PPF_INTERIOR", "Nội thất (PPF Interior)", "Film bảo vệ chi tiết nội thất (Màn hình giải trí, tapi cửa, cần số...)"],
        ["WINDOW_FILM", "Phim cách nhiệt (Window Film)", "Film cách nhiệt kính chắn gió, kính sườn và kính hậu"]
    ]
    add_custom_table(doc, pg_headers, pg_data, [1.8, 2.5, 2.2])

    add_header_styled(doc, "3.2. Danh sách các API Cấu hình Xe", level=2)
    vc_api_headers = ["Phương thức", "Endpoint", "Vai trò (Role)", "Chức năng"]
    vc_api_data = [
        ["GET", "/api/vehicle-configurations", "ADMIN", "Lấy danh sách cấu hình xe (phân trang, lọc theo nhóm, hãng, dòng, năm, từ khóa)"],
        ["GET", "/api/vehicle-configurations/{id}", "ADMIN", "Xem thông tin chi tiết một cấu hình xe theo ID"],
        ["POST", "/api/vehicle-configurations", "ADMIN", "Tạo mới cấu hình xe (bắt buộc kiểm tra năm từ <= năm đến, hãng/dòng hợp lệ)"],
        ["PUT", "/api/vehicle-configurations/{id}", "ADMIN", "Cập nhật cấu hình xe hiện có"],
        ["DELETE", "/api/vehicle-configurations/{id}", "ADMIN", "Xóa mềm (soft-delete) cấu hình xe (ngăn xóa nếu file SVG đang gán)"],
        ["GET", "/api/car-brands", "ADMIN, USER", "Lấy danh mục các Hãng xe đang hoạt động (Toyota, BMW, Mercedes...)"],
        ["GET", "/api/car-models?brandId={brandId}", "ADMIN, USER", "Lấy danh mục Dòng xe theo Hãng xe (Camry, X5, GLC...)"]
    ]
    add_custom_table(doc, vc_api_headers, vc_api_data, [1.1, 2.2, 1.2, 2.0])

    p_vc_req = doc.add_paragraph()
    p_vc_req.add_run("Ví dụ Request Body tạo Cấu hình xe (POST /api/vehicle-configurations):").bold = True
    sample_vc_create = """{
  "productGroup": "PPF_EXTERIOR",
  "brandId": 1,
  "modelId": 12,
  "yearFrom": 2019,
  "yearTo": 2024,
  "generationCode": "G05"
}"""
    add_code_block(doc, sample_vc_create)

    p_vc_res = doc.add_paragraph()
    p_vc_res.add_run("Ví dụ Response Cấu hình xe trả về:")
    sample_vc_res = """{
  "id": 5,
  "productGroup": "PPF_EXTERIOR",
  "productGroupDisplayName": "Ngoại thất (PPF Exterior)",
  "brand": {
    "id": 1,
    "name": "BMW"
  },
  "model": {
    "id": 12,
    "name": "X5"
  },
  "yearFrom": 2019,
  "yearTo": 2024,
  "generationCode": "G05",
  "status": "ACTIVE",
  "createdAt": "2026-09-25T08:30:00Z"
}"""
    add_code_block(doc, sample_vc_res)

    doc.add_page_break()

    # ==================== CHƯƠNG 4: NHÓM API KHO MẪU & PART FILE SVG ====================
    add_header_styled(doc, "4. Nhóm API Kho Mẫu & Part File SVG", level=1)
    
    p = doc.add_paragraph()
    p.add_run(
        "Module Kho mẫu & Part file cho phép ADMIN quản lý, tải lên nhiều file SVG (Batch Upload lên đến 10 file/lần), "
        "gán cho một hoặc nhiều Cấu hình xe, đồng thời phân quyền chi tiết (Xem / Tải) theo từng Đại lý."
    )

    add_header_styled(doc, "4.1. Quy tắc Kế thừa Phân quyền Đại lý (Dealer Permission Inheritance)", level=2)
    p = doc.add_paragraph()
    p.add_run(
        "1. File dùng chung: File SVG không gán cấu hình xe nào sẽ đóng vai trò là mẫu dùng chung.\n"
        "2. Phân quyền Đại lý: ADMIN gán quyền theo 2 cờ độc lập: canView (Xem chi tiết/preview) và canDownload (Tải file gốc để cắt).\n"
        "3. Kế thừa tự động: Mọi User thuộc Đại lý sẽ tự động kế thừa chính xác quyền hạn mà ADMIN đã cấp cho Đại lý đó."
    )

    add_header_styled(doc, "4.2. Danh sách các API Kho Mẫu SVG", level=2)
    svg_api_headers = ["Phương thức", "Endpoint", "Vai trò", "Chức năng"]
    svg_api_data = [
        ["POST", "/api/svg/batch", "ADMIN", "Upload tối đa 10 file SVG một lần kèm cấu hình xe và phân quyền đại lý"],
        ["GET", "/api/svg", "ADMIN, USER", "Tìm kiếm, lọc danh sách file SVG (lọc theo xe, nhóm sản phẩm, đại lý, từ khóa)"],
        ["GET", "/api/svg/{id}", "ADMIN, USER", "Xem thông tin metadata, cấu hình xe và danh sách quyền đại lý"],
        ["GET", "/api/svg/{id}/preview", "ADMIN, USER", "Stream nội dung SVG để hiển thị xem trước trực tiếp trên giao diện Web"],
        ["GET", "/api/svg/{id}/download", "ADMIN, USER", "Tải file SVG đính kèm (kiểm tra quyền canDownload của Đại lý)"],
        ["PUT", "/api/svg/{id}/dealers", "ADMIN", "Cập nhật/thay thế danh sách đại lý và quyền xem/tải cho file SVG"],
        ["PUT", "/api/svg/{id}/vehicle-configurations", "ADMIN", "Cập nhật danh sách cấu hình xe được gán cho file SVG"],
        ["DELETE", "/api/svg/{id}", "ADMIN", "Xóa mềm file SVG khỏi hệ thống"]
    ]
    add_custom_table(doc, svg_api_headers, svg_api_data, [1.1, 2.3, 1.2, 1.9])

    p_batch = doc.add_paragraph()
    p_batch.add_run("Ví dụ Tham số Gọi Batch Upload (POST /api/svg/batch - multipart/form-data):").bold = True
    p_batch_desc = doc.add_paragraph()
    p_batch_desc.add_run(
        "• files: Mảng từ 1 đến 10 file định dạng .svg (multipart binary)\n"
        "• vehicleConfigurationIds: Danh sách ID cấu hình xe gán cho các file (ví dụ: [5, 8])\n"
        "• dealerPermissions: Chuỗi JSON phân quyền đại lý, ví dụ: [{\"dealerId\": 1, \"canView\": true, \"canDownload\": true}, {\"dealerId\": 2, \"canView\": true, \"canDownload\": false}]"
    )

    doc.add_page_break()

    # ==================== CHƯƠNG 5: NHÓM API QUẢN TRỊ NGƯỜI DÙNG & ĐẠI LÝ ====================
    # ==================== CHƯƠNG 5: NHÓM API QUẢN TRỊ BẢN ĐÃ LƯU (ADMIN USER SAVED FILES) ====================
    add_header_styled(doc, "5. Nhóm API Quản trị Bản Đã Lưu Người Dùng (Admin User Saved SVG Files)", level=1)

    p = doc.add_paragraph()
    p.add_run(
        "Module 'Bản đã lưu' trên Web Admin phục vụ việc quản lý, giám sát và kiểm tra toàn bộ các file SVG "
        "do người dùng máy cắt tại các Đại lý lưu thông qua Internal API (POST /api/internal/user-files). "
        "Dữ liệu được lấy trực tiếp từ bảng cơ sở dữ liệu user_svg_files mà không sử dụng dữ liệu giả lập (mock)."
    )

    add_header_styled(doc, "5.1. Nguyên tắc Quản trị & Quyền Xem Toàn Bộ của ADMIN", level=2)
    add_callout(
        doc,
        "1. Phân quyền ADMIN độc lập: Khác với người dùng máy cắt thông thường chỉ được xem file của chính mình, "
        "ADMIN có quyền xem TOÀN BỘ file SVG do mọi User đã lưu trên toàn bộ hệ thống đại lý.\n"
        "2. Bảo mật đường dẫn vật lý: API Admin TUYỆT ĐỐI KHÔNG để lộ các trường storageKey, physicalPath hoặc "
        "URL lưu trữ nội bộ máy chủ.\n"
        "3. Enforce Phân quyền Backend: Quyền hạn được kiểm soát nghiêm ngặt ở tầng backend bằng @PreAuthorize(\"hasRole('ADMIN')\"). "
        "Tài khoản ROLE_USER hoặc ROLE_AGENT gọi vào nhóm API này sẽ bị từ chối với HTTP 403 Forbidden.",
        title="NGUYÊN TẮC BẢO MẬT & PHÂN QUYỀN ADMIN"
    )

    add_header_styled(doc, "5.2. Danh sách các API Admin Bản Đã Lưu", level=2)
    admin_user_file_api_headers = ["Phương thức", "Endpoint", "Vai trò", "Chức năng"]
    admin_user_file_api_data = [
        ["GET", "/api/admin/user-files", "ADMIN", "Lấy danh sách toàn bộ bản lưu SVG (phân trang, tìm kiếm, lọc theo nhiều tiêu chí)"],
        ["GET", "/api/admin/user-files/{id}", "ADMIN", "Xem chi tiết metadata đầy đủ của một bản lưu SVG (không lộ storageKey/path)"],
        ["GET", "/api/admin/user-files/{id}/download", "ADMIN", "Tải trực tiếp file SVG nhị phân về máy quản trị viên"],
        ["GET", "/api/admin/user-files/{id}/preview", "ADMIN", "Stream nội dung SVG inline an toàn để component Web hiển thị xem trước"]
    ]
    add_custom_table(doc, admin_user_file_api_headers, admin_user_file_api_data, [1.1, 2.3, 1.1, 2.0])

    p_admin_filter = doc.add_paragraph()
    p_admin_filter.add_run("Các Tham số Lọc & Tìm kiếm của API GET /api/admin/user-files:").bold = True
    admin_filter_params = [
        ["keyword", "String", "Tùy chọn", "Tìm kiếm theo tên file, tên file gốc hoặc mô tả bản lưu", "BMW_X5"],
        ["categoryId", "Long", "Tùy chọn", "Lọc theo ID danh mục sản phẩm", "1"],
        ["vehicleNodeId", "Long", "Tùy chọn", "Lọc theo ID node xe / cấu hình xe", "12"],
        ["brandId", "Long", "Tùy chọn", "Lọc theo ID hãng xe (BMW, Toyota, Mercedes...)", "1"],
        ["modelId", "Long", "Tùy chọn", "Lọc theo ID dòng xe (X5, Camry, GLC...)", "12"],
        ["dealerId", "Long", "Tùy chọn", "Lọc theo ID Đại lý", "2"],
        ["userId", "Long", "Tùy chọn", "Lọc theo ID người dùng tạo bản lưu", "15"],
        ["createdFrom", "ISO 8601", "Tùy chọn", "Lọc từ ngày tạo (VD: 2026-10-01T00:00:00)", "2026-10-01T00:00:00"],
        ["createdTo", "ISO 8601", "Tùy chọn", "Lọc đến ngày tạo (VD: 2026-10-03T23:59:59)", "2026-10-03T23:59:59"],
        ["status", "String", "Tùy chọn", "Lọc theo trạng thái (ACTIVE, DELETED). Mặc định trừ DELETED", "ACTIVE"],
        ["page / size", "Integer", "Mặc định", "Phân trang: page (từ 0), size (mặc định 20)", "page=0&size=20"],
        ["sort", "String", "Mặc định", "Sắp xếp: createdAt,desc hoặc fileName,asc", "createdAt,desc"]
    ]
    add_custom_table(doc, ["Tham số Query", "Kiểu", "Bắt buộc", "Mô tả", "Ví dụ"], admin_filter_params, [1.3, 0.8, 0.9, 2.3, 1.2])

    p_sample_admin_res = doc.add_paragraph()
    p_sample_admin_res.add_run("Ví dụ Cấu trúc Response Phân trang (GET /api/admin/user-files):").bold = True
    sample_admin_list_res = """{
  "content": [
    {
      "id": 5001,
      "fileName": "BMW_X5_G05_DOOR_CUT.svg",
      "originalFileName": "BMW_X5_G05_DOOR_CUT.svg",
      "category": {
        "id": 1,
        "name": "PPF Exterior"
      },
      "vehicleConfiguration": {
        "id": 12,
        "productGroup": "PPF_EXTERIOR",
        "productGroupName": "PPF Exterior",
        "brandName": "BMW",
        "modelName": "X5",
        "yearFrom": 2019,
        "yearTo": 2024,
        "generationCode": "G05"
      },
      "cutSize": {
        "filmWidth": 1520.0,
        "filmWidthUnit": "MM",
        "rollLength": 3500.0,
        "rollLengthUnit": "MM",
        "axisX": 3500.0,
        "axisY": 1520.0
      },
      "description": "Bản cắt thử nghiệm cửa trước xe BMW X5",
      "createdAt": "2026-10-03T22:00:00+07:00",
      "createdBy": {
        "id": 15,
        "username": "operator1@gmail.com",
        "displayName": "Kỹ thuật viên Cắt 01"
      },
      "dealer": {
        "id": 2,
        "name": "Đại lý Hà Nội - AutoCare"
      },
      "fileSize": 1258291,
      "mimeType": "image/svg+xml",
      "checksum": "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      "status": "ACTIVE"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}"""
    add_code_block(doc, sample_admin_list_res)

    doc.add_page_break()

    # ==================== CHƯƠNG 6: NHÓM API QUẢN TRỊ NGƯỜI DÙNG & ĐẠI LÝ ====================
    add_header_styled(doc, "6. Nhóm API Quản trị Người Dùng & Đại Lý (Users & Dealers)", level=1)
    
    p = doc.add_paragraph()
    p.add_run(
        "Hệ thống quản lý đại lý và người dùng phục vụ xác thực người dùng cho cả hệ thống Web Admin lẫn ứng dụng Client máy cắt. "
        "User được tạo phải liên kết chặt chẽ với Đại lý để thừa hưởng quyền hạn cắt film."
    )

    add_header_styled(doc, "6.1. Quy tắc Tạo Người Dùng Mới (User Creation Rules)", level=2)
    add_callout(
        doc,
        "1. Trường email (Gmail): Là BẮT BUỘC (@NotBlank, @Email).\n"
        "2. Trường username: Là TÙY CHỌN. Nếu không truyền hoặc để trống, hệ thống TỰ ĐỘNG lấy địa chỉ Gmail làm username đăng nhập.\n"
        "3. Trường dealerId: BẮT BUỘC liên kết với một Đại lý đang hoạt động (ACTIVE) trong hệ thống.\n"
        "4. Đồng bộ Keycloak: Người dùng được tự động khởi tạo trên Keycloak và gán mật khẩu tương ứng.",
        title="QUY TẮC TẠO TÀI KHOẢN NGƯỜI DÙNG"
    )

    add_header_styled(doc, "6.2. Danh sách các API Người Dùng & Đại Lý", level=2)
    user_api_headers = ["Phương thức", "Endpoint", "Vai trò", "Chức năng"]
    user_api_data = [
        ["GET", "/api/users", "ADMIN, AGENT", "Danh sách người dùng (phân trang, lọc theo email, username, role, trạng thái)"],
        ["GET", "/api/users/{id}", "ADMIN, AGENT", "Chi tiết tài khoản người dùng"],
        ["POST", "/api/users", "ADMIN, AGENT", "Tạo người dùng mới (Gmail bắt buộc, tự gán username, link Đại lý)"],
        ["PUT", "/api/users/{id}", "ADMIN, AGENT", "Cập nhật thông tin người dùng, đổi mật khẩu hoặc khóa tài khoản"],
        ["DELETE", "/api/users/{id}", "ADMIN", "Xóa tài khoản người dùng"],
        ["GET", "/api/dealers", "ADMIN, AGENT", "Danh sách đại lý (phân trang, tìm kiếm, lọc khu vực, trạng thái)"],
        ["GET", "/api/dealers/all", "ADMIN, AGENT", "Lấy toàn bộ danh sách đại lý đang hoạt động phục vụ Dropdown chọn khi tạo User"],
        ["POST", "/api/dealers", "ADMIN", "Tạo mới một đại lý (Mã đại lý, tên, địa chỉ, người liên hệ)"]
    ]
    add_custom_table(doc, user_api_headers, user_api_data, [1.1, 2.0, 1.3, 2.1])

    p_user_req = doc.add_paragraph()
    p_user_req.add_run("Ví dụ Request Body Tạo Người Dùng (POST /api/users):").bold = True
    sample_user_create = """{
  "email": "technician_hanoi@gmail.com",
  "password": "Password123!",
  "fullName": "Trần Văn Cường",
  "phoneNumber": "0987654321",
  "dealerId": 2,
  "role": "USER"
}"""
    add_code_block(doc, sample_user_create)

    doc.add_page_break()

    # ==================== CHƯƠNG 6: BẢNG MÃ LỖI & BEST PRACTICES ====================
    add_header_styled(doc, "7. Bảng Mã Lỗi Nghiệp Vụ & Khuyến Nghị Tích Hợp", level=1)

    add_header_styled(doc, "7.1. Bảng Tra Cứu Mã Lỗi Hệ Thống (Business Error Codes)", level=2)
    err_headers = ["HTTP Status", "Mã Lỗi (Error Code)", "Mô tả nguyên nhân & Cách khắc phục"]
    err_data = [
        ["400 Bad Request", "VALIDATION_FAILED", "Dữ liệu gửi lên thiếu các trường bắt buộc hoặc sai định dạng (VD: email sai định dạng)."],
        ["400 Bad Request", "YEAR_RANGE_INVALID", "Năm sản xuất từ (yearFrom) lớn hơn năm đến (yearTo) trong cấu hình xe."],
        ["400 Bad Request", "CURRENT_PASSWORD_INCORRECT", "Mật khẩu hiện tại không chính xác khi thực hiện đổi mật khẩu."],
        ["401 Unauthorized", "INVALID_CREDENTIALS", "Tài khoản hoặc mật khẩu không chính xác, hoặc người dùng bị vô hiệu hóa."],
        ["401 Unauthorized", "TOKEN_EXPIRED", "JWT Access Token hoặc Refresh Token đã hết hạn. Cần gọi API refresh-token hoặc login lại."],
        ["403 Forbidden", "ACCESS_DENIED", "Tài khoản không đủ quyền hạn (Role) để gọi endpoint quản trị."],
        ["403 Forbidden", "DOWNLOAD_PERMISSION_DENIED", "Đại lý của tài khoản chỉ được cấp quyền VIEW, không có quyền DOWNLOAD file SVG."],
        ["404 Not Found", "RESOURCE_NOT_FOUND", "Không tìm thấy cấu hình xe, đại lý, hoặc file SVG không tồn tại / Đại lý không có quyền VIEW."],
        ["409 Conflict", "CONFIG_ALREADY_EXISTS", "Cấu hình xe cho nhóm sản phẩm, hãng, dòng, năm và mã đời này đã tồn tại."],
        ["409 Conflict", "CONFIG_IN_USE", "Cấu hình xe đang được gán cho các file SVG trong hệ thống, không thể xóa."]
    ]
    add_custom_table(doc, err_headers, err_data, [1.4, 2.3, 2.8])

    add_header_styled(doc, "7.2. Khuyến nghị Tích hợp cho Lập trình viên Client Máy Cắt", level=2)
    p = doc.add_paragraph()
    p.add_run(
        "1. Quản lý Vòng đời Token:\n"
        "   • Sau khi login thành công, lưu trữ an toàn `accessToken` trong bộ nhớ và `refreshToken` trong kho lưu trữ bảo mật của hệ điều hành.\n"
        "   • Theo dõi thời gian hết hạn (`expiresIn` tính bằng giây, mặc định 3600s = 1 giờ). Chủ động gọi `POST /api/internal/auth/refresh-token` trước khi token hết hạn 5 phút.\n"
        "   • Khi nhận HTTP 401 trên bất kỳ API nghiệp vụ nào, tự động kích hoạt tiến trình refresh-token 01 lần. Nếu refresh thất bại, điều hướng người dùng về màn hình Đăng nhập.\n\n"
        "2. Luôn truyền Định danh Thiết bị & IP:\n"
        "   • Luôn cung cấp tên máy cắt và IP client trong các payload POST, hoặc gửi kèm trong HTTP Headers (`X-Device` và `X-Forwarded-For`). Điều này giúp người vận hành và quản trị viên dễ dàng theo dõi lịch sử cắt và hỗ trợ kỹ thuật khi gặp sự cố.\n\n"
        "3. Xử lý File SVG Tải về:\n"
        "   • File SVG tải từ `/api/internal/svg-files/{id}/download` đã được hệ thống chuẩn hóa, loại bỏ mã độc hại (XXE, Javascript script injection) và sẵn sàng chuyển tiếp sang bộ điều khiển dao cắt (G-code / HPGL / Plotter Engine)."
    )

    output_path = "/Users/phuvk/IdeaProjects/cutting-admin/Tai_Lieu_Tich_Hop_API_He_Thong.docx"
    doc.save(output_path)
    print(f"Document successfully created at: {output_path}")

if __name__ == "__main__":
    create_document()
