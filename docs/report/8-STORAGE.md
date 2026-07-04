# Thiết kế lưu trữ

Mô hình tri thức của hệ thống có hai mặt khác bản chất: **ngữ nghĩa** (chunk nào gần nghĩa với câu hỏi — trả lời bằng so khớp vector) và **quan hệ** (cái gì nối với cái gì — trả lời bằng duyệt đồ thị). Không một loại kho nào phục vụ tốt cả hai, nên hệ thống dùng **hai kho chuyên biệt song song**: một **vector store** cho embedding và một **graph store** cho đồ thị tri thức cùng toàn bộ nội dung, metadata. Chương này trình bày dữ liệu nào nằm ở đâu, hai kho nối với nhau bằng gì, và các cấu trúc chỉ mục đỡ cho từng kiểu truy cập.

## Một chunk, hai hình hài

Đơn vị nối hai kho là **chunk**, với **định danh chunk là khóa dùng chung**. Cùng một chunk hiện diện ở hai nơi dưới hai dạng khác nhau:

- Trong **vector store**, nó là một *điểm*: embedding của nội dung, kèm một **payload** nhỏ chỉ gồm những trường cần cho việc *lọc và định vị* — định danh chunk, nguồn (để lọc phân quyền), đường dẫn, loại dữ liệu, khoảng dòng, và ref/commit với nguồn Git. Đáng chú ý là **văn bản gốc không nằm ở đây**: ngữ nghĩa của nó đã được mã hóa vào embedding, còn chuỗi văn bản thô thì tìm kiếm vector không cần đến.

- Trong **graph store**, nó là một *node*: mang đầy đủ văn bản, content hash, số token, khoảng dòng và provenance, đồng thời nối bằng cạnh vào file chứa nó và (với chunk mã nguồn) vào entity mã mà nó thuộc về.

Tìm kiếm ngữ nghĩa vì thế là một phép **hai thì**: vector store trả về *danh sách định danh kèm điểm số*, rồi nội dung và metadata đầy đủ được nạp từ graph store theo định danh — và chỉ nạp cho những kết quả sống sót sau bước hợp nhất, lọc, để không tốn công tra cứu cho kết quả sẽ bị bỏ.

*Ví dụ.* Chunk chứa hàm `parsePdf` tồn tại dưới dạng: (a) một điểm vector có embedding nhiều chiều và payload `{chunk_id, source_id: "docs-service", path: "src/PdfReader.java", type: "code", line 40–72, ref: "main"}`; và (b) một node đồ thị mang nguyên văn thân hàm, nối *thuộc về* file `PdfReader.java` và *là chunk của* entity phương thức `parsePdf`. Truy vấn ngữ nghĩa chạm vào (a) để tìm và xếp hạng; câu trả lời cuối cùng đọc (b) để lấy nội dung trích dẫn.

## Mô hình đồ thị

Graph store chứa các loại node phản ánh mô hình tri thức: nguồn dữ liệu, file, chunk, entity mã nguồn, tài liệu, yêu cầu và commit; cùng các node vận hành (principal, credential của phân quyền; trạng thái độ mới của từng nguồn). Bộ khung cấu trúc gồm: file *khai báo* entity mức đỉnh, entity *chứa* thành viên của nó, chunk *thuộc về* file và *gắn với* entity tương ứng; bên trên bộ khung là các quan hệ ngữ nghĩa của bộ từ vựng đã trình bày ở chương *Knowledge Graph và liên kết tri thức*.

Mỗi loại node có **ràng buộc duy nhất** trên định danh của nó. Kết hợp với định danh ổn định dẫn xuất từ danh tính/nội dung, mọi thao tác ghi trở thành cập nhật-đè an toàn: xử lý lại dữ liệu không tạo bản trùng, và cạnh không bao giờ trỏ vào node mồ côi.

## Chỉ mục cho từng kiểu truy cập

Mỗi đường truy cập dữ liệu có cấu trúc chỉ mục riêng đỡ cho nó:

- **Tìm theo từ khóa** — graph store duy trì chỉ mục **full-text (BM25)** trên văn bản chunk và trên tên/chữ ký entity. Hai chỉ mục được truy vấn cùng lúc, nên một tên hàm xuất hiện trong văn xuôi hay trong chữ ký mã đều nổi lên được.
- **Khử trùng lặp** — chỉ mục trên content hash của chunk và của file, đỡ cho phép so sánh "nội dung này đã index chưa" khi đồng bộ (FR-6.3).
- **Phân giải tham chiếu** — chỉ mục trên tên định danh đầy đủ và tên đơn của entity, đỡ cho bước liên kết tri thức tra cứu theo lô.
- **Lọc phân quyền** — mọi node tri thức và mọi payload vector đều mang định danh nguồn, và bộ lọc ACL được **đẩy vào trong truy vấn** ở cả hai kho như một điều kiện cứng: kết quả từ nguồn ngoài quyền đọc không bao giờ rời khỏi kho (FR-8.6).

## Vì sao hai kho — và cái giá của nó

Dùng đúng công cụ cho đúng việc: so khớp xấp xỉ trên không gian vector nhiều chiều và duyệt quan hệ nhiều bước là hai bài toán có cấu trúc dữ liệu, chỉ mục và mẫu truy cập hoàn toàn khác nhau. Ép cả hai vào một kho hoặc làm nghèo khả năng đồ thị, hoặc làm nghèo hiệu năng vector.

Cái giá là phải giữ hai kho **nhất quán với nhau**: một vector không còn node đối ứng (hoặc ngược lại) là tri thức chết mà truy vấn vẫn có thể trả về. Hệ thống xử lý bằng cách dùng chung một khóa định danh và gỡ bỏ theo khóa đó ở cả hai kho trong cùng một thao tác đồng bộ (FR-6.4, xem chương *Quản lý phiên bản tri thức*). Đồng thời, cả hai kho đều nằm sau các **cổng trừu tượng** của kiến trúc, nên việc thay thế một backend lưu trữ là thêm một adapter mới thay vì sửa nghiệp vụ (FR-7.2, NFR-3.2).

## Vai trò trong hệ thống

Thiết kế hai kho với khóa chung là nền móng vật lý cho ba năng lực: truy xuất hybrid (mỗi đường tìm kiếm chạy trên kho sở trường của nó rồi hợp nhất bằng định danh chung — FR-4.2), lưu trữ sao cho mỗi chunk truy ngược được nguồn gốc (provenance đầy đủ trên node, tóm tắt trong payload — FR-2.4), và đồng bộ/gỡ bỏ nhất quán giữa hai kho (FR-6.4).
