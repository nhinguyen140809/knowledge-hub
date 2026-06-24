# Introduction

Knowledge Hub là một dịch vụ độc lập, đóng vai trò long-term memory và context provider cho các AI Agent trong SDLC của một team.

Hệ thống thu thập, cấu trúc hóa và truy xuất tri thức hỗn hợp của dự án/sản phẩm mà team sở hữu, bao gồm mã nguồn, tài liệu, git log và SRS. Tri thức được biểu diễn bằng cách kết hợp Vector Embedding và Knowledge Graph (GraphRAG), nhằm hỗ trợ đồng thời semantic search và graph traversal trên quan hệ giữa các entity.

Tri thức được cung cấp qua hai interface với vai trò khác nhau: MCP là đường chính để các AI Agent của team truy vấn bằng ngôn ngữ tự nhiên và kích hoạt cập nhật, nhận về kết quả có cấu trúc kèm relevance score và metadata -- thay vì phải tự khám phá lại tri thức của dự án trong mỗi phiên làm việc; REST API phục vụ quản trị/vận hành và các caller không đi qua agent.

## Scope of work

**Phạm vi của dự án**

- Thu thập và xử lý các loại dữ liệu của dự án, bao gồm mã nguồn, tài liệu, git log và SRS.
- Xây dựng pipeline ingestion: chia dữ liệu thành các chunk (theo AST đối với mã nguồn, theo cấu trúc đối với tài liệu), sinh embedding, rồi xây dựng Knowledge Graph biểu diễn quan hệ giữa các entity (ví dụ: một hàm gọi một hàm khác, một yêu cầu được hiện thực bởi một đoạn mã).
- Hỗ trợ hybrid search, kết hợp semantic search với keyword search và graph traversal; trả kết quả dưới dạng JSON kèm relevance score và metadata.
- Đồng bộ và cập nhật tri thức khi dữ liệu nguồn thay đổi.
- Cung cấp tri thức qua REST API và MCP; đóng gói bằng Docker và hỗ trợ cấu hình theo nhiều môi trường (dev, server, cloud).
- Cho phép nhiều AI Agent trong cùng một team truy cập đồng thời vào một kho tri thức dùng chung.
- Xác thực mọi request (authn) và kiểm soát truy cập tri thức ở mức source qua ACL do admin cấu hình (principal, grant, credential).
- Cho phép cấu hình embedding provider theo hai chế độ: gọi API bên ngoài hoặc dùng local model để giữ dữ liệu trong mạng nội bộ.

**Ngoài phạm vi của dự án**

- Hệ thống không tự xây dựng AI Agent; agent là bên sử dụng tri thức và được minh họa bằng các agent có sẵn (ví dụ Claude Code, Cursor).
- Hệ thống không cung cấp giao diện đồ họa (UI); tri thức chỉ được truy vấn qua agent thông qua MCP và REST, còn việc vận hành và cấu hình được thực hiện qua file cấu hình và Docker.
- Hệ thống không hướng tới quy mô enterprise, tức không hỗ trợ hàng trăm connector và không index toàn bộ tổ chức.
- Hệ thống không hỗ trợ multi-tenancy; phân quyền giới hạn ở mức source qua ACL cấu hình được, không tới mức tài liệu hay chunk; phạm vi là một team trên một product.
- Hệ thống không nhận danh tính người dùng cuối do agent forward (identity propagation); tại runtime hub chỉ thấy principal gắn với credential được dùng.

## Stakeholders

- **AI Agent (bên sử dụng chính):** các agent phục vụ những tác vụ khác nhau trong SDLC (ví dụ viết mã, kiểm thử, tạo tài liệu). Agent truy vấn Knowledge Hub qua MCP để lấy ngữ cảnh, và là tác nhân duy nhất tương tác trực tiếp với tri thức tại runtime.
- **Developer (thành viên team):** vừa là chủ sở hữu dữ liệu nguồn (mã nguồn, tài liệu) được đưa vào hệ thống, vừa là người sử dụng tri thức một cách gián tiếp thông qua agent của mình.
- **Admin / maintainer:** một vai trò vận hành, có thể do một developer đảm nhiệm, chịu trách nhiệm cấu hình nguồn dữ liệu, lựa chọn embedding provider, quản lý phân quyền truy cập và các tham số hệ thống qua file cấu hình và Docker; đồng thời quản lý quá trình ingestion, theo dõi và bảo trì hệ thống.

Đối tượng mục tiêu là một team phát triển phần mềm ở quy mô trung bình. Tri thức được dùng chung cho cả team: dữ liệu chỉ được index một lần và mọi agent cũng như thành viên đều truy vấn trên cùng một kho. Hệ thống không hướng tới developer cá nhân (vốn đã được hỗ trợ tốt bởi agentic search), cũng không hướng tới quy mô enterprise.

## Deliverables

- **Mã nguồn hệ thống** — toàn bộ Knowledge Hub: pipeline ingestion (FR-1), xử lý & lập chỉ mục (FR-2), liên kết tri thức (FR-3), truy xuất hybrid (FR-4), REST API và MCP server (FR-5), đồng bộ & cập nhật (FR-6), cấu hình đa môi trường (FR-7), xác thực & phân quyền ACL (FR-8).
- **Cụm Docker** — Dockerfile cho từng thành phần và một file `docker-compose` khởi chạy toàn hệ thống (ứng dụng + vector store + graph store) bằng một lệnh (NFR-9).
- **Tài liệu thiết kế hệ thống** — kiến trúc RAG/Knowledge Graph, thiết kế database, chiến lược chunking mã nguồn và tài liệu, mô hình entity/quan hệ và thiết kế API.
- **Tài liệu API** — mô tả OpenAPI cho REST kèm ví dụ request/response (FR-5.3, NFR-8.1) và mô tả các MCP tool (FR-5.2).
- **Hướng dẫn build & deploy** — cấu hình môi trường, chọn embedding provider và vector store, nạp dữ liệu mẫu ban đầu, và chạy hệ thống (NFR-9, NFR-10).
- **Bộ đánh giá** — gold set ≥ 50 truy vấn có nhãn cùng script đo Recall@10, MRR và latency (NFR-1, NFR-2); và bộ test kiểm chứng phân quyền không rò rỉ (NFR-7.4).
- **Demo** — kịch bản hoặc video minh họa một AI Agent đặt câu hỏi về dự án và hub truy xuất ra đúng ngữ cảnh (mã nguồn/tài liệu) liên quan.