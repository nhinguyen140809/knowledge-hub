# Thiết kế API

Tri thức được cung cấp qua hai giao diện với vai trò phân định rõ: **MCP** là đường chính để AI Agent truy vấn và chủ động kích hoạt cập nhật ngay trong phiên làm việc; **REST API** phục vụ quản trị, vận hành và các caller không đi qua agent (FR-5.1, FR-5.2). Cả hai đi qua cùng một lớp nghiệp vụ bên dưới — cùng luật phân quyền, cùng ngữ nghĩa truy vấn — nên agent và quản trị viên luôn nhìn thấy một hệ thống nhất quán.

## Giao diện MCP cho agent

MCP server công bố các tool để agent tự khám phá và gọi bằng ngôn ngữ tự nhiên của nó:

- **Tool liệt kê nguồn** — cho agent khám phá các source nó được phép đọc, mỗi source kèm id, loại (git hay filesystem) và ref cấu hình. Tập trả về chính là tập mà mọi truy vấn bị giới hạn vào, nên tool không lộ gì mà truy vấn không thể lộ; agent mới kết nối gọi tool này trước tiên để biết mình có thể hỏi về những gì.
- **Tool truy vấn tri thức** — nhận câu truy vấn văn bản tự do cùng các tham số tùy chọn (số kết quả tối đa, khoanh theo source, khoanh theo ref, khoanh theo loại dữ liệu), trả về danh sách kết quả xếp hạng như mô tả ở chương *Truy xuất hybrid*. Mô tả tool nói rõ cho agent biết mỗi kết quả kèm điểm tin cậy và metadata trích dẫn, và kết quả đã được giới hạn trong phạm vi nguồn agent được đọc.
- **Tool đồng bộ nguồn** — cho agent chủ động kích hoạt cập nhật một nguồn khi nghi ngờ tri thức đã cũ (ví dụ sau khi chính nó vừa sửa mã), thay vì phải chờ quản trị viên (FR-6.1). Kết quả trả về gồm số file được index/re-index/gỡ bỏ và số commit mới được nạp vào đồ thị.
- **Tool trạng thái nguồn** — cho agent kiểm tra độ mới của một nguồn: đã từng đồng bộ chưa, lần đồng bộ gần nhất, và (với nguồn git) ref/commit đã index — để quyết định có cần đồng bộ trước lúc hỏi (FR-6.5). Tool này chỉ đọc, mở cho mọi agent đã xác thực.
- **Tool thông tin hệ thống** — cho agent xem thông tin vận hành runtime của hệ thống.

Các tool này ghép thành vòng làm việc tự nhiên của agent: *khám phá nguồn → kiểm tra độ mới → (nếu cần) đồng bộ → truy vấn → trích dẫn*.

## Giao diện REST

REST API có tiền tố phiên bản (`/api/v1`) và chia theo nhóm tài nguyên:

- **Truy vấn** — `POST /query`: nhận cùng cấu trúc truy vấn với tool MCP, cho các caller không đi qua agent.
- **Quản trị nguồn dữ liệu** — đăng ký, liệt kê, xem, sửa cấu hình và xóa nguồn dưới `/admin/sources`; kích hoạt đồng bộ (`POST .../sync`) và xem trạng thái độ mới (`GET .../status`) cho từng nguồn.
- **Quản trị phân quyền** — quản lý principal (kèm thành viên nhóm và credential), grant, và chính sách mặc định dưới `/admin/...`; đủ để quản trị viên vận hành trọn vòng đời ACL qua API (FR-8.9, FR-8.10).
- **Thông tin hệ thống** — `GET /system/info` cho thông tin vận hành runtime.

Các thao tác quản trị đòi hỏi vai trò admin; mọi endpoint — kể cả truy vấn — đều yêu cầu xác thực bằng credential đã phát hành (FR-8.2, NFR-7.1).

