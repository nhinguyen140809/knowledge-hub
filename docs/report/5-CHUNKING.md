# Chiến lược chunking

Trước khi sinh embedding, dữ liệu thô phải được chia thành các đơn vị nhỏ gọi là **chunk**. Lý do là kép: mô hình embedding chỉ nhận đầu vào trong một giới hạn độ dài nhất định, và một vector duy nhất cho cả một file lớn sẽ pha loãng ngữ nghĩa đến mức truy xuất mất chính xác. Nhưng nếu cắt thô theo độ dài, ranh giới chunk sẽ rơi vào giữa một hàm hay giữa một câu, phá vỡ ý nghĩa và làm giảm chất lượng cả embedding lẫn kết quả trả về. Do đó nguyên tắc nền tảng của chiến lược chunking là **cắt theo ranh giới tự nhiên của dữ liệu, không cắt thô theo kích thước** (FR-2.1).

Vì mã nguồn và tài liệu có cấu trúc khác nhau, hệ thống áp dụng hai chiến lược chunking khác nhau và tự động chọn chiến lược phù hợp cho từng đơn vị dữ liệu dựa trên loại của nó.

## Chunking mã nguồn theo cấu trúc

Mã nguồn được phân tích thành cây cú pháp (**AST**) và cắt theo ranh giới khai báo, không theo độ dài. Chiến lược này tuân theo mấy quy tắc:

- **Mỗi hàm/phương thức là một chunk trọn vẹn.** Một hàm không bao giờ bị cắt làm đôi, kể cả khi vượt quá ngân sách token — một nửa hàm vừa vô nghĩa với người đọc, vừa gây hiểu sai khi truy xuất. Trường hợp hiếm khi một hàm quá dài, hệ thống chấp nhận giữ nguyên chunk lớn hơn ngân sách thay vì phá vỡ tính toàn vẹn của nó.

- **Mỗi kiểu (lớp, interface, enum) có một chunk "khung" (shell).** Chunk khung chứa phần ngữ cảnh của kiểu — chữ ký lớp và các trường thành viên — nhưng **loại bỏ thân các phương thức**, vì thân đã nằm ở chunk riêng của từng phương thức. Nhờ vậy cùng một đoạn nội dung không bị lặp lại trong hai chunk.

- **Chú thích đi kèm được giữ cùng chunk.** Tài liệu hay bình luận đứng ngay trước một khai báo được gộp vào chunk của khai báo đó, để tăng tín hiệu ngữ nghĩa cho embedding.

Song song với việc cắt chunk, chiến lược này còn trích ra một hệ thống phân cấp **entity** (kiểu → phương thức, trường; kể cả kiểu lồng nhau) để dựng Knowledge Graph. Đáng lưu ý: một trường (field) trở thành entity trong graph nhưng **không** là một chunk riêng — nội dung của nó sống trong chunk khung của kiểu chứa nó.

*Ví dụ.* Một lớp có hai phương thức và hai trường được cắt thành **ba chunk**: một chunk cho mỗi phương thức, và một chunk khung chứa chữ ký lớp cùng hai trường. Về phía graph, cùng lớp đó sinh ra **năm entity**: bản thân lớp, hai phương thức và hai trường. Hai phương thức trở thành vector tìm kiếm được; hai trường góp mặt trong graph để trả lời câu hỏi cấu trúc ("lớp này có trường nào") nhưng không tự chiếm một vector riêng.

## Chunking tài liệu theo cấu trúc văn bản

Với tài liệu và các loại văn bản khác, không có AST để dựa vào, nên hệ thống cắt theo **cấu trúc văn bản** với thứ tự ưu tiên ranh giới giảm dần: đoạn văn → dòng → câu → từ → ký tự. Hệ thống luôn chọn ranh giới thô nhất còn nằm trong ngân sách token, và chỉ hạ xuống mức mịn hơn khi một đơn vị vượt ngân sách. Nhờ vậy một chunk hầu như luôn trùng với một đơn vị ý nghĩa hoàn chỉnh (một đoạn, một câu) thay vì bị cắt ngang.

