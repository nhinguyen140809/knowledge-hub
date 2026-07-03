# Kiến trúc hệ thống

Knowledge Hub phải phục vụ nhiều loại dữ liệu, hai giao diện truy cập (MCP và REST API), hai kho lưu trữ khác bản chất (graph và vector) và một provider embedding bên ngoài, đồng thời vẫn dễ kiểm thử, dễ thay thế từng thành phần và dễ mở rộng khi thêm loại dữ liệu hay nguồn mới. Để đạt được điều đó, hệ thống được tổ chức theo **Clean Architecture**: nghiệp vụ nằm ở trung tâm, mọi phụ thuộc hướng vào trong, và các chi tiết công nghệ (web, cơ sở dữ liệu, API ngoài) chỉ là lớp vỏ ngoài cùng có thể thay thế. Chương này trình bày cách phân chia trách nhiệm giữa các tầng, cách đóng gói mã nguồn theo tính năng, và các mẫu kiến trúc chi phối những luồng xử lý chính.

## Phân tầng theo trách nhiệm

Mỗi tính năng trong hệ thống được chia thành ba tầng với trách nhiệm tách bạch, xếp theo một chiều phụ thuộc duy nhất — từ ngoài vào trong, không bao giờ ngược lại.

- **Tầng miền (domain)** — trung tâm của kiến trúc, chứa mô hình nghiệp vụ (entity, value object) và các **cổng** (port): những interface khai báo *thứ mà nghiệp vụ cần* từ thế giới bên ngoài, chẳng hạn khả năng lưu trữ hay sinh embedding. Tầng này là Java thuần, không phụ thuộc bất kỳ framework nào (không Spring, không thư viện lưu trữ), nên các luật nghiệp vụ và bất biến của mô hình được biểu đạt độc lập với công nghệ triển khai. Bất biến được đặt *bên trong* chính đối tượng miền: một `Source` tự đảm bảo phiên bản (`ref`) chỉ có nghĩa với nguồn Git, một chunk tự dẫn xuất định danh từ nội dung của nó.

- **Tầng ứng dụng (application)** — điều phối các ca sử dụng (use case). Tầng này phối hợp các đối tượng miền và gọi qua các cổng để hoàn thành một nghiệp vụ, nhưng bản thân không chứa luật bản chất của mô hình (đã nằm ở domain) và cũng không biết chi tiết công nghệ (nằm ở infrastructure). Nói cách khác, nó nắm *"nghiệp vụ diễn ra theo trình tự nào"* chứ không nắm *"làm bằng công cụ gì"*: chọn bộ xử lý phù hợp với loại nguồn, quyết định thứ tự các bước, đặt ra chính sách khi một bước lỗi, mở ranh giới giao dịch, và phát sự kiện khi một nghiệp vụ hoàn tất. Tầng ứng dụng chỉ được phụ thuộc vào tầng miền.

- **Tầng hạ tầng (infrastructure)** — các **bộ chuyển đổi** (adapter) hiện thực hóa cổng và nối hệ thống với thế giới bên ngoài. Tầng này chia hai hướng: *inbound* (REST controller và MCP tool — các adapter *dẫn* yêu cầu vào hệ thống) và *outbound* (kho Neo4j, kho vector Qdrant, API embedding — các adapter *mà* hệ thống gọi ra). Đây là nơi duy nhất được biết đến framework và công nghệ cụ thể.

Quy tắc phụ thuộc là bất biến kiến trúc quan trọng nhất và được kiểm chứng như một tiêu chí trước mỗi lần tích hợp: hạ tầng được phép phụ thuộc ứng dụng và miền; ứng dụng chỉ được phụ thuộc miền; miền không phụ thuộc gì bên ngoài. Cụ thể, tầng miền khai báo một interface (ví dụ cổng sinh embedding) còn tầng hạ tầng hiện thực nó; tầng ứng dụng nói chuyện với *cổng*, không bao giờ import trực tiếp một adapter — việc gắn adapter cụ thể vào cổng do khung tiêm phụ thuộc (dependency injection) lo tại thời điểm khởi động. Nhờ chiều phụ thuộc này, phần lõi nghiệp vụ có thể được kiểm thử mà không cần cơ sở dữ liệu hay mạng, và một công nghệ hạ tầng có thể được thay thế mà không chạm tới nghiệp vụ.

## Đóng gói theo tính năng

