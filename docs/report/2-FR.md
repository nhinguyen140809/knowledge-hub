## Functional requirements

### FR-1 — Thu thập dữ liệu (Data ingestion)

- **FR-1.1** Hệ thống phải thu thập được dữ liệu từ mã nguồn (source code), tài liệu (SRS, v.v), và git log.
- **FR-1.2** Hệ thống phải đọc và ingest được các định dạng tài liệu phổ biến: Markdown, PDF và ảnh (trích xuất văn bản từ ảnh).
- **FR-1.3** Hệ thống phải nạp dữ liệu từ các source được cấu hình, mỗi source thuộc một trong hai loại: (a) một Git repository (qua remote URL hoặc đường dẫn tới một repo trên đĩa) tại một ref cấu hình được, mặc định là default branch; và (b) một thư mục filesystem không yêu cầu version control. Source loại (a) cung cấp thêm `ref` và `commit_sha` cho provenance (FR-1.4); loại (b) định danh phiên bản qua `content_hash`.
- **FR-1.4** Hệ thống phải gắn metadata nguồn gốc cho mỗi đơn vị dữ liệu được nạp. Provenance bắt buộc gồm `source_id`, đường dẫn file, `content_hash` và thời điểm index (`indexed_at`); riêng nguồn Git bổ sung `ref` (branch) và `commit_sha`.
- **FR-1.5** Hệ thống phải cho phép cấu hình danh sách nguồn cần nạp, bao gồm việc loại trừ (ignore) các file hoặc thư mục không cần index.

### FR-2 — Xử lý & lập chỉ mục (Processing & indexing)

- **FR-2.1** Hệ thống phải chia dữ liệu thành các chunk trước khi sinh embedding: chunk mã nguồn theo cấu trúc AST (hàm, lớp), chunk tài liệu theo cấu trúc văn bản (heading, đoạn).
- **FR-2.2** Hệ thống phải sinh embedding cho từng chunk thông qua embedding provider đang được cấu hình.
- **FR-2.3** Hệ thống phải xây dựng Knowledge Graph gồm các entity và quan hệ giữa chúng (theo FR-3). Entity gồm: các entity mã nguồn theo các mức ở FR-3.4, cùng các entity phi mã nguồn là tài liệu, yêu cầu (SRS) và commit.
- **FR-2.4** Hệ thống phải lưu trữ đồng thời vector embedding, Knowledge Graph và metadata, sao cho mỗi chunk truy ngược được về nguồn gốc theo FR-1.4.

### FR-3 — Liên kết tri thức (Knowledge linking)

- **FR-3.1** Hệ thống phải tạo được các quan hệ cấu trúc và phụ thuộc trong mã nguồn, bao gồm: chứa/khai báo (CONTAINS/DECLARES), gọi hàm (CALLS), nhập phụ thuộc (IMPORTS), kế thừa (EXTENDS), hiện thực interface (IMPLEMENTS) và ghi đè (OVERRIDES).
- **FR-3.2** Hệ thống có thể bổ sung các quan hệ sâu hơn: khởi tạo (INSTANTIATES), đọc/ghi biến, field hoặc constant (READS/WRITES), tham chiếu một định danh khác mà không gọi hàm (REFERENCES), dùng kiểu (HAS_TYPE), annotation (ANNOTATED_WITH), ném ngoại lệ (THROWS), liên kết test (TESTS).
- **FR-3.3** Hệ thống phải liên kết giữa các artifact khác nhau, gồm: tài liệu mô tả code (DESCRIBES), yêu cầu được hiện thực bởi code (IMPLEMENTED_BY), yêu cầu được kiểm bởi test (VERIFIED_BY), commit sửa đổi code (MODIFIES), client gọi tới endpoint/API do code khác cung cấp (CONSUMES), và tài liệu tham chiếu tài liệu khác (LINKS_TO). Việc liên kết dựa trên các tín hiệu: tên định danh (tên hàm, tên lớp), từ khóa, và tham chiếu đường dẫn; mỗi liên kết suy luận theo heuristic phải kèm một điểm tin cậy (confidence score) để phân biệt liên kết chắc chắn với liên kết phỏng đoán.
- **FR-3.4** Hệ thống phải biểu diễn tri thức theo các mức phân cấp, từ mịn đến thô: constant, field, function/method, class/interface, file, module, package, project; và quan hệ chứa giữa các mức (CONTAINS, theo FR-3.1).
- **FR-3.5** Hệ thống phải hỗ trợ các quan hệ nối entity thuộc các source khác nhau trong cùng một product (cross-source), áp dụng cho cả quan hệ cấu trúc (IMPORTS tới thư viện dùng chung, CONSUMES tới API của service khác) và quan hệ cross-artifact ở FR-3.3 (DESCRIBES, IMPLEMENTED_BY, VERIFIED_BY nối tài liệu/yêu cầu ở một source tới code ở source khác).

