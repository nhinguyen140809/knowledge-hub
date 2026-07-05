# Knowledge Graph và liên kết tri thức

Vector embedding trả lời tốt câu hỏi *"đoạn nào nói về điều này?"*, nhưng bất lực trước câu hỏi *"những gì liên quan đến nhau?"* — hàm nào gọi hàm nào, yêu cầu nào được hiện thực bởi đoạn mã nào, tài liệu nào mô tả thành phần nào. Loại tri thức quan hệ đó được biểu diễn bằng **Knowledge Graph**: các **entity** là node, các quan hệ có tên là cạnh nối giữa chúng (FR-2.3). Chương này trình bày mô hình entity, bộ từ vựng quan hệ, và cách các liên kết được tạo ra — đặc biệt là cách hệ thống phân biệt liên kết *chắc chắn* với liên kết *phỏng đoán*.

## Mô hình entity

Đồ thị chứa hai họ entity:

- **Entity mã nguồn** — các cấu trúc có tên trích từ cây cú pháp: lớp, interface, enum, phương thức, constructor, trường. Chúng xếp thành phân cấp từ mịn đến thô (FR-3.4): một file *khai báo* các kiểu ở mức đỉnh, một kiểu *chứa* các thành viên và kiểu lồng bên trong nó. Mỗi entity mang tên định danh đầy đủ (package, kiểu bao ngoài, chữ ký thành viên) — đây là "địa chỉ" để các tham chiếu từ file khác, thậm chí nguồn khác, tìm về đúng nó.

- **Entity phi mã nguồn** — tài liệu, yêu cầu (mục có mã hiệu trong SRS) và commit. Chúng tồn tại để làm đầu mối cho các quan hệ *liên artifact*: một yêu cầu chỉ có thể "được hiện thực bởi" một đoạn mã nếu bản thân yêu cầu là một node.

Mỗi entity mang định danh ổn định dẫn xuất từ danh tính của nó (xem chương *Quản lý phiên bản tri thức*), nên chỉnh sửa thân một hàm không làm nó thành node mới — các cạnh đã nối vào nó vẫn nguyên vẹn.

## Bộ từ vựng quan hệ

Toàn bộ quan hệ được định nghĩa trước thành một bộ từ vựng đóng, chia ba nhóm theo *độ chắc chắn* của cách chúng được tạo ra:

- **Quan hệ cấu trúc** (FR-3.1) — đọc trực tiếp từ cú pháp, luôn chắc chắn: chứa/khai báo (CONTAINS/DECLARES), gọi hàm (CALLS), nhập phụ thuộc (IMPORTS), kế thừa (EXTENDS), hiện thực interface (IMPLEMENTS), ghi đè (OVERRIDES).

- **Quan hệ sâu** (FR-3.2) — cũng tất định nhưng ở độ mịn cao hơn, được trích trong cùng lượt đọc cú pháp với nhóm cấu trúc: khởi tạo (INSTANTIATES), đọc/ghi trường (READS/WRITES), tham chiếu định danh (REFERENCES), dùng kiểu (HAS_TYPE), annotation (ANNOTATED_WITH), ném ngoại lệ (THROWS), liên kết test (TESTS).

- **Quan hệ liên artifact** (FR-3.3) — nối các loại tri thức khác nhau: tài liệu mô tả mã (DESCRIBES), yêu cầu được hiện thực bởi mã (IMPLEMENTED_BY), yêu cầu được kiểm bởi test (VERIFIED_BY), commit sửa đổi file (MODIFIES), lời gọi API giữa các service (CONSUMES), tài liệu tham chiếu tài liệu (LINKS_TO). Phần lớn các quan hệ này **suy luận theo heuristic** nên bắt buộc kèm một **điểm tin cậy** (confidence score). MODIFIES là ngoại lệ: nó thuộc nhóm này vì nối hai loại artifact, nhưng được đọc trực tiếp từ diff của commit nên tất định và luôn mang độ tin cậy 1.

Ranh giới giữa "chắc chắn" và "phỏng đoán" được mã hóa ngay trong mô hình: quan hệ tất định luôn được ghi với độ tin cậy 1, còn quan hệ suy luận phải khai điểm số của nó. Định nghĩa trước toàn bộ từ vựng — kể cả những quan hệ chưa có bộ sinh tương ứng — giữ cho lược đồ đồ thị và các truy vấn duyệt đồ thị ổn định, bất kể bộ sinh nào đang được bật.

## Trích quan hệ cấu trúc

Với mã nguồn, quan hệ cấu trúc được đọc từ cây cú pháp của từng file: các câu lệnh import, mệnh đề kế thừa và hiện thực, phương thức ghi đè, và lời gọi giữa các phương thức cùng một kiểu. Nguyên tắc an toàn xuyên suốt: **một tham chiếu không phân giải được về một entity đã index thì bị bỏ, không bao giờ đoán**. Đồ thị vì thế có thể thiếu một cạnh, nhưng không chứa cạnh sai — với tri thức cung cấp cho agent, một liên kết sai gây hại hơn một liên kết thiếu.