Ở cấp cao nhất, mã nguồn được chia **theo tính năng trước, rồi mới theo tầng** — mỗi gói cấp cao là một năng lực gắn kết (một *bounded context*), và bên trong nó mới chia thành ba tầng nói trên. Cách này giữ mọi thứ liên quan đến một năng lực ở cùng một chỗ, giúp tìm, sửa hay gỡ bỏ một tính năng dễ dàng — trái với việc gom toàn bộ controller vào một gói, toàn bộ service vào một gói (đóng gói theo tầng), vốn làm một thay đổi nghiệp vụ phải lan ra nhiều gói rời rạc.

Hệ thống gồm bốn bounded context và một nhân dùng chung:

- **`access`** — xác thực mọi request và phân quyền truy cập tri thức ở mức nguồn (ACL): principal, grant, credential, và chính sách mặc định.
- **`knowledge`** — phía *ghi*, chịu trách nhiệm dựng nên mô hình tri thức. Do khối lượng lớn, nó được chia tiếp thành các mô-đun con: nạp dữ liệu (ingestion), lập chỉ mục (indexing), dựng đồ thị tri thức (graph), và đồng bộ/cập nhật (sync); kèm theo tập cổng và adapter dùng chung cho cả phía ghi.
- **`retrieval`** — phía *đọc*, phục vụ truy vấn hybrid trên mô hình tri thức đã dựng.
- **`system`** — thông tin vận hành của hệ thống tại runtime.
- **`shared`** — nhân dùng chung, chỉ chứa các khối xây dựng phi-nghiệp-vụ: cấu hình, xử lý lỗi tập trung, sinh định danh, khối `Pipeline`/`Stage` tổng quát, và hạ tầng quan sát (observability). Nếu nảy sinh nhu cầu chia sẻ *logic nghiệp vụ* giữa các tính năng, đó là dấu hiệu cần xem lại ranh giới tính năng, không phải lý do để nhét logic vào `shared`.

Bản đồ các bounded context, các mô-đun con bên trong `knowledge`, và các phụ thuộc biên giới giữa chúng được trình bày ở sơ đồ thành phần [MODULE-ARCHITECTURE.d2](../diagrams/d2/MODULE-ARCHITECTURE.d2).

## Cổng và bộ chuyển đổi

Nguyên tắc **cổng và bộ chuyển đổi** (ports & adapters) là thứ cho phép hệ thống giữ *một* hợp đồng lưu trữ duy nhất trong khi đồ thị tri thức nằm ở Neo4j còn vector nằm ở Qdrant, và cho phép thay một trong hai backend mà không đụng tới mã nghiệp vụ. Chìa khóa là đặt cổng ở đúng ranh giới: **cổng mô tả *cái gì*, không mô tả *bằng cách nào***.

Một cổng nên nằm ở mức năng lực ổn định mà mọi adapter đều có thể đáp ứng, chứ không phải ở tính năng giàu nhất của riêng một công nghệ. Những gì mang tính điều phối, kết hợp nhiều nguồn, hoặc phải cư xử như nhau bất kể backend — chẳng hạn dàn xếp embedding, tìm kiếm từ khóa, duyệt đồ thị, hợp nhất kết quả và lọc ACL — thuộc *trên vạch* (tầng ứng dụng). Những gì thuộc về *cách* một công nghệ lưu và tìm dữ liệu — định dạng lưu trữ, nén/lượng tử hóa, chiến lược lọc cụ thể, cơ chế ghi song song — thuộc *dưới vạch* (adapter) và được tự do thay đổi mà không ảnh hưởng cổng. Nhờ vạch phân chia này, việc đổi engine vector không kéo theo thay đổi nào ở tầng ứng dụng, mà một tính năng đặc thù của công nghệ (ví dụ khả năng nén) cũng không bị đánh mất bởi lớp trừu tượng — nó được kích hoạt *bên trong* adapter thay vì bị loại bỏ.

Khi một công nghệ có một ưu thế thực sự đáng giá mà mẫu số chung không diễn đạt được, ưu thế đó được phơi bày qua một **interface năng lực** tùy chọn: tầng ứng dụng dò xem adapter hiện tại có hỗ trợ hay không, nếu có thì đi đường nhanh, nếu không thì rơi về cơ chế tổng quát. Cách làm này được dùng dè dặt, chỉ khi ưu thế mang lại lợi ích đo được, để vẫn giữ được tính chất "thay backend bằng cấu hình và vượt qua cùng bộ test" (NFR-3.2, NFR-10.1).

## Tách phía ghi và phía đọc