Điểm khác biệt quan trọng so với mã nguồn là **chồng lấn** (overlap): mỗi chunk mang theo một phần đuôi của chunk liền trước, dài tối đa một số token cho trước. Một ý nằm vắt qua ranh giới cắt nhờ đó xuất hiện trọn vẹn trong ít nhất một chunk, nên vẫn tìm được — cải thiện độ bao phủ (recall) khi truy xuất. Mã nguồn không cần chồng lấn vì ranh giới của nó đã là ranh giới ngữ nghĩa trọn vẹn (một hàm), còn với văn bản thì ranh giới cắt mang tính nhân tạo.

*Ví dụ.* Một mục dài vượt ngân sách được cắt thành hai chunk, trong đó vài dòng cuối của chunk thứ nhất được lặp lại ở đầu chunk thứ hai. Nếu một câu định nghĩa quan trọng rơi đúng chỗ nối, nó vẫn nằm nguyên vẹn trong một trong hai chunk chứ không bị chia đôi.

## Kích thước đo theo token thực

Ngân sách của một chunk được đo bằng **số token**, tính theo cùng cách mà mô hình embedding tách token, chứ không ước lượng thô theo số ký tự hay số từ. Nhờ đó mỗi chunk khớp với giới hạn đầu vào thật của mô hình, tránh trường hợp một chunk "trông ngắn" theo ký tự nhưng lại vượt giới hạn token. Ngân sách token tối đa và độ chồng lấn đều **cấu hình được**, với giá trị mặc định hợp lý (khoảng 512 token mỗi chunk và khoảng 64 token chồng lấn), kèm ràng buộc rằng độ chồng lấn luôn nhỏ hơn ngân sách để phép cắt luôn tiến về phía trước.

## Liên hệ với định danh và khử trùng

Mỗi chunk sinh ra được gán một content hash và một định danh dẫn xuất từ nội dung. Nhờ đó chunk có nội dung không đổi được nhận diện qua các lần đồng bộ và không phải sinh lại embedding (FR-6.3). Cơ chế định danh này được trình bày chi tiết trong chương *Quản lý phiên bản tri thức*; ở đây chỉ cần lưu ý rằng cách một chunk được định danh là một phần không tách rời của chiến lược chunking — nó quyết định chunk được tái sử dụng hay thay thế mỗi khi nguồn thay đổi.

## Vai trò trong hệ thống

Chunking là phép biến đổi **đầu tiên** trong pipeline lập chỉ mục: mọi bước sau — sinh embedding, dựng graph, truy xuất — đều thao tác trên chunk. Vì vậy chất lượng chunk đặt trần cho chất lượng truy xuất: một chunk cắt khéo, trọn vẹn ngữ nghĩa sẽ cho embedding sắc nét và kết quả trả về chính xác, còn một chunk cắt vụn sẽ kéo theo sai lệch suốt phần còn lại của pipeline.

Chiến lược chunking cũng là một **điểm mở rộng**: mỗi loại dữ liệu mới — chẳng hạn một ngôn ngữ lập trình khác — được hỗ trợ bằng cách bổ sung một chiến lược cắt theo cấu trúc riêng cho nó, trong khi các loại chưa có chiến lược chuyên biệt vẫn được xử lý an toàn theo cách chunking văn bản.

Trong đặc tả, chiến lược chunking đỡ trực tiếp cho yêu cầu chia dữ liệu trước khi sinh embedding (FR-2.1), là bước tiền đề cho việc sinh embedding theo từng chunk (FR-2.2) và lưu trữ sao cho mỗi chunk truy ngược được về nguồn gốc (FR-2.4), đồng thời phối hợp với cơ chế khử trùng lặp theo nội dung (FR-6.3).