### FR-4 — Truy xuất (Retrieval)

- **FR-4.1** Hệ thống phải nhận truy vấn ở dạng văn bản ngôn ngữ tự nhiên (free-text) và dùng làm đầu vào cho truy xuất: embedding hóa để semantic search và tách từ khóa cho keyword search (FR-4.2). Hệ thống không tự hiểu hay diễn giải ý định ngôn ngữ -- đó là vai trò của agent.
- **FR-4.2** Hệ thống phải thực hiện hybrid search, kết hợp semantic search, keyword search và graph traversal, rồi hợp nhất (fusion) thành một danh sách kết quả.
- **FR-4.3** Hệ thống phải trả kết quả dưới dạng JSON có cấu trúc, mỗi kết quả kèm relevance score và metadata (nguồn, đường dẫn file, vị trí dòng code, ref/phiên bản).
- **FR-4.4** Hệ thống phải hỗ trợ các tham số truy vấn tùy chọn, bao gồm: giới hạn số kết quả (top-k) và lọc theo ref hoặc loại dữ liệu.
- **FR-4.5** Khi truy vấn tham chiếu tới một ref chưa được index, hệ thống phải trả kết quả theo ref canonical và chỉ rõ điều này trong metadata.

### FR-5 — API & MCP

- **FR-5.1** Hệ thống phải cung cấp REST API cho cả thao tác truy vấn và thao tác quản trị (nạp, cập nhật, xóa nguồn và kiểm tra trạng thái).
- **FR-5.2** Hệ thống phải cung cấp một MCP server expose hai tool: một tool truy vấn tri thức và một tool kích hoạt cập nhật/đồng bộ, để AI Agent vừa tích hợp truy vấn vừa chủ động cập nhật trực tiếp.
- **FR-5.3** Hệ thống phải cung cấp tài liệu API mô tả request, response và ví dụ, đủ để agent hiểu cách dùng và diễn giải kết quả.
- **FR-5.4** Mọi lỗi API phải trả về theo một schema thống nhất gồm mã lỗi và thông điệp.

### FR-6 — Đồng bộ & cập nhật (Sync & update)

- **FR-6.1** Việc cập nhật tri thức là trigger-based: hệ thống phải cung cấp thao tác cập nhật/đồng bộ qua REST API và MCP tool để agent hoặc caller chủ động kích hoạt khi nguồn thay đổi. Thao tác này phải idempotent (kích hoạt lại khi nội dung không đổi là một no-op).
- **FR-6.2** Khi được kích hoạt, hệ thống phải tự động nhận diện file mới, sửa đổi hoặc xóa và chỉ xử lý phần thay đổi (incremental), thay vì re-index toàn bộ.
- **FR-6.3** Hệ thống phải tránh xử lý trùng lặp bằng dedup theo content hash: chunk có nội dung không đổi thì không sinh lại embedding.
- **FR-6.4** Hệ thống phải gỡ bỏ (evict) tri thức lỗi thời khi nguồn tương ứng bị xóa hoặc thay đổi, giữ chỉ mục nhất quán với nguồn.
- **FR-6.5** Hệ thống phải cho biết độ mới của tri thức (qua `indexed_at` và `commit_sha`) trong kết quả truy vấn hoặc endpoint trạng thái, để agent quyết định có cần kích hoạt cập nhật trước khi truy vấn.
- **FR-6.6** Hệ thống có thể hỗ trợ thêm cơ chế tự kích hoạt cập nhật như tùy chọn (webhook khi có commit/merge, hoặc quét định kỳ), mặc định tắt.

### FR-7 — Cấu hình (Configuration)