Việc phân giải mục tiêu đi theo tên định danh đầy đủ và được gom theo lô cho từng file (một lượt tra cứu cho tất cả tham chiếu, thay vì một lượt cho mỗi tham chiếu). Nhờ phân giải theo tên, một import trỏ tới lớp nằm ở **nguồn khác** trong cùng product vẫn nối được xuyên ranh giới nguồn (FR-3.5) — ví dụ service A import một lớp thuộc thư viện dùng chung được index như một nguồn riêng.

## Liên kết liên artifact và điểm tin cậy

Liên kết giữa tài liệu và mã không có cú pháp để dựa vào, nên hệ thống đọc các **tín hiệu** trong nội dung tài liệu và cho điểm theo độ mạnh của tín hiệu:

- **Tham chiếu đường dẫn** — tài liệu nêu rõ đường dẫn một file mã nguồn. Đây là bằng chứng tường minh, không nhập nhằng, nên điểm cao.
- **Tên định danh đầy đủ** — tài liệu nêu tên đầy đủ (kèm package) của một lớp/hàm và tên đó phân giải về đúng một entity: bằng chứng mạnh.
- **Tên ghép dạng CamelCase** — một tên ghép nhiều từ khớp với đúng một entity: bằng chứng vừa, vì tên ghép hiếm khi xuất hiện tình cờ trong văn xuôi.
- **Tên đơn hoặc nhập nhằng** — một từ đơn cũng là từ thông dụng, hoặc một tên khớp với nhiều entity: điểm thấp, và **ngưỡng tin cậy** cấu hình được sẽ loại bỏ mặc định.

Chỉ ứng viên đạt ngưỡng mới được ghi vào đồ thị; phần bị loại được đếm lại để quan sát. Cơ chế ngưỡng cho phép điều chỉnh cân bằng giữa độ phủ và độ sạch của đồ thị mà không sửa logic liên kết.

*Ví dụ.* Một tài liệu thiết kế viết: *"Yêu cầu FR-3 được kiểm chứng bởi `GraphLinkingTests`, phần hiện thực chính nằm ở `com.example.graph.LinkingService`."* Từ đoạn này hệ thống suy ra hai liên kết: yêu cầu FR-3 → VERIFIED_BY → lớp test (mục tiêu là test nên quan hệ là *được kiểm bởi*), và FR-3 → IMPLEMENTED_BY → lớp hiện thực (tên định danh đầy đủ, điểm cao). Ngược lại, nếu tài liệu chỉ nhắc tới từ "Chunk" — vừa là tên lớp vừa là danh từ thường — ứng viên DESCRIBES sẽ nhận điểm thấp và bị ngưỡng loại, tránh nối bừa tài liệu vào mã.

## Lịch sử commit trong đồ thị

Với nguồn Git, mỗi lần đồng bộ còn nạp thêm **lịch sử commit**: các commit mới nhất (số lượng giới hạn cấu hình được) được đọc từ nhánh đã cấu hình, mỗi commit trở thành một entity mang message, tác giả và thời điểm. Diff của commit so với parent đầu tiên cho biết nó chạm vào những file nào — mỗi file trong số đó nếu đã được index thì nhận một cạnh MODIFIES từ commit; file nằm ngoài phạm vi index (bị glob loại, hoặc đã bị xóa) đơn giản là không có cạnh, giữ đúng nguyên tắc *không đoán* ở trên.

Message của commit được embed như một đơn vị tri thức, nên cả tìm kiếm ngữ nghĩa lẫn từ khóa đều với tới nó — agent hỏi *"file này gần đây bị sửa vì lý do gì"* sẽ đi từ file, ngược cạnh MODIFIES, về đúng những commit giải thích thay đổi. Khác với file, lịch sử commit là **bất biến và chỉ nối thêm** (append-only): commit không bao giờ được xử lý lại hay gỡ bỏ lẻ, mỗi lần đồng bộ chỉ nạp phần lịch sử chưa có, và toàn bộ chỉ bị xóa cùng nguồn của nó.

## Ghi đồ thị idempotent

Cạnh được ghi theo khóa *(nguồn, đích, loại quan hệ)*: liên kết lại nội dung không đổi chỉ ghi đè đúng cạnh cũ, không nhân bản. Nhờ đó bước liên kết chạy lại thoải mái sau mỗi lần đồng bộ — kể cả trên file không đổi — để các tham chiếu *trỏ tới file được index muộn hơn* dần được phân giải: khi tài liệu được index trước và mã được index sau, lần liên kết kế tiếp sẽ nối được những gì lần đầu chưa thấy.

## Vai trò trong hệ thống

Knowledge Graph là nửa "quan hệ" của mô hình tri thức GraphRAG: nó cho phép đường truy xuất theo đồ thị mở rộng từ kết quả tìm kiếm ban đầu sang các entity liên quan (hàm được gọi, tài liệu mô tả, yêu cầu liên đới) — loại ngữ cảnh mà tìm kiếm vector thuần bỏ lỡ (xem chương *Truy xuất hybrid*). Trong đặc tả, chương này đỡ trực tiếp cho nhóm yêu cầu liên kết tri thức (FR-3) và yêu cầu dựng Knowledge Graph (FR-2.3).
