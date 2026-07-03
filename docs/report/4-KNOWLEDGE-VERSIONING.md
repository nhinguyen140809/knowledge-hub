# Quản lý phiên bản tri thức

Nguồn dữ liệu của một product thay đổi liên tục: mã nguồn được sửa, tài liệu được cập nhật, file được thêm hoặc xóa. Để tri thức trong hub luôn phản ánh đúng nguồn mà không phải dựng lại toàn bộ chỉ mục sau mỗi thay đổi, hệ thống cần xác định được *phần nào đã đổi, phần nào giữ nguyên, và phần nào không còn tồn tại*. Cơ chế nền tảng cho việc này là **định danh ổn định dẫn xuất từ nội dung** (content-addressed stable identifier).

## Định danh ổn định

Mỗi đơn vị tri thức — file, entity mã nguồn, chunk — được gán một định danh **tất định** (deterministic): cùng một đầu vào luôn sinh ra cùng một định danh, độc lập với thời điểm hay số lần xử lý. Định danh được dẫn xuất bằng cách băm các thành phần đặc trưng của đơn vị, theo hai chiến lược khác nhau tùy bản chất của đơn vị:

- **Định danh theo danh tính** — áp dụng cho file và entity mã nguồn. Định danh dẫn xuất từ *danh tính* của đơn vị (nguồn và đường dẫn file; với entity bổ sung tên định danh đầy đủ) và **không** phụ thuộc nội dung. Nhờ vậy một file giữ nguyên định danh qua mọi lần chỉnh sửa: khi nội dung đổi, đơn vị được **cập nhật tại chỗ** dưới cùng một định danh.

- **Định danh theo nội dung** — áp dụng cho chunk. Ngoài danh tính, định danh còn dẫn xuất từ **content hash** của chunk, nên nội dung đổi thì định danh cũng đổi: một chunk có nội dung mới là một *chunk khác*, không phải cùng chunk được sửa. Lựa chọn này xuất phát từ bản chất của chunk — giá trị của một chunk nằm ở embedding của nó, mà embedding là hàm của nội dung; đồng thời ranh giới chunk dịch chuyển khi file thay đổi, nên không tồn tại một vị trí chunk bền vững để cập nhật đè lên.

## Các tính chất được bảo đảm

Định danh ổn định là nền tảng chung cho toàn bộ cơ chế đồng bộ và cập nhật (FR-6):

- **Tính idempotent (FR-6.1)** — xử lý lại một nguồn không thay đổi sinh ra đúng tập định danh cũ, nên mọi thao tác ghi trở thành cập nhật-đè (upsert) thay vì tạo bản trùng; kích hoạt đồng bộ nhiều lần trên cùng một trạng thái là một no-op.

- **Cập nhật tăng dần (FR-6.2)** — vì định danh ổn định giữa các lần chạy, hệ thống so sánh tập định danh mới sinh với tập đang lưu để nhận ra chunk nào được thêm và chunk nào không còn; chỉ phần thay đổi được xử lý, thay vì lập lại chỉ mục toàn bộ.

- **Khử trùng lặp theo nội dung (FR-6.3)** — chunk có nội dung không đổi cho ra cùng định danh, nên được nhận diện là đã tồn tại và **không sinh lại embedding**, tránh gọi lại embedding provider cho phần dữ liệu không đổi — thao tác tốn kém nhất trong pipeline.

- **Gỡ bỏ nhất quán (FR-6.4)** — định danh chunk là khóa dùng chung giữa vector store và graph store. Khi một file thu hẹp hoặc bị xóa, hệ thống gỡ đúng những định danh không còn thuộc file khỏi cả hai kho cùng lúc, giữ chúng đồng bộ và không để lại tri thức lỗi thời (một vector không còn node tương ứng, hoặc ngược lại) mà truy vấn vẫn trả về được.

Khi một file được xử lý lại, các tính chất trên phối hợp thành một phép hợp nhất: chunk có nội dung không đổi giữ nguyên định danh và được giữ lại kèm embedding sẵn có; chunk có nội dung mới nhận định danh mới và được thêm vào; chunk cũ không còn xuất hiện bị gỡ bỏ. Nhờ đó tri thức luôn khớp với nguồn ở mức nội dung, đồng thời mỗi chunk vẫn truy ngược được về nguồn gốc theo provenance (FR-1.4, FR-2.4).

## Vai trò trong hệ thống

Cơ chế định danh ổn định là nền tảng cho hai năng lực của hệ thống:

- **Quản lý phiên bản tri thức (đồng bộ & gỡ bỏ)** — các tính chất idempotent, cập nhật tăng dần và gỡ bỏ nhất quán cho phép cập nhật hoặc loại bỏ tri thức cũ khi nguồn thay đổi, giữ chỉ mục luôn khớp với nguồn.
- **Chiến lược chunking** — sơ đồ định danh của chunk (định danh theo nội dung) là một phần của chiến lược chunking, quyết định cách một chunk được nhận diện, tái sử dụng hay thay thế qua các lần đồng bộ.

Trong đặc tả, cơ chế đỡ trực tiếp cho nhóm yêu cầu đồng bộ & cập nhật (FR-6) và bổ trợ cho yêu cầu lưu trữ sao cho mỗi chunk truy ngược được nguồn gốc (FR-2.4).