Các bounded context được tách dọc theo ranh giới lệnh/truy vấn, theo tinh thần CQRS ở dạng nhẹ. Context **`knowledge`** là phía *ghi* (nạp → lập chỉ mục → liên kết → đồng bộ), có nhiệm vụ duy nhất là *dựng nên* mô hình đọc gồm vector và đồ thị. Context **`retrieval`** là phía *đọc*, phục vụ truy vấn trên mô hình đó và không bao giờ ghi. Đây là CQRS theo nghĩa nhẹ — tách mô hình và đường xử lý cho ghi so với đọc — chứ **không** phải event sourcing và **không** dùng hai cơ sở dữ liệu riêng. Sự tách bạch này giải thích vì sao `retrieval` là một context độc lập và vì sao nó chỉ đọc: nó cho phép tối ưu, mở rộng và bảo trì hai phía một cách độc lập.

## Đường ống và bộ lọc cho luồng dài

Luồng lập chỉ mục và luồng truy vấn đều là chuỗi nhiều bước dài, nên mỗi luồng được tổ chức thành một **đường ống các bộ lọc** (pipes & filters): mỗi bước là một bộ lọc nhỏ, đơn trách nhiệm, nhận và truyền tiếp một **ngữ cảnh** có kiểu dọc theo đường ống, thay vì gộp tất cả vào một phương thức service khổng lồ. Đây là một lớp tổ chức *đặt lên trên* Clean Architecture chứ không thay thế nó: các bộ lọc là thành phần tầng ứng dụng — chúng điều phối qua cổng miền và không bao giờ chạm trực tiếp vào hạ tầng — nên quy tắc phụ thuộc vẫn được giữ; đường ống và bộ lọc chỉ tổ chức *phần bên trong* của tầng ứng dụng. Khối `Pipeline`/`Stage` tổng quát là một khối phi-nghiệp-vụ nằm ở `shared`, còn các bộ lọc cụ thể sống trong tầng ứng dụng của từng tính năng.

Luồng lập chỉ mục là một đường ống tuần tự: nạp → chia chunk → sinh embedding → lưu và liên kết. Luồng truy vấn là một biến thể *song song* theo kiểu scatter-gather: sau bước chuẩn bị, các đường tìm kiếm ngữ nghĩa và từ khóa tỏa ra chạy song song, bước duyệt đồ thị mở rộng từ kết quả của chúng, rồi một bộ lọc hợp nhất (RRF) gom lại, trước khi lắp ráp kết quả và áp bộ lọc cuối. Điểm cấu trúc này được thể hiện chi tiết ở các sơ đồ lớp [CLASS-INDEXING.d2](../diagrams/d2/CLASS-INDEXING.d2) và [CLASS-RETRIEVAL.d2](../diagrams/d2/CLASS-RETRIEVAL.d2).

Lợi ích của cách tổ chức này trên một luồng dài là cụ thể: có thể sắp xếp lại, chèn thêm hay bỏ qua một bước bằng cấu hình mà không sửa các bước khác; kiểm thử từng bước một cách cô lập; cô lập một bước lỗi để nó không làm hỏng cả mẻ (NFR-6.1); và đo độ trễ theo từng bước (NFR-1, NFR-10.2). Một hệ quả quan trọng: luồng đồng bộ **tái sử dụng chính các bộ lọc của luồng lập chỉ mục** trên phần dữ liệu thay đổi, thay vì nhân đôi logic — đây là một lý do cốt lõi cho việc tách luồng.

## Chiến lược thay thế được

Ở mọi nơi mà một bước có nhiều cách hiện thực hoán đổi được cho nhau — bộ chia chunk cho mã nguồn so với tài liệu, bộ đọc tài liệu cho Markdown so với PDF, bộ so sánh khác biệt cho nguồn Git so với thư mục, chiến lược hợp nhất kết quả, hay các adapter vector và embedding — tầng miền khai báo *một* cổng và tầng hạ tầng cung cấp *nhiều* adapter (mẫu Strategy). Việc chọn adapter dựa trên năng lực (adapter tự khai báo nó hỗ trợ loại nào) hoặc cấu hình, và khung tiêm phụ thuộc tự phát hiện các thành phần. Nhờ vậy, thêm một định dạng tài liệu, một loại nguồn hay một ngôn ngữ mới thường chỉ là **thêm một adapter**, không phải sửa mã sẵn có (NFR-10.3).

## Giao tiếp giữa các ngữ cảnh bằng sự kiện

