# -*- coding: utf-8 -*-
"""
Script to generate a comprehensive, professional Word Document (.docx)
for System API Integration Specification (Tài liệu Tích hợp API Hệ thống)
Updated: October 2026 - Version 2.0.0
Includes:
 - Internal API V1 & V2 (AES-256-GCM Encrypted SVG Files, Multi-tier Security Hardening)
 - User SVG File Sharing (Internal V1/V2 & Admin)
 - Part Library Categories (Danh mục kho mẫu & part - independent from vehicle categories)
 - Vehicle Configurations & Catalogs
 - User & Dealer Management (User Expiration Date & Validation)
 - Comprehensive Business Error Codes Reference
"""

import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import parse_xml
from docx.oxml.ns import nsdecls

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
        run.font.size = Pt(15)
        run.font.bold = True
        run.font.color.rgb = RGBColor(0x1A, 0x36, 0x5D) # Dark Navy
    elif level == 2:
        run.font.name = 'Arial'
        run.font.size = Pt(12.5)
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

def add_security_callout(doc, text, title="BẢO MẬT & AN TOÀN DỮ LIỆU"):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    
    cell = table.cell(0, 0)
    cell.width = Inches(6.5)
    set_cell_background(cell, "FFF5F5") # Soft Red/Orange
    set_cell_margins(cell, top=140, bottom=140, left=200, right=140)
    
    tc_pr = cell._tc.get_or_add_tcPr()
    tc_borders = parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'<w:top w:val="none"/>'
        f'<w:left w:val="single" w:sz="24" w:space="0" w:color="E53E3E"/>'
        f'<w:bottom w:val="none"/>'
        f'<w:right w:val="none"/>'
        f'</w:tcBorders>'
    )
    tc_pr.append(tc_borders)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(2)
    r_title = p.add_run(f"🛡️ {title}: ")
    r_title.bold = True
    r_title.font.name = 'Arial'
    r_title.font.size = Pt(10)
    r_title.font.color.rgb = RGBColor(0xC5, 0x30, 0x30)
    
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
    r_sub = sub_p.add_run("Bao gồm: Internal API V1 & V2 (Mã hoá AES-256-GCM, Bảo mật SVG), Chia sẻ Bản Đã Lưu, Danh mục Kho Mẫu & Part, Quản lý Cấu hình Xe, Quản lý Hạn dùng Người Dùng & Đại Lý")
    r_sub.font.name = 'Arial'
    r_sub.font.size = Pt(10.5)
    r_sub.font.italic = True
    r_sub.font.color.rgb = RGBColor(0x4A, 0x55, 0x68)

    # Info box
    info_table = doc.add_table(rows=5, cols=2)
    info_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    info_data = [
        ("Phiên bản tài liệu:", "2.0.0 (Enterprise Architecture Update - October 2026)"),
        ("Ngày cập nhật:", "07/10/2026"),
        ("Phạm vi kỹ thuật:", "Internal API V1 & V2, Web Admin Console API, Partner & Cutter App Integration"),
        ("Cơ chế xác thực:", "OAuth2 / OpenID Connect (Keycloak JWT Bearer Token, Device Limit F-57, User Expiration)"),
        ("Tiêu chuẩn bảo mật:", "Mã hoá AES-256-GCM Envelope Encryption, SVG XML Sanitization (Anti-XSS/XXE/SSRF), Strict CSP")
    ]
    for idx, (k, v) in enumerate(info_data):
        row = info_table.rows[idx]
        row.cells[0].text = k
        row.cells[1].text = v
        set_cell_background(row.cells[0], "EDF2F7")
        set_cell_background(row.cells[1], "F7FAFC")
        set_cell_margins(row.cells[0], top=60, bottom=60, left=100, right=100)
        set_cell_margins(row.cells[1], top=60, bottom=60, left=100, right=100)
        set_cell_border(row.cells[0], color="CBD5E0")
        set_cell_border(row.cells[1], color="CBD5E0")
        row.cells[0].paragraphs[0].runs[0].font.bold = True
        row.cells[0].paragraphs[0].runs[0].font.name = 'Arial'
        row.cells[0].paragraphs[0].runs[0].font.size = Pt(9.5)
        row.cells[1].paragraphs[0].runs[0].font.name = 'Arial'
        row.cells[1].paragraphs[0].runs[0].font.size = Pt(9.5)
        row.cells[0].width = Inches(2.2)
        row.cells[1].width = Inches(4.3)

    doc.add_page_break()

    # ==================== CHƯƠNG 1: TỔNG QUAN KIẾN TRÚC ====================
    add_header_styled(doc, "1. Tổng quan Kiến trúc & Nguyên tắc Tích hợp", level=1)
    
    p = doc.add_paragraph()
    p.add_run(
        "Hệ thống Cutting Admin đóng vai trò là Cổng trung gian dịch vụ (API Gateway & Backend Core) điều phối toàn bộ hoạt động "
        "quản trị mẫu cắt film PPF, cấu hình dòng xe, phân quyền đại lý và cung cấp API chuyên dụng cho phần mềm máy cắt tại các xưởng dịch vụ."
    )

    add_header_styled(doc, "1.1. Sơ đồ kiến trúc & luồng xử lý", level=2)
    p = doc.add_paragraph()
    p.add_run(
        "1. Client máy cắt (Desktop/Embedded App): Giao tiếp với backend cutting-admin thông qua nhóm Internal API (V1 hoặc V2 mã hóa AES-256-GCM).\n"
        "2. Backend cutting-admin: Đảm nhận xác thực tài khoản, kiểm tra hạn dùng (expires_at), giới hạn số máy đăng ký (F-57), ủy quyền sang Keycloak cấp JWT Bearer Token, kiểm tra ma trận quyền truy cập dữ liệu SVG và ghi nhật ký Audit Log.\n"
        "3. Keycloak SSO: Quản lý Identity Provider, Token Lifecycle (Access Token, Refresh Token).\n"
        "4. Database & Storage: PostgreSQL lưu trữ cấu hình xe, quan hệ phân quyền đại lý, kho mẫu, audit logs; local/cloud storage lưu trữ an toàn các file SVG đã mã hóa bảo vệ bản quyền."
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
        "Hệ thống hỗ trợ linh hoạt các biến định danh JSON qua JsonAlias (cho phép gửi bằng tiếng Anh hoặc tiếng Việt):\n\n🛡️ Tự động lưu vết Audit Trail cho User & Đại lý:\nMọi request gọi API từ người dùng máy cắt (USER) hoặc đại lý (AGENT) đều được hệ thống tự động lưu vết và ghi nhận vào bảng audit_logs thông qua AuditTrailInterceptor. Thông tin ghi nhận bao gồm: Phương thức HTTP, URI, Query string, Mã trạng thái HTTP (status), Thời gian xử lý (latency), Địa chỉ IP client, Định danh thiết bị (device), và Controller handler thực thi."
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
  "timestamp": "2026-10-07T00:15:30.733Z",
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "User account has expired",
  "path": "/api/internal/auth/login",
  "code": "USER_ACCOUNT_EXPIRED"
}"""
    add_code_block(doc, error_sample)

    doc.add_page_break()

    # ==================== CHƯƠNG 2: NHÓM INTERNAL API V1 ====================
    add_header_styled(doc, "2. Nhóm Internal API V1 (Dành cho Ứng dụng Client Máy Cắt)", level=1)
    
    p = doc.add_paragraph()
    p.add_run(
        "Nhóm API V1 là cổng giao tiếp chuẩn của phần mềm điều khiển máy cắt chạy tại các Đại lý / Workshop. "
        "User đăng nhập là kỹ thuật viên (USER) trực thuộc một Đại lý cụ thể."
    )

    # API 2.1: Login
    add_header_styled(doc, "2.1. Đăng nhập Client Máy Cắt (Internal Login)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Endpoint: ").bold = True
    p.add_run("POST /api/internal/auth/login\n")
    p.add_run("• Quyền truy cập: ").bold = True
    p.add_run("Public (Không yêu cầu Bearer token)\n")
    p.add_run("• Mục đích: ").bold = True
    p.add_run("Xác thực người dùng máy cắt, hỗ trợ đăng nhập linh hoạt bằng Gmail hoặc Username. Kiểm tra trạng thái hoạt động của User, hạn dùng tài khoản (expires_at) và kiểm soát giới hạn máy đăng ký (F-57).")

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
    "role": "USER",
    "expirationDate": "2026-12-31",
    "expired": false
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

    ref_req_table = [
        ["refreshToken", "String", "Bắt buộc", "Chuỗi Refresh token nhận được khi login", "eyJhbGciOi..."],
        ["device", "String", "Tùy chọn*", "Tên/mã thiết bị máy cắt (hoặc thietBi)", "Graphtec-FC9000-140"],
        ["ipAddress", "String", "Tùy chọn*", "Địa chỉ IP máy client (hoặc diaChiIp)", "192.168.10.15"]
    ]
    add_custom_table(doc, ["Trường dữ liệu", "Kiểu", "Bắt buộc", "Mô tả", "Ví dụ"], ref_req_table, [1.2, 0.8, 1.0, 2.3, 1.2])

    # API 2.3: Logout
    add_header_styled(doc, "2.3. Đăng xuất Client Máy Cắt (Internal Logout)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Endpoint: ").bold = True
    p.add_run("POST /api/internal/auth/logout\n")
    p.add_run("• Header: ").bold = True
    p.add_run("Authorization: Bearer <accessToken>\n")
    p.add_run("• Mục đích: ").bold = True
    p.add_run("Hủy phiên làm việc trên Keycloak, thu hồi Refresh Token và giải phóng phiên thiết bị.")

    # API 2.4: Change Password
    add_header_styled(doc, "2.4. Đổi Mật Khẩu Người Dùng (Internal Change Password)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Endpoint: ").bold = True
    p.add_run("POST /api/internal/auth/change-password\n")
    p.add_run("• Header: ").bold = True
    p.add_run("Authorization: Bearer <accessToken> (Bắt buộc)\n")
    p.add_run("• Mục đích: ").bold = True
    p.add_run("Cho phép thợ máy cắt tự đổi mật khẩu tài khoản trực tiếp trên ứng dụng client máy cắt.")

    # API 2.5: Internal SVG Detail
    add_header_styled(doc, "2.5. Xem Chi Tiết File SVG & Cấu Hình Xe (Internal SVG Detail)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Endpoint: ").bold = True
    p.add_run("GET /api/internal/svg-files/{svgFileId}?device=...&ipAddress=...\n")
    p.add_run("• Header: ").bold = True
    p.add_run("Authorization: Bearer <accessToken> (Bắt buộc)\n")
    p.add_run("• Kiểm tra quyền: ").bold = True
    p.add_run("Bắt buộc Đại lý của User phải có quyền canView = true đối với file SVG được yêu cầu.")

    # API 2.6: Internal SVG Download
    add_header_styled(doc, "2.6. Tải Xuống File SVG Nhị Phân (Internal SVG Download)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Endpoint: ").bold = True
    p.add_run("GET /api/internal/svg-files/{svgFileId}/download?device=...&ipAddress=...\n")
    p.add_run("• Header: ").bold = True
    p.add_run("Authorization: Bearer <accessToken> (Bắt buộc)\n")
    p.add_run("• Kiểm tra quyền: ").bold = True
    p.add_run("Bắt buộc Đại lý của User phải có cờ canDownload = true. Nếu chỉ có canView, hệ thống trả về HTTP 403 Forbidden (DOWNLOAD_PERMISSION_DENIED).")

    # API 2.7: Save User SVG File V1
    add_header_styled(doc, "2.7. Lưu Bản Vẽ SVG Người Dùng V1 (Internal Save User SVG File)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Endpoint: ").bold = True
    p.add_run("POST /api/internal/user-files\n")
    p.add_run("• Content-Type hỗ trợ: ").bold = True
    p.add_run("multipart/form-data (gửi file nhị phân) HOẶC application/json (gửi svgContent dạng chuỗi)\n")
    p.add_run("• Header: ").bold = True
    p.add_run("Authorization: Bearer <accessToken> (Bắt buộc - Role USER)\n")

    user_file_param_table = [
        ["file", "MultipartFile", "Bắt buộc*", "File SVG đính kèm (dùng cho multipart/form-data)", "file.svg"],
        ["svgContent", "String", "Bắt buộc*", "Chuỗi nội dung XML SVG (dùng cho application/json)", "<svg viewBox=...>"],
        ["fileName", "String", "Tùy chọn", "Tên bản lưu hiển thị trên hệ thống", "BMW_X5_Door_Cut.svg"],
        ["categoryId", "Long", "Tùy chọn", "ID danh mục (PPF Exterior, Interior, Window Film...)", "1"],
        ["vehicleNodeId", "Long", "Tùy chọn", "ID node xe / cấu hình xe liên kết", "12"],
        ["brandName / modelName", "String", "Tùy chọn", "Hãng xe và Dòng xe", "BMW / X5"],
        ["yearFrom / yearTo", "Integer", "Tùy chọn", "Năm sản xuất từ - đến", "2019 / 2024"],
        ["generationCode", "String", "Tùy chọn", "Mã đời / Mã khung xe", "G05"],
        ["filmWidth / rollLength", "Double", "Tùy chọn", "Khổ phim (Y) và Chiều dài dọc cuộn (X) tính bằng mm", "1520.0 / 3500.0"],
        ["axisX / axisY", "Double", "Tùy chọn", "Tọa độ trục cắt X và Y (mm)", "3500.0 / 1520.0"],
        ["description", "String", "Tùy chọn", "Ghi chú mô tả bản lưu", "Bản cắt cửa sau trái"]
    ]
    add_custom_table(doc, ["Trường / Tham số", "Kiểu dữ liệu", "Bắt buộc", "Mô tả", "Ví dụ"], user_file_param_table, [1.3, 0.8, 1.0, 2.2, 1.2])

    # API 2.8: List & Detail User Saved Files
    add_header_styled(doc, "2.8. Danh Sách & Chi Tiết Bản Lưu Của Chính Người Dùng (Internal User Files V1)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Endpoint danh sách: ").bold = True
    p.add_run("GET /api/internal/user-files?keyword=...&categoryId=...&status=...&page=0&size=20\n")
    p.add_run("• Endpoint chi tiết: ").bold = True
    p.add_run("GET /api/internal/user-files/{id}\n")
    p.add_run("• Header: ").bold = True
    p.add_run("Authorization: Bearer <accessToken> (Bắt buộc)\n")
    p.add_run("• Phạm vi dữ liệu: ").bold = True
    p.add_run("Trả về danh sách bản lưu do chính người dùng tạo (accessType=OWNER) và các bản lưu được người dùng khác chia sẻ (accessType=SHARED).")

    # API 2.9: Download User Saved File V1
    add_header_styled(doc, "2.9. Tải Xuống File SVG Bản Lưu Của Người Dùng V1 (Internal Download User File)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Endpoint: ").bold = True
    p.add_run("GET /api/internal/user-files/{id}/download\n")
    p.add_run("• Header: ").bold = True
    p.add_run("Authorization: Bearer <accessToken> (Bắt buộc)\n")

    # API 2.10: Preview, Update, Delete V1
    add_header_styled(doc, "2.10. Xem Trước, Cập Nhật & Xóa Bản Lưu V1", level=2)
    p = doc.add_paragraph()
    p.add_run("• Xem trước (Inline SVG): ").bold = True
    p.add_run("GET /api/internal/user-files/{id}/preview — Stream nội dung SVG nhị phân phục vụ hiển thị trực tiếp trên giao diện thợ cắt.\n")
    p.add_run("• Cập nhật bản lưu: ").bold = True
    p.add_run("PUT /api/internal/user-files/{id} — Ghi đè file SVG hoặc cập nhật metadata (hỗ trợ multipart/form-data và application/json).\n")
    p.add_run("• Xóa bản lưu: ").bold = True
    p.add_run("DELETE /api/internal/user-files/{id} — Xóa mềm bản lưu (Soft delete, status chuyển sang DELETED). Chỉ người tạo bản lưu mới có quyền xóa.")

    # API 2.11: File Sharing V1
    add_header_styled(doc, "2.11. Chia Sẻ Bản Lưu SVG Giữa Các User (Internal File Sharing V1)", level=2)
    add_callout(
        doc,
        "1. Quyền chia sẻ: Chỉ CHỦ SỞ HỮU (Owner) của bản lưu mới có quyền chia sẻ file cho USER khác hoặc thu hồi chia sẻ.\n"
        "2. Chống tự chia sẻ: Hệ thống ngăn chặn tự chia sẻ cho chính mình (trả về mã lỗi CANNOT_SHARE_TO_SELF).\n"
        "3. Quyền của người nhận: Người dùng được chia sẻ chỉ có quyền XEM (Preview) và TẢI (Download); KHÔNG có quyền chỉnh sửa, xóa hoặc chia sẻ tiếp cho người thứ ba.",
        title="QUY TẮC PHÂN QUYỀN CHIA SẺ FILE SVG V1"
    )

    share_api_headers = ["Phương thức", "Endpoint", "Vai trò", "Chức năng"]
    share_api_data = [
        ["POST", "/api/internal/user-files/{id}/shares", "USER (Owner)", "Chia sẻ file SVG cho một người dùng khác"],
        ["GET", "/api/internal/user-files/{id}/shares", "USER (Owner)", "Lấy danh sách người dùng đang được chia sẻ file này"],
        ["DELETE", "/api/internal/user-files/{id}/shares/{targetUserId}", "USER (Owner)", "Thu hồi quyền chia sẻ của một người dùng"]
    ]
    add_custom_table(doc, share_api_headers, share_api_data, [1.1, 2.7, 1.2, 1.5])

    p_share_req = doc.add_paragraph()
    p_share_req.add_run("Ví dụ Request Body Chia Sẻ File (POST /api/internal/user-files/{id}/shares):").bold = True
    sample_share_req = """{
  "userId": 2002
}"""
    add_code_block(doc, sample_share_req)

    p_share_res = doc.add_paragraph()
    p_share_res.add_run("Ví dụ Response Thành công Chia Sẻ File (HTTP 201 Created):").bold = True
    sample_share_res = """{
  "userId": 2002,
  "username": "thocathcm@gmail.com",
  "displayName": "Kỹ thuật viên HCM 02",
  "dealerId": 5,
  "dealerName": "Đại lý TP.HCM - Workshop",
  "sharedAt": "2026-10-06T14:30:00",
  "sharedBy": {
    "userId": 15,
    "username": "operator1@gmail.com"
  },
  "status": "ACTIVE"
}"""
    add_code_block(doc, sample_share_res)

    doc.add_page_break()

    # ==================== CHƯƠNG 3: INTERNAL API V2 ====================
    add_header_styled(doc, "3. Nhóm Internal API V2 – Quản lý File SVG Mã Hoá AES-256-GCM & Bảo Mật Đa Lớp", level=1)
    
    p = doc.add_paragraph()
    p.add_run(
        "Nhóm Internal API V2 (`/api/internal/v2/user-files`) là phân hệ lưu trữ và xử lý bản vẽ SVG thế hệ mới, "
        "được thiết kế với kiến trúc độc lập hoàn toàn (Isolated Architecture) và cơ chế mã hóa dữ liệu đầu-cuối (At-Rest Encryption) "
        "đáp ứng các tiêu chuẩn bảo mật dữ liệu cấp doanh nghiệp."
    )

    add_header_styled(doc, "3.1. Kiến Trúc Hoàn Toàn Độc Lập & Cơ Chế Mã Hoá AES-256-GCM (Envelope Encryption)", level=2)
    add_callout(
        doc,
        "1. Độc lập hoàn toàn với V1: Toàn bộ Controller, Service, Repository, Entity (UserSvgFileV2, UserSvgFileV2Share), bảng cơ sở dữ liệu (user_svg_files_v2, user_svg_file_v2_shares) và phân vùng lưu trữ (storage/v2/) đều tách biệt 100% với V1. Không chia sẻ business logic, đảm bảo nâng cấp hoặc rollback V2 mà không ảnh hưởng V1.\n"
        "2. Thuật toán mã hoá AES-256-GCM: Mỗi file SVG được mã hoá bằng một Data Encryption Key (DEK) 256-bit sinh ngẫu nhiên (CSPRNG). DEK được bao bọc an toàn bởi Master Key (Envelope Encryption). Dữ liệu mã hóa bao gồm Authentication Tag 128-bit và IV ngẫu nhiên 96-bit, đảm bảo cả tính bảo mật (Confidentiality) lẫn tính toàn vẹn (Integrity).\n"
        "3. File lưu trữ nhị phân an toàn: Tệp lưu trên đĩa máy chủ hoàn toàn ở dạng ciphertext nhị phân (.bin/.enc). Kể cả khi phân vùng lưu trữ bị rò rỉ, dữ liệu vector SVG gốc không thể bị phục hồi.\n"
        "4. Giải mã On-The-Fly trong RAM: Khi client gọi Preview hoặc Download, hệ thống giải mã tức thời trong bộ nhớ và stream về client; tuyệt đối không tạo file tạm plaintext trên ổ cứng.",
        title="NGUYÊN TẮC KIẾN TRÚC & MÃ HOÁ V2"
    )

    add_header_styled(doc, "3.2. Tiêu Chuẩn Bảo Mật SVG Đa Tầng (SVG Security Hardening)", level=2)
    add_security_callout(
        doc,
        "File SVG thực chất là tài liệu XML có khả năng thực thi mã JavaScript và nhúng tài nguyên bên ngoài. Do đó, API V2 áp dụng quy trình kiểm soát an ninh nghiêm ngặt trước khi lưu trữ:\n\n"
        "1. Không tin tưởng đuôi file (.svg): Kiểm tra đồng thời Extension + Content-Type (image/svg+xml) + Magic Bytes + XML Well-formed parsing. Từ chối file đổi tên từ .exe, .html, .sh.\n"
        "2. Loại bỏ Active Elements: Cấm tuyệt đối thẻ <script>, <foreignObject>, <iframe>, <object>, <embed>, <applet>, <meta>, <link>.\n"
        "3. Vô hiệu hóa Event Handlers: Cấm tất cả thuộc tính sự kiện JavaScript (onload, onerror, onclick, onmouseover, onfocus...).\n"
        "4. Chặn URL Schema nguy hiểm: Cấm giao thức javascript:, vbscript:, data: không phải ảnh.\n"
        "5. Chống XXE & SSRF: Vô hiệu hóa triệt để XML DOCTYPE, External Entities, DTD declarations, XML Processing Instructions.\n"
        "6. Chống XML Bomb (Billion Laughs): Giới hạn độ sâu phân cấp XML (Max Depth = 100), giới hạn số lượng thẻ (Max Elements = 100,000).\n"
        "7. Giới hạn dung lượng an toàn: Cấu hình linh hoạt (mặc định 20MB). File vượt ngưỡng sẽ bị từ chối với lỗi SVG_SIZE_LIMIT_EXCEEDED.\n"
        "8. Safe Response Headers: Mọi phản hồi Preview/Download đều bắt buộc bổ sung headers:\n"
        "   • Content-Security-Policy: default-src 'none'; img-src 'self' data:; style-src 'unsafe-inline';\n"
        "   • X-Content-Type-Options: nosniff\n"
        "   • Cache-Control: private, no-store",
        title="CHÍNH SÁCH BẢO MẬT & KIỂM DUYỆT SVG V2"
    )

    add_header_styled(doc, "3.3. Danh Sách Các Endpoint Internal API V2", level=2)
    v2_api_headers = ["Phương thức", "Endpoint", "Vai trò", "Chức năng & Mô tả bảo mật"]
    v2_api_data = [
        ["POST", "/api/internal/v2/user-files", "USER", "Lưu file SVG V2 (Multipart): Kiểm duyệt XML, mã hoá AES-256-GCM và lưu trữ an toàn"],
        ["POST", "/api/internal/v2/user-files", "USER", "Lưu file SVG V2 (JSON): Nhận svgContent chuỗi, kiểm duyệt, mã hoá AES-256-GCM"],
        ["GET", "/api/internal/v2/user-files", "USER", "Danh sách bản lưu V2 (phân trang, keyword, category, status; gồm bản tạo & bản được chia sẻ)"],
        ["GET", "/api/internal/v2/user-files/{id}", "USER", "Chi tiết metadata bản lưu V2 (không lộ DEK, master key hay đường dẫn lưu trữ vật lý)"],
        ["GET", "/api/internal/v2/user-files/{id}/preview", "USER", "Xem trước file SVG V2 (Giải mã on-the-fly, stream inline kèm Strict CSP & nosniff)"],
        ["GET", "/api/internal/v2/user-files/{id}/download", "USER", "Tải file SVG V2 đã giải mã (Content-Disposition: attachment, filename gốc)"],
        ["PUT", "/api/internal/v2/user-files/{id}", "USER (Owner)", "Ghi đè file SVG V2 / cập nhật thông số cắt (Multipart hoặc JSON, tự động re-encrypt)"],
        ["DELETE", "/api/internal/v2/user-files/{id}", "USER (Owner)", "Xoá mềm một bản lưu SVG V2 của chính người dùng (status = DELETED)"],
        ["POST", "/api/internal/v2/user-files/{fileId}/shares", "USER (Owner)", "Chia sẻ file SVG V2 cho người dùng khác theo target userId"],
        ["GET", "/api/internal/v2/user-files/{fileId}/shares", "USER (Owner)", "Lấy danh sách người dùng được chia sẻ file SVG V2"],
        ["DELETE", "/api/internal/v2/user-files/{fileId}/shares/{targetUserId}", "USER (Owner)", "Thu hồi quyền chia sẻ file SVG V2"]
    ]
    add_custom_table(doc, v2_api_headers, v2_api_data, [1.1, 2.7, 1.1, 1.6])

    p_v2_sample_req = doc.add_paragraph()
    p_v2_sample_req.add_run("Ví dụ Request Body Lưu File V2 (JSON - POST /api/internal/v2/user-files):").bold = True
    sample_v2_upload_json = """{
  "fileName": "BMW_X5_G05_DOOR_ENCRYPTED.svg",
  "svgContent": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1520 3500\"><path d=\"M50 50 L200 200\" stroke=\"black\" fill=\"none\"/></svg>",
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
  "description": "Bản cắt cửa trước bên phụ mã hoá bảo mật AES-256"
}"""
    add_code_block(doc, sample_v2_upload_json)

    p_v2_sample_res = doc.add_paragraph()
    p_v2_sample_res.add_run("Ví dụ Response Thành công V2 (HTTP 201 Created - UserSvgFileV2Response):").bold = True
    sample_v2_upload_res = """{
  "id": 8001,
  "fileName": "BMW_X5_G05_DOOR_ENCRYPTED.svg",
  "originalFileName": "BMW_X5_G05_DOOR_ENCRYPTED.svg",
  "description": "Bản cắt cửa trước bên phụ mã hoá bảo mật AES-256",
  "fileSize": 142050,
  "mimeType": "image/svg+xml",
  "checksum": "sha256:d41d8cd98f00b204e9800998ecf8427e998ecf8427e1234567890abcdef12345",
  "status": "ACTIVE",
  "accessType": "OWNER",
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
  "createdBy": {
    "id": 15,
    "username": "operator1@gmail.com",
    "displayName": "Kỹ thuật viên Cắt 01"
  },
  "dealer": {
    "id": 2,
    "name": "Đại lý Hà Nội - AutoCare"
  },
  "createdAt": "2026-10-07T00:20:00",
  "updatedAt": "2026-10-07T00:20:00"
}"""
    add_code_block(doc, sample_v2_upload_res)

    p_v2_share_res = doc.add_paragraph()
    p_v2_share_res.add_run("Ví dụ Response Chia Sẻ File V2 (HTTP 201 Created - UserSvgFileV2ShareResponse):").bold = True
    sample_v2_share_res = """{
  "id": 9005,
  "fileId": 8001,
  "targetUser": {
    "id": 2002,
    "username": "thocathcm@gmail.com",
    "displayName": "Kỹ thuật viên HCM 02"
  },
  "dealer": {
    "id": 5,
    "name": "Đại lý TP.HCM - Workshop"
  },
  "sharedBy": {
    "id": 15,
    "username": "operator1@gmail.com",
    "displayName": "Kỹ thuật viên Cắt 01"
  },
  "sharedAt": "2026-10-07T00:22:00"
}"""
    add_code_block(doc, sample_v2_share_res)

    doc.add_page_break()

    # ==================== CHƯƠNG 4: NHÓM API CẤU HÌNH XE ====================
    add_header_styled(doc, "4. Nhóm API Quản lý Cấu hình Xe (Vehicle Configurations)", level=1)
    
    p = doc.add_paragraph()
    p.add_run(
        "Cấu hình xe là dữ liệu cốt lõi định danh chi tiết từng mẫu xe cần cắt dán film. "
        "Mỗi cấu hình xe bao gồm Nhóm sản phẩm, Hãng xe, Dòng xe, Năm sản xuất từ - đến và Mã khung / Mã đời."
    )

    add_header_styled(doc, "4.1. Danh mục 3 Nhóm Sản Phẩm Quy Chuẩn", level=2)
    pg_headers = ["Enum Key", "Tên Hiển Thị Tiếng Việt Quy Chuẩn", "Mô tả phạm vi"]
    pg_data = [
        ["PPF_EXTERIOR", "Ngoại thất (PPF Exterior)", "Film bảo vệ bề mặt sơn bên ngoài (Nắp capo, cản trước/sau, cửa...)"],
        ["PPF_INTERIOR", "Nội thất (PPF Interior)", "Film bảo vệ chi tiết nội thất (Màn hình giải trí, tapi cửa, cần số...)"],
        ["WINDOW_FILM", "Phim cách nhiệt (Window Film)", "Film cách nhiệt kính chắn gió, kính sườn và kính hậu"]
    ]
    add_custom_table(doc, pg_headers, pg_data, [1.8, 2.5, 2.2])

    add_header_styled(doc, "4.2. Danh sách các API Cấu hình Xe", level=2)
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

    doc.add_page_break()

    # ==================== CHƯƠNG 5: NHÓM API KHO MẪU & PART FILE SVG ====================
    add_header_styled(doc, "5. Nhóm API Kho Mẫu & Part File SVG", level=1)

    add_header_styled(doc, "5.1. Quy tắc Kế thừa Phân quyền Đại lý (Dealer Permission Inheritance)", level=2)
    p = doc.add_paragraph()
    p.add_run(
        "1. File dùng chung: File SVG không gán cấu hình xe nào sẽ đóng vai trò là mẫu dùng chung.\n"
        "2. Phân quyền Đại lý: ADMIN gán quyền theo 2 cờ độc lập: canView (Xem chi tiết/preview) và canDownload (Tải file gốc để cắt).\n"
        "3. Kế thừa tự động: Mọi User thuộc Đại lý sẽ tự động kế thừa chính xác quyền hạn mà ADMIN đã cấp cho Đại lý đó."
    )

    add_header_styled(doc, "5.2. Danh sách các API Kho Mẫu SVG", level=2)
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

    # 5.3: Part Library Categories
    add_header_styled(doc, "5.3. Nhóm API Quản lý Danh mục Kho Mẫu & Part (Part Library Categories)", level=2)
    add_callout(
        doc,
        "⚠️ QUY TẮC PHÂN TÁCH DANH MỤC:\n"
        "1. Tách biệt hoàn toàn với Danh mục xe: 'Danh mục kho mẫu & part' là domain hoàn toàn mới (PartLibraryCategory, bảng part_library_category), không dùng chung bảng category, không dùng chung DTO, không mapping ID với Danh mục xe.\n"
        "2. Mục đích: Phục vụ phân loại các part file trong Kho mẫu (ví dụ: Ngoại thất, Nội thất, Đèn pha, Cản trước, Kính hậu...).\n"
        "3. Quyền hạn: ADMIN toàn quyền quản lý; Client/User có quyền xem danh sách ACTIVE để phục vụ lọc mẫu.",
        title="ĐỘC LẬP HOÀN TOÀN GIỮA DANH MỤC KHO MẪU & DANH MỤC XE"
    )

    plc_api_headers = ["Phương thức", "Endpoint", "Vai trò", "Chức năng"]
    plc_api_data = [
        ["GET", "/api/part-library-categories", "ADMIN", "Lấy danh sách Danh mục kho mẫu & part có phân trang, tìm kiếm keyword"],
        ["GET", "/api/part-library-categories/active", "ADMIN, USER", "Lấy toàn bộ danh sách Danh mục kho mẫu & part đang ACTIVE (cho dropdown/filter)"],
        ["GET", "/api/part-library-categories/{id}", "ADMIN", "Lấy thông tin chi tiết một danh mục theo ID"],
        ["POST", "/api/part-library-categories", "ADMIN", "Tạo mới Danh mục kho mẫu & part (code duy nhất, name, status ACTIVE/INACTIVE)"],
        ["PUT", "/api/part-library-categories/{id}", "ADMIN", "Cập nhật Danh mục kho mẫu & part theo ID"],
        ["DELETE", "/api/part-library-categories/{id}", "ADMIN", "Xóa Danh mục kho mẫu & part khỏi hệ thống"]
    ]
    add_custom_table(doc, plc_api_headers, plc_api_data, [1.1, 2.7, 1.2, 1.5])

    p_plc_req = doc.add_paragraph()
    p_plc_req.add_run("Ví dụ Request Body Tạo Danh mục Kho Mẫu & Part (POST /api/part-library-categories):").bold = True
    sample_plc_create = """{
  "code": "PPF_EXTERIOR",
  "name": "Ngoại thất (PPF Exterior)",
  "status": "ACTIVE"
}"""
    add_code_block(doc, sample_plc_create)

    p_plc_res = doc.add_paragraph()
    p_plc_res.add_run("Ví dụ Response Thành công (HTTP 200 OK / 201 Created):").bold = True
    sample_plc_res = """{
  "id": 1,
  "code": "PPF_EXTERIOR",
  "name": "Ngoại thất (PPF Exterior)",
  "status": "ACTIVE",
  "createdAt": "2026-10-06T10:00:00",
  "updatedAt": "2026-10-06T10:00:00",
  "createdBy": "admin",
  "updatedBy": "admin",
  "usageCount": 18
}"""
    add_code_block(doc, sample_plc_res)

    doc.add_page_break()

    # ==================== CHƯƠNG 6: NHÓM API QUẢN TRỊ BẢN ĐÃ LƯU ====================
    add_header_styled(doc, "6. Nhóm API Quản trị Bản Đã Lưu Người Dùng (Admin User Saved Files)", level=1)
    
    add_header_styled(doc, "6.1. Nguyên tắc Quản trị & Quyền Xem Toàn Bộ của ADMIN", level=2)
    add_callout(
        doc,
        "1. Phân quyền ADMIN độc lập: Khác với người dùng máy cắt thông thường chỉ được xem file của chính mình hoặc file được chia sẻ, ADMIN có quyền xem TOÀN BỘ file SVG do mọi User đã lưu trên toàn bộ hệ thống đại lý.\n"
        "2. Bảo mật đường dẫn vật lý: API Admin TUYỆT ĐỐI KHÔNG để lộ các trường storageKey, physicalPath hoặc URL lưu trữ nội bộ máy chủ.\n"
        "3. Enforce Phân quyền Backend: Quyền hạn được kiểm soát nghiêm ngặt ở tầng backend bằng @PreAuthorize(\"hasRole('ADMIN')\").\n"
        "4. Chức năng Chia sẻ Bản đã lưu: ADMIN có quyền chia sẻ trực tiếp bất kỳ bản lưu nào cho một hoặc nhiều USER khác thông qua Button 'Chia sẻ' tại cột Thao tác trên Web Admin.",
        title="NGUYÊN TẮC BẢO MẬT & PHÂN QUYỀN ADMIN"
    )

    add_header_styled(doc, "6.2. Danh sách các API Admin Bản Đã Lưu & Chia Sẻ File", level=2)
    admin_saved_headers = ["Phương thức", "Endpoint", "Vai trò", "Chức năng"]
    admin_saved_data = [
        ["GET", "/api/admin/user-files", "ADMIN", "Lấy danh sách toàn bộ bản lưu SVG (phân trang, tìm kiếm, lọc theo nhiều tiêu chí)"],
        ["GET", "/api/admin/user-files/{id}", "ADMIN", "Xem chi tiết metadata đầy đủ của một bản lưu SVG (không lộ storageKey/path)"],
        ["GET", "/api/admin/user-files/{id}/download", "ADMIN", "Tải trực tiếp file SVG nhị phân về máy quản trị viên"],
        ["GET", "/api/admin/user-files/{id}/preview", "ADMIN", "Stream nội dung SVG inline an toàn để component Web hiển thị xem trước"],
        ["POST", "/api/admin/user-files/{id}/shares", "ADMIN", "ADMIN chia sẻ file SVG đã lưu của người dùng cho một USER khác"],
        ["GET", "/api/admin/user-files/{id}/shares", "ADMIN", "Xem danh sách toàn bộ người dùng đang được chia sẻ file SVG này"],
        ["DELETE", "/api/admin/user-files/{id}/shares/{targetUserId}", "ADMIN", "ADMIN thu hồi quyền chia sẻ file SVG của một người dùng"]
    ]
    add_custom_table(doc, admin_saved_headers, admin_saved_data, [1.1, 2.6, 1.1, 1.7])

    p_admin_share_req = doc.add_paragraph()
    p_admin_share_req.add_run("Ví dụ Request Body ADMIN Chia Sẻ File (POST /api/admin/user-files/{id}/shares):").bold = True
    sample_admin_share_req = """{
  "userId": 2002
}"""
    add_code_block(doc, sample_admin_share_req)

    doc.add_page_break()

    # ==================== CHƯƠNG 7: NHÓM API QUẢN TRỊ NGƯỜI DÙNG & ĐẠI LÝ ====================
    add_header_styled(doc, "7. Nhóm API Quản trị Người Dùng & Đại Lý (Users & Dealers)", level=1)
    
    p = doc.add_paragraph()
    p.add_run(
        "Hệ thống quản lý đại lý và người dùng phục vụ xác thực người dùng cho cả hệ thống Web Admin lẫn ứng dụng Client máy cắt. "
        "User được tạo phải liên kết chặt chẽ với Đại lý để thừa hưởng quyền hạn cắt film."
    )

    add_header_styled(doc, "7.1. Quy tắc Tạo Người Dùng & Ngày Hết Hạn Tài Khoản (Expiration Date)", level=2)
    add_callout(
        doc,
        "1. Trường email (Gmail): Là BẮT BUỘC (@NotBlank, @Email).\n"
        "2. Trường username: Là TÙY CHỌN. Nếu để trống, hệ thống TỰ ĐỘNG lấy địa chỉ Gmail làm username đăng nhập.\n"
        "3. Trường dealerId: BẮT BUỘC liên kết với một Đại lý đang hoạt động (ACTIVE) trong hệ thống.\n"
        "4. Đồng bộ Keycloak: Người dùng được tự động khởi tạo trên Keycloak và gán mật khẩu tương ứng.\n"
        "5. Quy tắc NGÀY HẾT HẠN TÀI KHOẢN (expirationDate - Định dạng YYYY-MM-DD):\n"
        "   • ADMIN: KHÔNG áp dụng kiểm tra ngày hết hạn.\n"
        "   • AGENT: Áp dụng kiểm tra ngày hết hạn nếu có thiết lập.\n"
        "   • USER: Áp dụng kiểm tra ngày hết hạn nếu có thiết lập.\n"
        "   • Ngày hết hạn là TÙY CHỌN (Optional). Nếu để trống, tài khoản hoạt động vĩnh viễn không hết hạn.\n"
        "   • Nếu ngày hiện tại vượt quá ngày hết hạn: Chặn đăng nhập và từ chối cấp token với mã lỗi USER_ACCOUNT_EXPIRED.\n"
        "   • Bộ lọc expirationStatus trên API GET /api/users: ALL, VALID, EXPIRED, NO_EXPIRATION.",
        title="QUY TẮC TẠO TÀI KHOẢN & NGÀY HẾT HẠN"
    )

    add_header_styled(doc, "7.2. Danh sách các API Người Dùng & Đại Lý", level=2)
    user_api_headers = ["Phương thức", "Endpoint", "Vai trò", "Chức năng"]
    user_api_data = [
        ["GET", "/api/users", "ADMIN, AGENT", "Danh sách người dùng (phân trang, lọc theo email, role, status, expirationStatus)"],
        ["GET", "/api/users/{id}", "ADMIN, AGENT", "Chi tiết tài khoản người dùng (bao gồm expirationDate, expired)"],
        ["POST", "/api/users", "ADMIN, AGENT", "Tạo người dùng mới (Gmail bắt buộc, link Đại lý, tùy chọn expirationDate)"],
        ["PUT", "/api/users/{id}", "ADMIN, AGENT", "Cập nhật thông tin người dùng, đổi mật khẩu, gia hạn/đặt expirationDate"],
        ["DELETE", "/api/users/{id}", "ADMIN", "Xóa tài khoản người dùng"],
        ["GET", "/api/dealers", "ADMIN, AGENT", "Danh sách đại lý (phân trang, tìm kiếm, lọc khu vực, trạng thái)"],
        ["GET", "/api/dealers/all", "ADMIN, AGENT", "Lấy toàn bộ danh sách đại lý đang hoạt động phục vụ Dropdown chọn khi tạo User"],
        ["POST", "/api/dealers", "ADMIN", "Tạo mới một đại lý (Mã đại lý, tên, địa chỉ, người liên hệ)"]
    ]
    add_custom_table(doc, user_api_headers, user_api_data, [1.1, 2.0, 1.3, 2.1])

    p_user_req = doc.add_paragraph()
    p_user_req.add_run("Ví dụ Request Body Tạo Người Dùng Kèm Ngày Hết Hạn (POST /api/users):").bold = True
    sample_user_create = """{
  "email": "technician_hanoi@gmail.com",
  "password": "Password123!",
  "fullName": "Trần Văn Cường",
  "phoneNumber": "0987654321",
  "dealerId": 2,
  "role": "USER",
  "expirationDate": "2026-12-31"
}"""
    add_code_block(doc, sample_user_create)

    p_user_res = doc.add_paragraph()
    p_user_res.add_run("Ví dụ Response Thông Tin Người Dùng (HTTP 200 OK / 201 Created):").bold = True
    sample_user_res = """{
  "id": 15,
  "username": "technician_hanoi@gmail.com",
  "email": "technician_hanoi@gmail.com",
  "fullName": "Trần Văn Cường",
  "phone": "0987654321",
  "role": "USER",
  "dealerId": 2,
  "dealerName": "Đại lý Hà Nội - AutoCare",
  "enabled": true,
  "expirationDate": "2026-12-31",
  "expired": false,
  "effectiveMaxDevices": 1,
  "createdAt": "2026-10-05T09:00:00",
  "updatedAt": "2026-10-05T09:00:00"
}"""
    add_code_block(doc, sample_user_res)

    # 7.3: Audit Trail & Activity Log API
    add_header_styled(doc, "7.3. Nhóm API Quản trị Audit Trail & Activity Log (Audit Logs)", level=2)
    add_callout(
        doc,
        "1. Tự động thu thập: Mọi hoạt động gọi API của USER (thợ cắt) và AGENT (đại lý) đều được tự động ghi nhận vào cơ sở dữ liệu.\n"
        "2. Quyền truy cập: Chỉ tài khoản có vai trò ADMIN mới được phép truy vấn danh sách và chi tiết Audit Log.\n"
        "3. Tìm kiếm & Lọc đa tiêu chí: Hỗ trợ tìm kiếm theo từ khoá, lọc theo actor, actorRole (USER/AGENT/ADMIN), action, entity và khoảng thời gian (from, to).",
        title="NGUYÊN TẮC QUẢN TRỊ AUDIT TRAIL & ACTIVITY LOG"
    )

    audit_api_headers = ["Phương thức", "Endpoint", "Vai trò", "Chức năng"]
    audit_api_data = [
        ["GET", "/api/audit-logs", "ADMIN", "Lấy danh sách nhật ký Audit Trail (phân trang, lọc theo keyword, actor, role, action, thời gian)"],
        ["GET", "/api/audit-logs/{id}", "ADMIN", "Xem thông tin chi tiết một bản ghi Audit Log theo ID"]
    ]
    add_custom_table(doc, audit_api_headers, audit_api_data, [1.1, 2.5, 1.1, 1.8])

    p_audit_res = doc.add_paragraph()
    p_audit_res.add_run("Ví dụ Response Danh sách Audit Log (HTTP 200 OK - GET /api/audit-logs):").bold = True
    sample_audit_res = """{
  "content": [
    {
      "id": 101,
      "actor": "operator1@gmail.com",
      "actorRole": "USER",
      "action": "GET /api/internal/user-files",
      "entity": "InternalUserSvgFile",
      "resource": "InternalUserSvgFile",
      "entityId": null,
      "details": "HTTP 200 (15ms) | Path: /api/internal/user-files | IP: 192.168.1.10 | Device: Graphtec-FC9000-140 | Handler: InternalUserSvgFileController.listMyFiles",
      "timestamp": "2026-10-07T00:30:00"
    },
    {
      "id": 102,
      "actor": "agent_hanoi@gmail.com",
      "actorRole": "AGENT",
      "action": "GET /api/users",
      "entity": "User",
      "resource": "User",
      "entityId": null,
      "details": "HTTP 200 (18ms) | Path: /api/users | IP: 118.69.15.22 | Handler: UserController.getUsers",
      "timestamp": "2026-10-07T00:32:15"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1,
  "first": true,
  "last": true
}"""
    add_code_block(doc, sample_audit_res)

    doc.add_page_break()

    # ==================== CHƯƠNG 8: BẢNG MÃ LỖI & BEST PRACTICES ====================
    add_header_styled(doc, "8. Bảng Mã Lỗi Nghiệp Vụ & Khuyến Nghị Tích Hợp", level=1)

    add_header_styled(doc, "8.1. Bảng Tra Cứu Toàn Bộ Mã Lỗi Hệ Thống (Business Error Codes)", level=2)
    err_headers = ["HTTP Status", "Mã Lỗi (Error Code)", "Mô tả nguyên nhân & Cách khắc phục"]
    err_data = [
        ["400 Bad Request", "VALIDATION_FAILED", "Dữ liệu gửi lên thiếu các trường bắt buộc hoặc sai định dạng."],
        ["400 Bad Request", "YEAR_RANGE_INVALID", "Năm sản xuất từ (yearFrom) lớn hơn năm đến (yearTo) trong cấu hình xe."],
        ["400 Bad Request", "CURRENT_PASSWORD_INCORRECT", "Mật khẩu hiện tại không chính xác khi thực hiện đổi mật khẩu."],
        ["400 Bad Request", "DEVICE_REQUIRED", "Yêu cầu từ ứng dụng máy cắt thiếu thông số định danh thiết bị."],
        ["400 Bad Request", "ONE_VEHICLE_PER_FILE", "Upload kho part file gửi nhiều hơn một mẫu xe (chỉ chấp nhận 1 file - 1 mẫu xe)."],
        ["400 Bad Request", "UNSUPPORTED_FORMAT", "Định dạng file tải lên không phải là SVG."],
        ["400 Bad Request", "SVG_UNITS_MISSING", "File SVG không khai báo đơn vị hợp lệ (width/height và viewBox)."],
        ["400 Bad Request", "CANNOT_SHARE_TO_SELF", "Không thể tự chia sẻ bản lưu SVG cho chính tài khoản của mình."],
        ["400 Bad Request", "INVALID_SHARE_TARGET", "Người dùng nhận quyền chia sẻ không hợp lệ hoặc không tồn tại."],
        ["400 Bad Request", "SVG_SECURITY_VALIDATION_FAILED", "File SVG vi phạm chính sách an toàn (chứa mã độc, script độc hại, XXE, SSRF, active tag)."],
        ["400 Bad Request", "SVG_SIZE_LIMIT_EXCEEDED", "File SVG vượt quá dung lượng tối đa (20MB) hoặc số phần tử/độ sâu XML cho phép."],
        ["400 Bad Request", "SVG_MALWARE_DETECTED", "Phát hiện cấu trúc nhị phân giả mạo hoặc chữ ký mã độc trong file SVG."],
        ["401 Unauthorized", "INVALID_CREDENTIALS", "Tài khoản hoặc mật khẩu không chính xác, hoặc người dùng bị vô hiệu hóa."],
        ["401 Unauthorized", "TOKEN_EXPIRED", "JWT Access Token hoặc Refresh Token đã hết hạn. Cần gọi API refresh-token hoặc login lại."],
        ["401 Unauthorized", "USER_ACCOUNT_EXPIRED", "Tài khoản người dùng đã hết hạn sử dụng theo thiết lập expirationDate."],
        ["401 Unauthorized", "SESSION_REVOKED", "Thiết bị máy cắt đã bị quản trị viên gỡ khỏi tài khoản hoặc phiên đã chấm dứt."],
        ["403 Forbidden", "ACCESS_DENIED", "Tài khoản không đủ quyền hạn (Role) để gọi endpoint quản trị."],
        ["403 Forbidden", "DOWNLOAD_PERMISSION_DENIED", "Đại lý của tài khoản chỉ được cấp quyền VIEW, không có quyền DOWNLOAD file SVG."],
        ["403 Forbidden", "USER_WEB_LOGIN_FORBIDDEN", "Tài khoản thợ máy cắt (USER) không được phép đăng nhập trang Web Quản trị."],
        ["403 Forbidden", "SESSION_LIMIT", "Tài khoản đã đăng nhập đủ số máy cắt tối đa cho phép (F-57)."],
        ["403 Forbidden", "USER_FILE_SHARE_FORBIDDEN", "Không có quyền chia sẻ file SVG này (chỉ Owner hoặc ADMIN mới có quyền)."],
        ["404 Not Found", "RESOURCE_NOT_FOUND", "Không tìm thấy cấu hình xe, đại lý hoặc tài nguyên yêu cầu."],
        ["404 Not Found", "USER_FILE_NOT_FOUND", "Không tìm thấy bản lưu file SVG của người dùng."],
        ["404 Not Found", "USER_FILE_SHARE_NOT_FOUND", "Không tìm thấy lượt chia sẻ file SVG tương ứng."],
        ["409 Conflict", "CONFIG_ALREADY_EXISTS", "Cấu hình xe cho nhóm sản phẩm, hãng, dòng, năm và mã đời này đã tồn tại."],
        ["409 Conflict", "CONFIG_IN_USE", "Cấu hình xe đang được gán cho các file SVG trong hệ thống, không thể xóa."],
        ["409 Conflict", "USER_FILE_SHARE_ALREADY_EXISTS", "File SVG đã được chia sẻ cho người dùng này trước đó."],
        ["429 Too Many Requests", "RATE_LIMIT_EXCEEDED", "Tần suất gửi request vượt quá giới hạn an toàn của hệ thống."]
    ]
    add_custom_table(doc, err_headers, err_data, [1.4, 2.3, 2.8])

    add_header_styled(doc, "8.2. Khuyến nghị Tích hợp cho Lập trình viên Client Máy Cắt", level=2)
    p = doc.add_paragraph()
    p.add_run(
        "1. Quản lý Vòng đời Token & Ngày Hết Hạn Tài Khoản:\n"
        "   • Sau khi login thành công, lưu trữ an toàn `accessToken` trong bộ nhớ và `refreshToken` trong kho lưu trữ bảo mật của hệ điều hành.\n"
        "   • Kiểm tra trường `expirationDate` và `expired` trong đối tượng User. Nếu tài khoản sắp hết hạn, hiển thị thông báo nhắc nhở người dùng liên hệ đại lý gia hạn.\n"
        "   • Chủ động gọi `POST /api/internal/auth/refresh-token` trước khi access token hết hạn 5 phút. Khi gặp HTTP 401 với mã USER_ACCOUNT_EXPIRED, hiển thị thông báo tài khoản hết hạn và điều hướng về màn hình Đăng nhập.\n\n"
        "2. Nâng cấp Sang Internal API V2 Mã Hoá AES-256-GCM:\n"
        "   • Khuyến nghị các ứng dụng máy cắt chuyển đổi sang sử dụng endpoint `/api/internal/v2/user-files` để tận hưởng khả năng bảo mật mã hoá AES-256-GCM.\n"
        "   • Khi tải và hiển thị file SVG, ứng dụng client máy cắt được bảo vệ hoàn toàn khỏi các mã độc XSS/XXE nhờ cơ chế kiểm duyệt nghiêm ngặt và giải mã trong suốt.\n\n"
        "3. Luôn truyền Định danh Thiết bị & IP Client:\n"
        "   • Luôn cung cấp tên máy cắt và IP client trong các payload POST, hoặc gửi kèm trong HTTP Headers (`X-Device` và `X-Forwarded-For`). Điều này giúp theo dõi lịch sử cắt và hỗ trợ kỹ thuật kịp thời khi có sự cố.\n\n"
        "4. Phân Biệt Rõ Ràng Hai Loại Danh Mục:\n"
        "   • Không nhầm lẫn giữa 'Danh mục cấu hình xe' và 'Danh mục kho mẫu & part' (`/api/part-library-categories`). Khi xây dựng bộ lọc mẫu part trên ứng dụng client, luôn gọi endpoint `/api/part-library-categories/active`."
    )

    output_path = "/Users/phuvk/IdeaProjects/cutting-admin/Tai_Lieu_Tich_Hop_API_He_Thong.docx"
    doc.save(output_path)
    print(f"Document successfully created at: {output_path}")

if __name__ == "__main__":
    create_document()
