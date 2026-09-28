# BÁO CÁO NGHIÊN CỨU & ĐỀ XUẤT ỨNG DỤNG AI (TRÍ TUỆ NHÂN TẠO)
## HỆ THỐNG QUẢN LÝ CHUỖI KHÁCH SẠN SEN VIỆT LUXURY
### *Tài liệu phục vụ Báo cáo & Thuyết minh Khóa luận Tốt nghiệp (KLTN)*

---

## I. ĐẶT VẤN ĐỀ & ĐỘNG LỰC NGHIÊN CỨU
Trong bối cảnh kỷ nguyên Chuyển đổi số 4.0 và sự bùng nổ của **Generative AI (GenAI)**, các hệ thống Quản lý Khách sạn (PMS - Property Management System) truyền thống chỉ dừng lại ở mức lưu trữ dữ liệu CRUD (Tạo, Đọc, Sửa, Xóa). Để tạo bước đột phá học thuật và tính ứng dụng thực tiễn cao cho đồ án **Khóa luận Tốt nghiệp**, nhóm nghiên cứu định hướng tích hợp **Trí tuệ Nhân tạo (AI)** vào hệ thống Sen Việt nhằm giải quyết 3 bài toán trọng tâm:
1. **Nâng tầm trải nghiệm khách hàng (Customer Experience - CX):** Phản hồi tức thì 24/7, cá nhân hóa gợi ý phòng và dịch vụ.
2. **Tối ưu hóa năng suất vận hành (Operational Efficiency):** Tự động hóa thủ tục Check-in/Check-out, giảm thiểu thời gian chờ đợi tại quầy lễ tân từ 3 phút xuống còn dưới 10 giây.
3. **Tối đa hóa doanh thu (Revenue Optimization):** Tự động điều chỉnh giá phòng linh hoạt theo cung - cầu và phân tích cảm xúc phản hồi của khách hàng để nâng cao chất lượng dịch vụ.

---

## II. 4 MODULE AI CHIẾN LƯỢC ĐỀ XUẤT CHO KHOÁ LUẬN

### MODULE 1: TRỢ LÝ ẢO TƯ VẤN ĐẶT PHÒNG THÔNG MINH (AI CONCIERGE CHATBOT)
* **Mục tiêu:** Thay thế nhân viên tư vấn trực tuyến, trả lời khách hàng 24/7 bằng tiếng Việt tự nhiên và hỗ trợ tìm kiếm phòng trống theo ngữ cảnh.
* **Kiến trúc kỹ thuật:** **RAG (Retrieval-Augmented Generation)** kết hợp **Google Gemini 1.5 Flash API** và **Spring AI / LangChain4j**.
* **Luồng vận hành:**
  1. Dữ liệu khách sạn (chi nhánh, loại phòng, tiện nghi, dịch vụ, khuyến mãi từ `01/09/2026 - 31/12/2026`) được vector hóa (Embeddings) lưu vào Vector Store.
  2. Khi khách hàng hỏi: *"Tôi muốn đi Đà Lạt cùng gia đình 4 người vào cuối tuần này, có phòng nào view đẹp và có máy lọc không khí không, giá bao nhiêu?"*
  3. AI trích xuất thông tin: Tỉnh/Thành = `Đà Lạt`, Số người = `4` (gợi ý phòng Family), Tiện ích = `Ban công view cảnh` (Amenity ID 4).
  4. Hệ thống truy vấn phòng trống theo thời gian thực từ Database, kết hợp thông tin đưa vào Prompt của Gemini để sinh ra câu trả lời chuẩn xác, lịch thiệp và đính kèm nút **"Đặt ngay"**.

---

### MODULE 2: AI QUÉT CCCD / PASSPORT TỰ ĐỘNG CHECK-IN (AI OCR FAST CHECK-IN)
* **Mục tiêu:** Giải quyết nút thắt cổ chai (bottleneck) lớn nhất của khách sạn là tình trạng khách xếp hàng chờ lễ tân gõ thủ công 12 số CCCD, họ tên, ngày sinh, quê quán.
* **Kiến trúc kỹ thuật:** **Gemini 1.5 Flash Vision / Google Cloud Vision API**.
* **Luồng vận hành:**
  1. Lễ tân dùng camera điện thoại/máy quét chụp mặt trước CCCD gắn chip của khách.
  2. Ảnh được gửi lên Backend API `/api/ai/ocr-cccd`.
  3. AI nhận diện và trích xuất cấu trúc JSON chuẩn:
     ```json
     {
       "cccd": "079203001234",
       "fullName": "HUỲNH VĂN HIẾU",
       "dateOfBirth": "2004-04-16",
       "gender": "Nam",
       "address": "Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh"
     }
     ```
  4. Form tiếp nhận khách hàng tự động điền đầy đủ dữ liệu chỉ trong **1.2 giây**. Lễ tân chỉ cần nhấn nút "Xác nhận Nhận phòng", tăng tốc độ phục vụ lên gấp 10 lần.