- **FR-7.1** Hệ thống phải cho phép chọn embedding provider qua cấu hình (gọi API bên ngoài hoặc dùng local model).
- **FR-7.2** Hệ thống phải cho phép cấu hình backend lưu trữ vector, cho phép bổ sung vector store database khi mở rộng.
- **FR-7.3** Hệ thống phải hỗ trợ cấu hình theo nhiều môi trường (dev, server, cloud).

### FR-8 — Truy cập đồng thời & bảo mật (Concurrency & access)

Các yêu cầu FR-8.2 - 8.13 dựa trên mô hình hai bước: 
- **authentication** resolve credential của request về một principal (`credential` $\rightarrow$ `principal_id`); 
- **authorization** tính tập source mà principal được đọc rồi filter (`principal` $\rightarrow$ `sources`). 

Một principal không bắt buộc là một người - có thể là một người, một service, hoặc một định danh theo nhóm chức năng; độ mịn của định danh do cách cấp phát credential quyết định (một credential cho mỗi vai trò, hoặc một credential cho mỗi người), không do cơ chế ép.

- **FR-8.1** Hệ thống phải phục vụ nhiều AI Agent truy vấn đồng thời trên cùng một kho tri thức dùng chung và trả kết quả nhất quán.
- **FR-8.2** Hệ thống phải xác thực (authenticate) các request tới REST API và MCP, trước khi cho phép truy cập tri thức.
- **FR-8.3** Hệ thống phải kiểm soát truy cập tri thức ở mức source thông qua một ACL do admin cấu hình, xác định principal nào được đọc (`read`) tri thức từ source nào. Đơn vị phân quyền là source (`source_id`).
- **FR-8.4** ACL phải gồm các thành phần:
  - **Principal registry** -- mỗi principal gồm: `principal_id`, `type` (`subject` | `group`; `subject` là một người, một service, hoặc một định danh dùng chung theo nhóm chức năng như `backend`), `members` (danh sách `principal_id`, chỉ với `group`), `credentials` (danh sách `credential_id` gắn với principal, không lưu giá trị secret thô) và `role` (`admin` | `member`).
  - **Grants** — mỗi grant gồm: `principal_id`, danh sách `source_id` áp dụng, và `permission` = `read`.
  - **default_policy** — `deny` | `allow`.
- **FR-8.5** Trên mỗi request, hệ thống phải resolve credential đã xác thực (FR-8.2) về đúng một principal. Quyền hiệu lực của một principal là hợp (union) quyền của chính nó và mọi `group` chứa nó (đệ quy).
- **FR-8.6** Hệ thống phải áp ACL như một filter cứng trước khi kết quả rời hệ thống, trên tất cả đường truy xuất: semantic search, keyword search, graph traversal và fusion. Kết quả, node/quan hệ kề trong graph, và metadata có nguồn gốc từ source mà principal không được đọc không được xuất hiện trong phản hồi.
- **FR-8.7** Quy tắc quyết định quyền đọc một source: dưới `default_policy = deny`, principal chỉ đọc source được grant tường minh; dưới `default_policy = allow`, principal đọc mọi source trừ source xuất hiện trong bất kỳ grant nào (source đó trở thành restricted, chỉ principal được grant mới đọc được).
- **FR-8.8** Hệ thống phải fail-closed: khi không resolve được principal, hoặc khi không xác định được quyền, request bị từ chối.
- **FR-8.9** Admin có thể tạo/sửa/xóa principal, group, grant và `default_policy` qua REST API.
- **FR-8.10** Admin phải phát hành (issue) và thu hồi (revoke) credential gắn với một principal qua REST API; một principal có thể có nhiều credential.
- **FR-8.11** Giá trị secret của credential chỉ hiển thị một lần lúc phát hành và lưu ở dạng không thể khôi phục.
- **FR-8.12** Credential bị thu hồi phải bị từ chối ngay từ request kế tiếp.
- **FR-8.13** Các thao tác quản trị — quản lý principal/group/grant/`default_policy` (FR-8.9), cấp/thu hồi credential (FR-8.10), và cấu hình nguồn dữ liệu (FR-1.5, FR-5.1) — chỉ principal có `role = admin` mới được thực hiện; request không đủ quyền bị từ chối.