Ngữ nghĩa cập nhật được chọn có chủ đích ở những chỗ dễ nhầm. Sửa cấu hình nguồn là **cập nhật một phần** (partial update): trường không gửi thì giữ nguyên giá trị hiện tại, mảng rỗng nghĩa là xóa danh sách, mảng có phần tử là thay thế toàn bộ. *Ví dụ:* gửi `{"ignore": ["build"]}` chỉ thay danh sách loại trừ, còn ref và danh sách include giữ nguyên — người gọi không phải chép lại toàn bộ cấu hình chỉ để sửa một trường.

## Cấu trúc kết quả truy vấn

Kết quả trả về là JSON có cấu trúc, mỗi mục kèm relevance score và metadata (FR-4.3):

```json
{
  "hits": [
    {
      "id": "c7f3…",
      "relevanceScore": 0.93,
      "metadata": {
        "kind": "chunk",
        "sourceId": "docs-service",
        "path": "src/reader/PdfReader.java",
        "lineStart": 40,
        "lineEnd": 72,
        "type": "code",
        "ref": "main",
        "commitSha": "8a41…",
        "indexedAt": "2026-07-01T09:30:00Z",
        "viaPath": []
      }
    },
    {
      "id": "e91b…",
      "relevanceScore": 0.71,
      "metadata": {
        "kind": "chunk",
        "sourceId": "docs-service",
        "path": "docs/architecture.md",
        "type": "doc",
        "viaPath": ["DESCRIBES"]
      }
    }
  ],
  "servedFromCanonicalRef": false
}
```

Cấu trúc này cho agent ba thứ nó cần: **xếp hạng** để chọn ngữ cảnh, **tọa độ trích dẫn** (nguồn, file, dòng, phiên bản) để dẫn nguồn và tự lấy lại nội dung, và **dấu vết suy luận** (`viaPath` — chuỗi quan hệ đã duyệt) cho kết quả đến từ đường đồ thị. Cờ `servedFromCanonicalRef` báo khi kết quả được phục vụ từ ref canonical do ref yêu cầu chưa được index (FR-4.5).

## Mô hình lỗi thống nhất

Mọi lỗi — bất kể từ endpoint nào — trả về theo **một schema duy nhất** (FR-5.4) theo chuẩn RFC 7807, gồm mã HTTP, một **mã lỗi ổn định** dạng máy-đọc-được, thông điệp cho người, và định danh tương quan để tra log:

```json
{
  "title": "Not Found",
  "status": 404,
  "detail": "source docs-service not found",
  "code": "SOURCE_NOT_FOUND",
  "traceId": "5f2c…"
}
```

Vài nguyên tắc nhất quán: lỗi do dữ liệu đầu vào (thiếu trường, sai kiểu, body không đọc được) luôn là lỗi 4xx với mã `VALIDATION_FAILED` và thông điệp chung chung không lộ chi tiết nội bộ; lỗi bất ngờ của hệ thống là 500 với thông điệp trung tính, nguyên nhân thật chỉ nằm trong log — client không bao giờ nhận stack trace hay tên lớp nội bộ. Các tool MCP dùng lại đúng bộ mã lỗi này ở ranh giới của chúng, nên một agent xử lý lỗi theo cùng một cách trên cả hai giao diện.

## Tài liệu hóa

Toàn bộ endpoint REST được mô tả bằng OpenAPI kèm ví dụ request/response (FR-5.3, NFR-8.1), còn mỗi MCP tool tự mang mô tả tham số và hành vi trong phần khai báo của nó — agent đọc mô tả này lúc kết nối để biết cách dùng tool mà không cần tài liệu ngoài.

## Vai trò trong hệ thống

API là hợp đồng công khai của toàn hệ thống: mọi giá trị của pipeline bên dưới đến với agent qua đúng hai cửa này. Thiết kế nhấn ba điểm — *một lớp nghiệp vụ cho cả hai giao diện* (nhất quán), *kết quả đủ tọa độ trích dẫn* (tin cậy được), và *mô hình lỗi thống nhất* (tích hợp dễ) — phục vụ trực tiếp nhóm yêu cầu API & MCP (FR-5) và các yêu cầu xác thực trên mọi endpoint (FR-8.2, NFR-7.1).