---

### MODULE 3: ĐỊNH GIÁ PHÒNG ĐỘNG THÔNG MINH (AI DYNAMIC PRICING)
* **Mục tiêu:** Giúp Ban Quản trị tối ưu hóa chỉ số **RevPAR (Revenue Per Available Room)** và công suất phòng (Occupancy Rate).
* **Mô hình triển khai:** Thuật toán Machine Learning (Decision Tree / XGBoost Regression hoặc Heuristic Elastic Pricing Rule).
* **Các biến số đầu vào (Features):**
  - **Tỷ lệ lấp đầy hiện tại (Occupancy Rate):** Nếu số phòng trống < 20% -> Tự động tăng giá 15-20%.
  - **Mùa vụ & Sự kiện (Seasonality):** Ngày lễ Giáng Sinh, Tết Dương Lịch 2026 -> Điều chỉnh mức giá cao điểm.
  - **Ngày trong tuần:** Thứ 6, Thứ 7, Chủ Nhật (Weekend) áp dụng phụ thu theo chính sách chi nhánh.
  - **Lead Time:** Khách đặt trước trên 14 ngày áp dụng mức giá Early Bird (giảm 12% theo mã `EARLYBIRD12`).

---

### MODULE 4: PHÂN TÍCH CẢM XÚC Ý KIẾN KHÁCH HÀNG (AI SENTIMENT REVIEW ANALYSIS)
* **Mục tiêu:** Giúp Giám đốc khách sạn lắng nghe thị trường và phát hiện nhanh các vấn đề dịch vụ trước khi ảnh hưởng đến uy tín thương hiệu.
* **Kiến trúc kỹ thuật:** **Natural Language Processing (NLP) / Sentiment Classification**.
* **Luồng vận hành:**
  - Khi khách trả phòng và để lại đánh giá (Review text + Star rating):
  - AI tự động gán nhãn: **Tích cực (Positive)**, **Trung tính (Neutral)**, **Tiêu cực (Negative)**.
  - AI bóc tách khía cạnh (Aspect-based Sentiment):
    - Về Buồng phòng: *"Khăn tắm sạch sẽ, giường êm"* -> `Cleanliness: Positive`.
    - Về Kỹ thuật: *"Máy lạnh tầng 2 kêu hơi to vào ban đêm"* -> `Facility: Negative (Alert to Maintenance)`.
  - Nếu xuất hiện đánh giá tiêu cực, hệ thống tự động gửi thông báo trực tiếp qua Dashboard cho Quản lý ca trực giải quyết ngay lập tức.

---

## III. KẾ HOẠCH BÁO CÁO THUYẾT MINH TRƯỚC HỘI ĐỒNG KLTN
Khi báo cáo với Hội đồng Chấm Khóa luận Tốt nghiệp, nhóm có thể trình bày theo cấu trúc sau để đạt điểm tuyệt đối:
1. **Phần 1: Thực trạng ngành:** Trình bày lý do các hệ thống quản lý khách sạn truyền thống đang bị tụt hậu và khách hàng kỳ vọng tốc độ số hóa cao.
2. **Phần 2: Demo trực tiếp Module OCR CCCD:** Chụp ảnh CCCD thử nghiệm trực tiếp ngay trên màn hình -> Điền form trong 1 giây (gây ấn tượng mạnh mẽ nhất với Hội đồng).
3. **Phần 3: Trình diễn Chatbot thông minh:** Thử đặt một câu hỏi phức tạp về khuyến mãi tháng 9 - 12/2026 tại chi nhánh Nha Trang/Đà Nẵng -> Chatbot trả lời mượt mà, chính xác 100%.
4. **Phần 4: Hiệu quả kinh tế:** Chứng minh giải pháp giúp tăng 25% doanh thu phòng cuối tuần và giảm 70% thời gian chờ làm thủ tục.