Những ca sử dụng *kích hoạt* ca sử dụng khác được tách rời bằng cơ chế sự kiện trong tiến trình, thay vì để một service gọi thẳng service của context khác. Một context phát ra một sự kiện ở thì quá khứ — tường thuật *"việc đã xảy ra"* chứ không ra lệnh — chẳng hạn "nguồn đã đăng ký", "nguồn đã xóa", "lập chỉ mục đã hoàn tất"; context nào quan tâm thì tự lắng nghe và phản ứng. Nhờ đó phía phát không cần biết ai lắng nghe, và có thể thêm phía nghe mới mà không sửa phía phát. Ví dụ, khi một nguồn bị xóa, phía đồng bộ lắng nghe sự kiện để gỡ bỏ tri thức tương ứng; khi việc lập chỉ mục hoàn tất, phía truy xuất lắng nghe để làm mới bộ nhớ đệm — mà phía ghi hoàn toàn không phụ thuộc phía đọc.

Cơ chế này quyết định đúng chiều phụ thuộc: nếu gọi trực tiếp sẽ buộc một mô-đun cấp thấp phụ thuộc mô-đun cấp cao, hoặc buộc phía ghi phụ thuộc phía đọc, thì sự kiện đảo ngược sự phụ thuộc đó. Sự kiện là **trong tiến trình**, không dùng message broker; nếu về sau hệ thống được tách thành nhiều dịch vụ, chính những sự kiện này trở thành đường ghép nối tự nhiên.

## Các mối quan tâm xuyên suốt

Một số mối quan tâm cắt ngang mọi tính năng và được xử lý nhất quán ở nhân dùng chung hoặc ở tầng hạ tầng:

- **Cấu hình** — các tham số được nạp vào những đối tượng cấu hình có kiểu, kết hợp với cơ chế profile và biến môi trường để một mã nguồn chạy được trên nhiều môi trường (dev, server, cloud) mà không sửa mã.
- **Bảo mật và phân quyền** — mọi request đều được xác thực, và quyền đọc tri thức được kiểm soát ở mức nguồn qua ACL. Bộ lọc phân quyền được đẩy *vào trong* truy vấn (dưới vạch, ở tầng lưu trữ) và mặc định từ chối khi không có quyền tường minh, để tri thức ngoài phạm vi cho phép không bao giờ rời khỏi kho.
- **Khả năng chịu lỗi** — các lời gọi tới API embedding bên ngoài được bọc bằng cơ chế thử lại kèm khoảng lùi (retry + backoff) ngay tại adapter (NFR-6.2), để một trục trặc tạm thời của provider không làm hỏng cả mẻ. Các chú thích về chịu lỗi đặt trên *adapter*, không bao giờ trên service tầng ứng dụng.
- **Khả năng quan sát** — mỗi request được gắn một định danh tương quan (trace id) xuyên suốt để có thể tái dựng lại toàn bộ một request từ log; hệ thống dùng log có cấu trúc và các điểm cuối kiểm tra sức khỏe/thông tin runtime.

## Vai trò trong hệ thống

Các mẫu kiến trúc trên phối hợp để đạt những thuộc tính chất lượng mà đặc tả đề ra: sự phân tầng và quy tắc phụ thuộc bảo đảm khả năng kiểm thử và bảo trì; cổng và bộ chuyển đổi cùng chiến lược thay thế được bảo đảm khả năng mở rộng khi thêm nguồn, định dạng hay backend (NFR-3.2, NFR-10.1, NFR-10.3); đường ống và bộ lọc bảo đảm khả năng cô lập lỗi và đo lường theo bước (NFR-1, NFR-6.1, NFR-10.2); tách phía ghi/đọc cho phép phục vụ truy vấn độc lập với việc dựng chỉ mục. Toàn bộ cấu trúc mã nguồn được minh họa ở hai mức trong tập sơ đồ: sơ đồ thành phần [MODULE-ARCHITECTURE.d2](../diagrams/d2/MODULE-ARCHITECTURE.d2) cho cái nhìn toàn hệ thống, và các sơ đồ lớp cho từng mô-đun trọng yếu — [CLASS-INDEXING.d2](../diagrams/d2/CLASS-INDEXING.d2) (phía ghi), [CLASS-RETRIEVAL.d2](../diagrams/d2/CLASS-RETRIEVAL.d2) (phía đọc) và [CLASS-ACCESS.d2](../diagrams/d2/CLASS-ACCESS.d2) (xác thực và phân quyền).
