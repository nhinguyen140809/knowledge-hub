# Truy xuất hybrid

Không một kỹ thuật tìm kiếm đơn lẻ nào đủ cho tri thức phần mềm. Semantic search giỏi bắt *ý* ("hàm nào xử lý việc đọc PDF") nhưng dễ trượt một định danh chính xác; keyword search bắt trúng định danh (`parsePdf`) nhưng mù trước cách diễn đạt khác đi; và cả hai đều không thấy được thứ *liên quan qua quan hệ* — hàm được gọi bởi kết quả, tài liệu mô tả nó, yêu cầu mà nó hiện thực. Vì vậy hệ thống truy xuất theo lối **hybrid**: chạy nhiều đường tìm kiếm bổ khuyết cho nhau rồi hợp nhất thành một danh sách xếp hạng duy nhất (FR-4.2).

## Truy vấn là ngôn ngữ tự nhiên, không diễn giải

Đầu vào là một câu truy vấn văn bản tự do (FR-4.1). Hệ thống **không** cố hiểu ý định của câu — đó là vai trò của agent đứng ngoài. Văn bản được dùng nguyên vẹn theo hai cách: embedding hóa để so khớp ngữ nghĩa, và tách từ để so khớp từ khóa. Phân định vai trò này giữ hệ thống đơn giản và dự đoán được: cùng một câu hỏi luôn cho cùng một kết quả, không phụ thuộc một tầng "đoán ý" nào ở giữa.

## Các đường tìm kiếm và hợp nhất

Một truy vấn đi qua đường ống các bước sau:

1. **Chuẩn bị** — sinh embedding cho câu truy vấn và xác định tập nguồn được phép đọc của principal (dùng cho bộ lọc phân quyền ở mọi bước sau).
2. **Semantic search ∥ keyword search** — hai đường chạy **song song**: một so khớp embedding trên vector store, một chạy BM25 trên chỉ mục full-text (cả văn bản chunk lẫn tên/chữ ký entity). Mỗi đường trả về danh sách định danh kèm điểm số riêng của nó.
3. **Graph traversal** — lấy các kết quả tốt nhất của hai đường trên làm **hạt giống**, đi theo các cạnh của Knowledge Graph (gọi hàm, mô tả, hiện thực yêu cầu…) để với tới những mảnh tri thức *liên quan nhưng không tự khớp* với câu truy vấn; kết quả gần hạt giống được điểm cao hơn kết quả xa.
4. **Hợp nhất (RRF)** — ba danh sách được trộn bằng **Reciprocal Rank Fusion**: điểm hợp nhất của một kết quả tính từ *thứ hạng* của nó trong từng danh sách, không từ điểm thô. Chọn RRF vì điểm số của ba hệ (khoảng cách vector, BM25, độ gần đồ thị) nằm trên các thang không so được với nhau — hạng thì luôn so được. Kết quả xuất hiện trên nhiều đường tự nhiên trồi lên đầu.
5. **Lắp ráp và lọc cuối** — nạp metadata đầy đủ từ graph store *chỉ cho các kết quả sống sót*, áp các bộ lọc chức năng (ref, loại dữ liệu — FR-4.4) và bộ lọc phân quyền lần cuối, rồi trả về danh sách xếp hạng.

Xuyên suốt các bước, bộ lọc phân quyền là **pre-filter cứng đẩy vào từng kho**: nguồn ngoài quyền đọc bị loại ngay trong truy vấn ở vector store, keyword search và graph traversal, chứ không đợi lọc sau khi kết quả đã rời kho (FR-8.6, NFR-7.4).

*Ví dụ.* Truy vấn *"chỗ nào xử lý việc trích văn bản từ file PDF?"*: đường semantic tìm ra chunk của hàm đọc PDF nhờ nghĩa của câu; đường keyword bắt thêm entity có chữ ký chứa `Pdf`; đường graph, xuất phát từ hai kết quả đó, kéo thêm tài liệu hướng dẫn *mô tả* module đọc tài liệu và hàm tách nội dung mà hàm đọc PDF *gọi tới*. Sau hợp nhất, chunk hàm đọc PDF — xuất hiện ở cả ba đường — đứng đầu; các kết quả chỉ có mặt trên một đường xếp sau, kèm chú thích chuỗi quan hệ đã đi qua đối với kết quả đến từ đồ thị.

## Kết quả trả về

Mỗi kết quả gồm định danh, **relevance score** và metadata đủ để agent trích dẫn và tự lấy lại: loại (chunk hay entity), nguồn, đường dẫn file, khoảng dòng, loại dữ liệu, ref/commit và thời điểm index (FR-4.3, FR-6.5). Kết quả đến từ đường đồ thị mang thêm **chuỗi quan hệ đã duyệt** — agent nhìn vào đó biết *vì sao* mảnh tri thức này liên quan (ví dụ: "được tìm thấy qua DESCRIBES → CALLS").

## Các hành vi biên

- **Ref chưa được index** (FR-4.5) — khi truy vấn khoanh vào một ref chưa từng index, hệ thống không trả rỗng vô ích mà **rơi về ref canonical** (nhánh chính đã index) và đánh dấu rõ điều này trong kết quả, để agent biết ngữ cảnh nhận được thuộc phiên bản nào.
- **Một đường tìm kiếm lỗi** — đường lỗi được ghi log và **suy biến về danh sách rỗng** thay vì làm hỏng cả truy vấn: người dùng vẫn nhận kết quả từ các đường còn lại, chỉ kém phong phú hơn (NFR-6.1).
- **Truy vấn lặp lại** — kết quả được cache theo cặp *(nội dung truy vấn, tập nguồn được phép đọc)*: câu hỏi trùng trả về tức thì không chạm kho (NFR-1.2). Đưa tập quyền vào khóa cache là bắt buộc — hai principal khác quyền hỏi cùng một câu phải nhận hai kết quả khác nhau, cache không được làm rò tri thức xuyên quyền. Cache được làm mới khi tri thức thay đổi sau một lần đồng bộ.

## Vai trò trong hệ thống

Truy xuất hybrid là phía *đọc* của toàn hệ thống — điểm mà mọi đầu tư ở phía ghi (chunking giữ trọn ngữ nghĩa, đồ thị quan hệ, hai kho chuyên biệt) quy về giá trị sử dụng: agent hỏi bằng ngôn ngữ tự nhiên và nhận đúng ngữ cảnh kèm căn cứ. Trong đặc tả, chương này đỡ trực tiếp cho nhóm yêu cầu truy xuất (FR-4), yêu cầu lọc phân quyền trên mọi đường truy xuất (FR-8.6), và các chỉ tiêu chất lượng — độ trễ (NFR-1), chất lượng truy xuất với hybrid phải vượt semantic thuần (NFR-2).
