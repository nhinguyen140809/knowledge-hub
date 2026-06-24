# Thuật ngữ (Glossary)

**Bối cảnh & đối tượng**

| Thuật ngữ | Giải thích |
|---|---|
| **AI Agent** | Chương trình dùng LLM kết hợp với các công cụ (tool) để tự lập kế hoạch và thực hiện tác vụ; trong tài liệu này là bên sử dụng tri thức của hệ thống. |
| **SDLC** (Software Development Life Cycle) | Vòng đời phát triển phần mềm, từ phân tích, thiết kế, lập trình, kiểm thử đến bảo trì. |
| **SRS** (Software Requirements Specification) | Tài liệu đặc tả yêu cầu phần mềm. |
| **Agentic search** | Cách agent tự tìm kiếm qua nhiều bước bằng công cụ (đọc, grep file), thay cho một lần truy xuất RAG. |

**Phạm vi & nguồn dữ liệu**

| Thuật ngữ | Giải thích |
|---|---|
| **Product (dự án / sản phẩm)** | Sản phẩm phần mềm mà một hub được scope tới; gồm nhiều source liên quan (source mã nguồn, source tài liệu). Là phạm vi tri thức của một hub. |
| **Source (nguồn)** | Một nguồn dữ liệu được cấu hình để nạp vào hub: một Git repository hoặc một thư mục filesystem, chứa mã nguồn hoặc tài liệu. |
| **Repository (repo)** | Một loại source dạng kho Git (có version control). Một product có thể gồm nhiều repo. |
| **Connector** | Thành phần kết nối tới một nguồn dữ liệu cụ thể, ví dụ Git hoặc hệ thống tài liệu. |
| **Filesystem (fs) connector** | Connector nạp dữ liệu từ một thư mục local, dùng cho nguồn không có Git; versioning dựa trên content hash thay vì commit. |
| **Default branch** | Branch chính của một repo, thường là `main`. |
| **ref** | Tham chiếu tới một phiên bản mã nguồn cụ thể: tên branch hoặc commit. |
| **commit_sha** | Mã băm định danh duy nhất của một commit Git. |
| **Canonical** | Phiên bản tri thức chính thức, dùng chung của team; mặc định lấy từ default branch. |
| **Multi-tenancy** | Khả năng phục vụ nhiều tổ chức hoặc team tách biệt trên cùng một hệ thống. |

**Nạp & xử lý dữ liệu**

| Thuật ngữ | Giải thích |
|---|---|
| **Ingestion** | Quá trình nạp dữ liệu nguồn vào hệ thống: thu thập, xử lý rồi lưu trữ. |
| **Chunk / Chunking** | Việc chia dữ liệu lớn thành các đoạn nhỏ trước khi sinh embedding. |
| **AST** (Abstract Syntax Tree) | Cây cú pháp của mã nguồn, dùng để chunk code theo cấu trúc thay vì cắt thô. |
| **Vector Embedding** (embedding) | Biểu diễn dữ liệu (văn bản, mã nguồn) dưới dạng vector số thực, cho phép so khớp theo ngữ nghĩa. |
| **Embedding provider** | Nguồn sinh embedding, có thể là API bên ngoài hoặc local model. |
| **Local model** | Mô hình chạy cục bộ trong hạ tầng của team, giúp dữ liệu không rời mạng nội bộ. |

**Biểu diễn & lưu trữ tri thức**

| Thuật ngữ | Giải thích |
|---|---|
| **Knowledge Graph** | Đồ thị tri thức gồm các entity (node) và quan hệ (edge) giữa chúng. |
| **Entity** | Một thực thể trong Knowledge Graph, ví dụ hàm, lớp, file hoặc một yêu cầu trong tài liệu đặc tả. |
| **GraphRAG** | Biến thể của RAG, kết hợp Knowledge Graph với truy xuất vector để khai thác thêm quan hệ giữa các entity. |
| **Vector store** | Thành phần lưu trữ và tìm kiếm vector embedding, ví dụ Neo4j hoặc Qdrant. |

**Truy xuất (Retrieval)**

| Thuật ngữ | Giải thích |
|---|---|
| **RAG** (Retrieval-Augmented Generation) | Kỹ thuật truy xuất dữ liệu liên quan rồi đưa vào ngữ cảnh của LLM để sinh câu trả lời chính xác hơn. |
| **Semantic search** (vector search) | Tìm kiếm theo độ tương đồng ngữ nghĩa, dựa trên khoảng cách giữa các embedding. |
| **Keyword search** | Tìm kiếm theo độ khớp từ khóa trên văn bản, ví dụ thuật toán BM25. |
| **Hybrid search** (hybrid retrieval) | Kết hợp nhiều phương pháp truy xuất (vector, keyword, graph) rồi hợp nhất kết quả. |
| **Graph traversal** | Duyệt theo các quan hệ trong Knowledge Graph để mở rộng ngữ cảnh truy xuất. |
| **Fusion** | Hợp nhất kết quả từ nhiều phương pháp truy xuất thành một bảng xếp hạng (ví dụ RRF). |
| **Relevance score** | Điểm thể hiện mức độ liên quan của một kết quả đối với truy vấn. |
| **top-k** | Số lượng kết quả liên quan nhất được trả về cho một truy vấn. |
| **Metadata** | Dữ liệu mô tả đi kèm mỗi kết quả, ví dụ đường dẫn file, ngôn ngữ, vị trí dòng. |

**Đồng bộ & cập nhật**

| Thuật ngữ | Giải thích |
|---|---|
| **Content hash / Dedup** | Giá trị băm theo nội dung của một chunk, dùng để khử trùng lặp và phát hiện thay đổi. |
| **Incremental update** | Cập nhật chỉ phần dữ liệu đã thay đổi, không re-index toàn bộ. |
| **Eviction** | Gỡ bỏ tri thức lỗi thời khỏi chỉ mục khi nguồn bị xóa hoặc thay đổi. |
| **Webhook** | Cơ chế để nguồn dữ liệu chủ động báo cho hệ thống khi có thay đổi (ví dụ commit mới). |

**Giao tiếp & kiến trúc**

| Thuật ngữ | Giải thích |
|---|---|
| **Core** | Tầng nghiệp vụ trung tâm chứa logic xử lý và truy xuất|
| **MCP** (Model Context Protocol) | Giao thức chuẩn cho phép AI Agent kết nối tới dữ liệu và công cụ bên ngoài qua một interface thống nhất. |
| **REST API** | Giao diện lập trình theo kiến trúc REST, giao tiếp qua HTTP. |
| **OpenAPI** | Chuẩn mô tả REST API (request/response), dùng để sinh tài liệu API. |
| **Docker** | Nền tảng đóng gói ứng dụng thành container để triển khai nhất quán giữa các môi trường. |
| **Runtime** | Thời điểm hệ thống đang chạy và phục vụ truy vấn. |

**Bảo mật & phân quyền**

| Thuật ngữ | Giải thích |
|---|---|
| **Access Control List (ACL)** | Tập quy tắc xác định principal nào được thực hiện thao tác nào trên tài nguyên nào. |
| **Principal** | Chủ thể được định danh và phân quyền khi gọi hệ thống: một người, một service, hoặc một nhóm. |
| **Grant** | Một quy tắc trong ACL cấp một quyền cụ thể cho một principal trên một tập tài nguyên. |
| **Default policy** | Quy tắc mặc định áp dụng khi không có grant tường minh nào khớp, thường là `deny` (từ chối) hoặc `allow` (cho phép). |