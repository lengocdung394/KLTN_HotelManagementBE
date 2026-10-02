package iuh.fit.se.hotelmanagement_be.modular.ai.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import iuh.fit.se.hotelmanagement_be.modular.ai.requests.ChatMessage;
import iuh.fit.se.hotelmanagement_be.modular.ai.requests.ChatRequest;
import iuh.fit.se.hotelmanagement_be.modular.ai.responses.ChatResponse;
import iuh.fit.se.hotelmanagement_be.modular.ai.services.AiChatService;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.CustomerRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingChannel;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingDetailCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingServiceRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.BookingService;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BranchRoomPolicyRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.requests.PaymentRequest;
import iuh.fit.se.hotelmanagement_be.modular.payment.responses.PaymentResponse;
import iuh.fit.se.hotelmanagement_be.modular.payment.services.PaymentService;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.Promotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionStatus;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.PromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Amenity;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.AmenityRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import iuh.fit.se.hotelmanagement_be.modular.service.repositories.ServiceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AiChatServiceImpl implements AiChatService {

    HotelRepository hotelRepository;
    BranchRoomPolicyRepository branchRoomPolicyRepository;
    AmenityRepository amenityRepository;
    ServiceRepository serviceRepository;
    PromotionRepository promotionRepository;
    RoomRepository roomRepository;
    CustomerRepository customerRepository;
    BookingService bookingService;
    PaymentService paymentService;
    RestTemplate restTemplate;
    ObjectMapper objectMapper;

    @NonFinal
    @Value("${gemini.api-key}")
    String apiKey;

    @NonFinal
    @Value("${gemini.model:gemini-flash-latest}")
    String model;

    static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    static final int MAX_HISTORY_SIZE = 10;
    static final long CONTEXT_CACHE_DURATION_MS = 120_000; // 2 phút

    @NonFinal
    volatile String cachedHotelContext = null;

    @NonFinal
    volatile long lastContextCacheTime = 0;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public ChatResponse chat(ChatRequest request) {
        try {
            // 1. Generate session ID if not provided
            String sessionId = (request.getSessionId() != null && !request.getSessionId().isBlank())
                    ? request.getSessionId()
                    : UUID.randomUUID().toString();

            // 2. Ưu tiên chuyển tiếp yêu cầu tới Python AI Microservice (FastAPI + Vector DB RAG)
            try {
                String pythonAiUrl = "http://127.0.0.1:8000/ai/chat";
                ResponseEntity<JsonNode> pythonRes = restTemplate.postForEntity(pythonAiUrl, request, JsonNode.class);
                if (pythonRes.getStatusCode().is2xxSuccessful() && pythonRes.getBody() != null) {
                    JsonNode resultNode = pythonRes.getBody().path("result");
                    String pyReply = resultNode.path("reply").asText();
                    if (pyReply != null && !pyReply.isBlank()) {
                        pyReply = handleCreateBookingAction(pyReply, request);
                        return ChatResponse.builder()
                                .reply(pyReply)
                                .sessionId(sessionId)
                                .build();
                    }
                }
            } catch (Exception ex) {
                log.info("AI Microservice (Python) khong phan hoi, su dung bo du phong: {}", ex.getMessage());
            }

            // 3. Build hotel context from database (including real-time available rooms)
            String hotelContext = buildHotelContext();

            // 3. Build system prompt with context
            String systemPrompt = buildSystemPrompt(hotelContext);

            // 4. Prepare conversation history (limit to last N messages)
            List<ChatMessage> history = request.getHistory() != null
                    ? request.getHistory().stream()
                        .skip(Math.max(0, request.getHistory().size() - MAX_HISTORY_SIZE))
                        .collect(Collectors.toList())
                    : new ArrayList<>();

            // 5. Call Gemini API
            String reply = callGeminiApi(systemPrompt, history, request.getMessage());

            // 6. Xử lý Action tự động tạo đơn đặt phòng nếu khách đã xác nhận chốt đơn
            reply = handleCreateBookingAction(reply, request);

            return ChatResponse.builder()
                    .reply(reply)
                    .sessionId(sessionId)
                    .build();

        } catch (Exception e) {
            log.error("AI Chat error: {}", e.getMessage(), e);
            return ChatResponse.builder()
                    .reply("Xin lỗi, tôi đang gặp sự cố kỹ thuật. Vui lòng thử lại sau hoặc liên hệ hotline để được hỗ trợ trực tiếp. 🙏")
                    .sessionId(request.getSessionId())
                    .build();
        }
    }

    /**
     * Query all relevant hotel data from DB and format into a context string
     */
    private String buildHotelContext() {
        long now = System.currentTimeMillis();
        if (cachedHotelContext != null && (now - lastContextCacheTime) < CONTEXT_CACHE_DURATION_MS) {
            return cachedHotelContext;
        }

        StringBuilder sb = new StringBuilder();
        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

        // --- Hotels & Branches ---
        List<Hotel> hotels = hotelRepository.findAll();
        sb.append("=== DANH SÁCH CHI NHÁNH KHÁCH SẠN SEN VIỆT ===").append("\n");
        for (Hotel hotel : hotels) {
            sb.append(String.format("- %s | Địa chỉ: %s | Tỉnh/Thành: %s | SĐT: %s\n",
                    hotel.getName(),
                    hotel.getAddress(),
                    hotel.getProvince() != null ? hotel.getProvince().getName() : "N/A",
                    hotel.getPhone() != null ? hotel.getPhone() : "N/A"));
        }

        // --- Room Types & Prices ---
        List<BranchRoomPolicy> policies = branchRoomPolicyRepository.findAll();
        sb.append("\n=== BẢNG GIÁ PHÒNG THEO CHI NHÁNH ===").append("\n");
        for (BranchRoomPolicy p : policies) {
            sb.append(String.format("- Chi nhánh: %s | Loại phòng: %s | Giá cơ bản: %s VNĐ/đêm | Sức chứa tiêu chuẩn: %d người | Tối đa: %d người | Phụ thu người lớn: %s VNĐ | Phụ thu trẻ em: %s VNĐ\n",
                    p.getHotel() != null ? p.getHotel().getName() : "N/A",
                    p.getRoomType().name(),
                    currencyFormat.format(p.getBasePrice()),
                    p.getStandardCapacity(),
                    p.getMaxCapacity(),
                    currencyFormat.format(p.getExtraAdultFee()),
                    currencyFormat.format(p.getExtraChildFee())));
        }

        // --- Amenities ---
        List<Amenity> amenities = amenityRepository.findAll();
        sb.append("\n=== TIỆN ÍCH PHÒNG ===").append("\n");
        for (Amenity a : amenities) {
            sb.append(String.format("- %s: %s VNĐ\n",
                    a.getName(),
                    a.getPrice() != null ? currencyFormat.format(a.getPrice()) : "Miễn phí"));
        }

        // --- Services ---
        List<iuh.fit.se.hotelmanagement_be.modular.service.entities.Service> services =
                serviceRepository.findAll().stream()
                        .filter(s -> Boolean.TRUE.equals(s.getActive()))
                        .collect(Collectors.toList());
        sb.append("\n=== DỊCH VỤ KHÁCH SẠN ===").append("\n");
        for (var svc : services) {
            sb.append(String.format("- %s | Giá: %s VNĐ/%s | Phân loại: %s | Mô tả: %s\n",
                    svc.getName(),
                    currencyFormat.format(svc.getPrice()),
                    svc.getUnit() != null ? svc.getUnit() : "lượt",
                    svc.getCategory() != null ? svc.getCategory() : "Khác",
                    svc.getDescription() != null ? svc.getDescription() : ""));
        }

        // --- Active Promotions ---
        List<Promotion> promotions = promotionRepository.findAll().stream()
                .filter(p -> p.getStatus() == PromotionStatus.ACTIVE
                        && p.getEndDate() != null
                        && p.getEndDate().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());
        sb.append("\n=== KHUYẾN MÃI ĐANG ÁP DỤNG ===").append("\n");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (promotions.isEmpty()) {
            sb.append("- Hiện tại chưa có chương trình khuyến mãi nào.\n");
        } else {
            for (Promotion promo : promotions) {
                String minOrder = (promo.getMinBookingValue() != null && promo.getMinBookingValue().compareTo(BigDecimal.ZERO) > 0)
                        ? " | Đơn tối thiểu: " + currencyFormat.format(promo.getMinBookingValue()) + " VNĐ"
                        : " | Không yêu cầu đơn tối thiểu";
                sb.append(String.format("- Mã: %s | Tên: %s | Giảm: %s%s%s | HSD: %s - %s | Mô tả: %s\n",
                        promo.getCode(),
                        promo.getName(),
                        promo.getDiscountValue(),
                        promo.getType().name().contains("PERCENT") ? "%" : " VNĐ",
                        minOrder,
                        promo.getStartDate().format(dtf),
                        promo.getEndDate().format(dtf),
                        promo.getDescription() != null ? promo.getDescription() : ""));
            }
        }

        // --- Real-time Available Rooms from Database ---
        try {
            List<Room> allRooms = roomRepository.findAll();
            sb.append("\n=== TÌNH TRẠNG PHÒNG TRỐNG THỰC TẾ THEO CHI NHÁNH (REAL-TIME TỪ DATABASE) ===").append("\n");
            Map<String, Long> availableCounts = allRooms.stream()
                    .filter(r -> r.getRoomStatus() == RoomStatus.READY
                            && r.getFloor() != null
                            && r.getFloor().getBuilding() != null
                            && r.getFloor().getBuilding().getHotel() != null)
                    .collect(Collectors.groupingBy(
                            r -> r.getFloor().getBuilding().getHotel().getName() + " - Loại phòng " + (r.getRoomType() != null ? r.getRoomType().name() : "STANDARD"),
                            Collectors.counting()
                    ));
            if (availableCounts.isEmpty()) {
                sb.append("- Tất cả các chi nhánh hiện đang sẵn sàng phòng phục vụ quý khách.\n");
            } else {
                availableCounts.forEach((info, count) -> {
                    sb.append(String.format("- %s: Hiện còn %d phòng trống sẵn sàng đón khách.\n", info, count));
                });
            }
        } catch (Exception e) {
            log.warn("Không thể tải danh sách phòng trống thực tế: {}", e.getMessage());
        }

        cachedHotelContext = sb.toString();
        lastContextCacheTime = now;
        return cachedHotelContext;
    }

    /**
     * Build the system prompt with hotel context injected
     */
    private String buildSystemPrompt(String hotelContext) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String todayStr = today.format(dtf);
        java.time.LocalDate tomorrow = today.plusDays(1);
        java.time.LocalDate dayAfter = today.plusDays(2);

        String template = """
                Bạn là **Trợ lý AI Agent thông minh của Chuỗi Khách sạn Sen Việt Luxury** — một hệ thống khách sạn cao cấp tại Việt Nam.
                Hôm nay là ngày: %s

                ## NGUYÊN TẮC PHỤC VỤ & GIAO TIẾP QUAN TRỌNG:
                1. **NGÔN NGỮ PHẢN HỒI (ĐA NGÔN NGỮ TỰ ĐỘNG - BẮT BUỘC):**
                   - Tự động nhận diện ngôn ngữ của khách hàng và phản hồi bằng chính ngôn ngữ đó:
                     + Khách nhắn tiếng Anh -> BẮT BUỘC trả lời bằng tiếng Anh chuẩn mực, thân thiện, chuyên nghiệp theo phong cách khách sạn quốc tế.
                     + Khách nhắn tiếng Việt -> Trả lời bằng tiếng Việt lịch sự, chu đáo, xưng "em" và gọi khách là "anh/chị" hoặc "quý khách".
                     + Khách nhắn ngôn ngữ khác (Trung, Hàn, Nhật...) -> Trả lời bằng đúng ngôn ngữ đó.
                2. **KIỂM TRA NGÀY LƯU TRÚ (BẮT BUỘC):**
                   - Hôm nay là ngày %s.
                   - Ngày nhận phòng (Check-in) BẮT BUỘC phải là từ hôm nay trở về sau.
                   - NẾU khách yêu cầu đặt phòng hoặc hỏi về một ngày trong QUÁ KHỨ (ví dụ: hôm nay là cuối tháng 9 mà khách nói "ngày 2/9", hoặc bất kỳ ngày nào trước hôm nay):
                     BẮT BUỘC bạn phải lịch sự nhắc nhở khách ngay bằng ngôn ngữ khách vừa dùng (VD tiếng Việt: *"Dạ hôm nay đã là ngày %s rồi, ngày anh/chị vừa hỏi đã qua mất rồi ạ! Anh/chị có nhầm sang năm sau hoặc dự kiến đổi sang ngày nào sắp tới không để em hỗ trợ kiểm tra phòng trống cho mình nhé ạ!"* hoặc tiếng Anh tương đương).
                     TUYỆT ĐỐI KHÔNG tư vấn đặt phòng hay khen ngợi khách sạn cho ngày đã qua trong quá khứ!
                3. **Chỉ trả lời dựa trên dữ liệu bên dưới.** Nếu không có thông tin, hãy gợi ý khách liên hệ hotline chi nhánh.
                4. **Định dạng số tiền:** Ví dụ 1.200.000 VNĐ.
                5. **ƯU TIÊN TỐC ĐỘ & SÚC TÍCH:** Trả lời rõ ràng, cô đọng, đi thẳng vào câu hỏi của khách hàng (dưới 150 từ), không viết lan man dài dòng.

                ## QUY TRÌNH HỘI THOẠI ĐẶT PHÒNG THÔNG MINH (THEO CHUẨN NGHIỆP VỤ HỆ THỐNG):
                Khi người dùng thể hiện ý muốn ĐẶT PHÒNG, bạn là Trợ lý AI Agent dẫn dắt khách hàng qua đúng các bước nghiệp vụ sau (LƯU Ý: Luôn giao tiếp bằng ngôn ngữ tương ứng mà khách hàng đang sử dụng. Nếu khách nói tiếng Anh thì chào hỏi, hỏi ngày, hỏi thông tin và tư vấn hoàn toàn bằng tiếng Anh!):

                - **BƯỚC 1 (Tra cứu phòng trống & Lịch lưu trú):**
                  Khi khách muốn đặt phòng tại 1 chi nhánh:
                  1. Kiểm tra ngày khách hỏi: nếu là ngày quá khứ thì từ chối ngay như nguyên tắc 2. Nếu hợp lệ:
                  2. Tra cứu xem chi nhánh đó hiện có những loại phòng nào đang còn trống (từ dữ liệu real-time ở dưới), báo giá cơ bản và gợi ý loại phòng phù hợp.
                  3. Lịch sự hỏi: *"Quý khách dự kiến nhận phòng (Check-in) và trả phòng (Check-out) vào ngày nào ạ?"*
                  4. Đính kèm Thẻ Đặt Phòng Nhanh ở cuối tin nhắn theo ĐÚNG định dạng JSON sau:
                  ```json:booking_card
                  {
                    "hotelName": "Sen Việt Đà Nẵng",
                    "hotelSlug": "sen-viet-da-nang",
                    "checkInDate": "2026-09-28",
                    "checkOutDate": "2026-09-29",
                    "roomType": "DELUXE",
                    "roomTypeName": "Phòng Deluxe Cao Cấp",
                    "pricePerNight": 2200000,
                    "rooms": [
                      { "type": "STANDARD", "typeName": "Phòng Tiêu Chuẩn", "price": 1200000 },
                      { "type": "DELUXE", "typeName": "Phòng Cao Cấp", "price": 2200000 },
                      { "type": "SUITE", "typeName": "Phòng Thượng Hạng", "price": 3400000 },
                      { "type": "FAMILY", "typeName": "Phòng Gia Đình", "price": 3800000 }
                    ]
                  }
                  ```

                - **BƯỚC 2 (Số lượng khách, Phụ thu & Thông tin người đặt):**
                  Khi khách đã cung cấp ngày nhận/trả phòng:
                  1. Xác nhận ngày và tính số đêm.
                  2. Hỏi số lượng khách: *"Phòng mình dự kiến đi bao nhiêu người lớn, trẻ em (2-12 tuổi) và em bé (dưới 2 tuổi) ạ?"*
                  3. Lịch sự xin thông tin: *"Anh/chị vui lòng cho em xin **Họ tên**, **Số điện thoại** và **Số CCCD/CMND** của người đại diện nhận phòng nhé ạ!"*
                  *QUY TẮC PHỤ THU & ĐỊNH MỨC KHÁCH (BẮT BUỘC):*
                  - Em bé dưới 2 tuổi: Miễn phí.
                  - Nếu số người lớn vượt quá sức chứa tiêu chuẩn của phòng: Tính phụ thu người lớn (theo `Phụ thu người lớn` trong bảng giá) cho mỗi người/đêm.
                  - Trẻ em từ 2 đến dưới 12 tuổi: Tính phụ thu trẻ em (theo `Phụ thu trẻ em` trong bảng giá) cho mỗi bé/đêm.

                - **BƯỚC 3 (Tư vấn Dịch vụ đi kèm):**
                  Khi đã có thông tin khách và số người:
                  1. Giới thiệu 2 - 3 dịch vụ nổi bật từ mục DỊCH VỤ KHÁCH SẠN bên dưới (như Buffet sáng, Spa, Đưa đón sân bay...) kèm đơn giá.
                  2. Hỏi khách: *"Anh/chị có nhu cầu đăng ký thêm dịch vụ nào (như buffet sáng, spa, đưa đón...) kèm số lượng để chuyến đi trọn vẹn hơn không ạ?"* (Khách có thể chọn hoặc nhắn "không cần" để bỏ qua).

                - **BƯỚC 4 (Tư vấn Ưu đãi & Mã khuyến mãi):**
                  Khi khách phản hồi về dịch vụ:
                  1. Giới thiệu các chương trình KHUYẾN MÃI ĐANG ÁP DỤNG bên dưới.
                  2. Hỏi khách có mã voucher nào muốn dùng không. Nếu khách cung cấp mã:
                     - Kiểm tra điều kiện "Đơn tối thiểu". Nếu tiền phòng chưa đạt, giải thích lịch sự lý do.
                     - Nếu đạt điều kiện, tính toán số tiền được giảm giá theo %% hoặc tiền cố định.

                - **BƯỚC 5 (Tóm tắt phiếu đặt phòng chi tiết & Lựa chọn phương thức thanh toán):**
                  Tổng hợp bảng kê chi tiết minh bạch:
                  • 🏨 Chi nhánh & Phòng: [Tên khách sạn] - [Loại phòng]
                  • 📅 Thời gian: [Ngày nhận] ➔ [Ngày trả] ([x] đêm)
                  • 👤 Người đặt: [Họ tên] - [SĐT] - CCCD: [CCCD]
                  • 👥 Khách lưu trú: [x] người lớn, [y] trẻ em (Phụ thu: [z] VNĐ nếu có)
                  • 🛎️ Dịch vụ đi kèm: [Tên dv × số lượng: thành tiền] hoặc "Không đăng ký"
                  • 🎁 Khuyến mãi áp dụng: [Mã: giảm x VNĐ] hoặc "Không có"
                  • 💰 TỔNG CỘNG THANH TOÁN: [Tổng tiền cuối cùng = Phòng + Phụ thu + Dịch vụ - Khuyến mãi] VNĐ

                  LỊCH SỰ HỎI HÌNH THỨC THANH TOÁN:
                  *"Anh/chị muốn thanh toán qua hình thức nào để em tạo đơn giữ chỗ ạ?
                   1. **Chuyển khoản quét mã VietQR PayOS** (giữ chỗ tự động tức thì).
                   2. **Thanh toán trực tiếp tại quầy lễ tân** khi nhận phòng (Check-in)."*

                - **BƯỚC 6 (TỰ ĐỘNG TẠO ĐƠN THEO PHƯƠNG THỨC THANH TOÁN):**
                  Khi khách chọn hình thức thanh toán hoặc nhắn "chuyển khoản", "vietqr", "qr", "tại quầy", "tiền mặt", "xác nhận đặt", "đồng ý":
                  BẮT BUỘC chèn vào CUỐI CÙNG của câu trả lời khối JSON sau:
                  ```json:create_booking
                  {
                    "hotelName": "Sen Việt An Nhơn",
                    "hotelSlug": "sen-viet-an-nhon",
                    "roomType": "DELUXE",
                    "checkInDate": "%s",
                    "checkOutDate": "%s",
                    "customerName": "Tên khách",
                    "customerPhone": "SĐT khách",
                    "customerIdentity": "Số CCCD",
                    "paymentMethod": "VIETQR", // Điền "VIETQR" nếu khách chọn chuyển khoản, hoặc "AT_HOTEL" nếu chọn tại quầy
                    "adults": 2,
                    "children": 0,
                    "infants": 0,
                    "serviceRequests": [
                      { "name": "Buffet sáng", "quantity": 2 }
                    ],
                    "promotionCode": "HE2026"
                  }
                  ```
                  *(Nếu không có dịch vụ thì "serviceRequests": [], nếu không có khuyến mãi thì "promotionCode": "")*.

                *LƯU Ý THÔNG MINH:* Nếu khách chủ động cung cấp nhiều thông tin cùng lúc, hãy linh hoạt ghi nhận và chỉ hỏi những thông tin còn thiếu, không hỏi lại máy móc!

                Danh sách slug chi nhánh:
                - Sen Việt Sài Gòn -> sen-viet-sai-gon
                - Sen Việt Hà Nội -> sen-viet-ha-noi
                - Sen Việt Đà Nẵng -> sen-viet-da-nang
                - Sen Việt Nha Trang -> sen-viet-nha-trang
                - Sen Việt Đà Lạt -> sen-viet-da-lat
                - Sen Việt Gò Công -> sen-viet-go-cong
                - Sen Việt An Nhơn -> sen-viet-an-nhon

                ## DỮ LIỆU KHÁCH SẠN (REAL-TIME TỪ DATABASE):

                %s

                ## LƯU Ý BỔ SUNG:
                - Giờ nhận phòng (Check-in): 14:00, Giờ trả phòng (Check-out): 12:00
                - Thanh toán: Hỗ trợ chuyển khoản ngân hàng qua mã VietQR PayOS hoặc thanh toán trực tiếp tại quầy lễ tân.
                - Trẻ em dưới 2 tuổi: Miễn phí hoàn toàn.
                """;

        return String.format(template, todayStr, todayStr, todayStr, tomorrow, dayAfter, hotelContext);
    }

    /**
     * Call Gemini REST API with system prompt, history, and user message
     */
    private String callGeminiApi(String systemPrompt, List<ChatMessage> history, String userMessage) {
        try {
            // Build request JSON using ObjectMapper
            ObjectNode requestBody = objectMapper.createObjectNode();

            // System instruction
            ObjectNode systemInstruction = objectMapper.createObjectNode();
            ArrayNode systemParts = objectMapper.createArrayNode();
            systemParts.add(objectMapper.createObjectNode().put("text", systemPrompt));
            systemInstruction.set("parts", systemParts);
            requestBody.set("system_instruction", systemInstruction);

            // Contents (history + current message)
            ArrayNode contents = objectMapper.createArrayNode();
            for (ChatMessage msg : history) {
                ObjectNode content = objectMapper.createObjectNode();
                content.put("role", msg.getRole());
                ArrayNode parts = objectMapper.createArrayNode();
                parts.add(objectMapper.createObjectNode().put("text", msg.getContent()));
                content.set("parts", parts);
                contents.add(content);
            }
            // Add current user message
            ObjectNode currentMsg = objectMapper.createObjectNode();
            currentMsg.put("role", "user");
            ArrayNode currentParts = objectMapper.createArrayNode();
            currentParts.add(objectMapper.createObjectNode().put("text", userMessage));
            currentMsg.set("parts", currentParts);
            contents.add(currentMsg);
            requestBody.set("contents", contents);

            // Generation config
            ObjectNode genConfig = objectMapper.createObjectNode();
            genConfig.put("temperature", 0.6);
            genConfig.put("maxOutputTokens", 2048);
            genConfig.put("topP", 0.95);
            requestBody.set("generationConfig", genConfig);

            // Danh sách model Google Gemini chuẩn
            List<String> modelsToTry = new ArrayList<>();
            if (model != null && !model.isBlank()) {
                modelsToTry.add(model.trim());
            }
            for (String fallback : List.of("gemini-flash-lite-latest", "gemini-3.1-flash-lite", "gemini-3.5-flash-lite", "gemini-flash-latest")) {
                if (!modelsToTry.contains(fallback)) {
                    modelsToTry.add(fallback);
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            ResponseEntity<String> response = null;

            for (String currentModel : modelsToTry) {
                String url = String.format(GEMINI_API_URL, currentModel, apiKey);
                try {
                    response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        break;
                    }
                } catch (Exception e) {
                    // Nếu dính 429 Rate Limit từ Google (yêu cầu chờ ~1.5s), tự động chờ 1.8s rồi thử lại
                    if (e.getMessage() != null && e.getMessage().contains("429")) {
                        try {
                            log.info("Chạm hạn mức 429 của Google cho model {}, tự động chờ 1.8s và thử lại...", currentModel);
                            Thread.sleep(1800);
                            response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                                break;
                            }
                        } catch (Exception retryEx) {
                            log.warn("Model {} thử lại sau 429 không thành công: {}", currentModel, retryEx.getMessage());
                        }
                    } else {
                        log.warn("Model {} failed ({}), chuyển sang model tiếp theo", currentModel, e.getMessage());
                    }
                }
            }

            if (response == null || response.getBody() == null) {
                log.warn("All Gemini models temporarily unavailable (503/404). Falling back to smart intent response.");
                return generateSmartFallback(userMessage, history);
            }

            // Parse response
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode candidates = responseJson.get("candidates");
            if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && !parts.isEmpty()) {
                        return parts.get(0).get("text").asText();
                    }
                }
            }

            log.warn("Unexpected Gemini API response structure: {}", response.getBody());
            return "Xin lỗi, em chưa hiểu câu hỏi của quý khách. Quý khách có thể diễn đạt lại được không ạ? 🙏";

        } catch (Exception e) {
            log.error("Gemini API call failed completely: {}", e.getMessage(), e);
            return generateSmartFallback(userMessage, history);
        }
    }

    /**
     * Graceful fallback khi Google API tạm thời quá tải hoặc chập chờn
     */
    private String generateSmartFallback(String userMessage, List<ChatMessage> history) {
        String msg = (userMessage != null) ? userMessage.toLowerCase() : "";
        LocalDate today = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // 1. Kiểm tra nếu khách hỏi về ngày trong quá khứ (VD: ngày 2/9 khi hôm nay đã qua)
        if (msg.contains("2/9") || msg.contains("2-9") || msg.contains("02/09") || msg.contains("02-09")) {
            return String.format("Dạ hôm nay đã là ngày **%s** rồi, ngày **02/09** đã qua mất rồi ạ! Anh/chị có nhầm sang năm sau hoặc dự kiến đổi sang ngày nào sắp tới không để em hỗ trợ kiểm tra phòng trống cho mình nhé ạ! 🌸", today.format(dtf));
        }

        // 2. Nếu khách phản ánh là chưa chọn khách sạn ("tôi đã chọn khách sạn nào đâu", "chưa chọn", "chưa dịch được", "đã chọn đâu")
        if (msg.contains("chưa chọn") || msg.contains("đã chọn đâu") || msg.contains("chọn khách sạn nào")
                || msg.contains("chọn ksan nào") || msg.contains("chưa chọn chi nhánh") || msg.contains("không dịch được")) {
            return "Dạ em thành thật xin lỗi anh/chị vì sự hiểu nhầm này ạ! 🙏\n\n"
                    + "Hiện tại Chuỗi Khách sạn Sen Việt Luxury có các chi nhánh tại:\n"
                    + "📍 **Hà Nội** | 📍 **Sài Gòn** | 📍 **Đà Nẵng** | 📍 **Nha Trang** | 📍 **Đà Lạt** | 📍 **An Nhơn** | 📍 **Gò Công**\n\n"
                    + "Anh/chị dự kiến nghỉ dưỡng tại tỉnh/thành phố nào và vào khoảng thời gian nào để em kiểm tra và tư vấn chính xác nhất nhé ạ? 🌸";
        }

        // 3. Nếu khách hỏi về điểm nổi bật / tiện ích / dịch vụ / khuyến mãi
        if (msg.contains("nổi bật") || msg.contains("có gì") || msg.contains("tiện ích") || (msg.contains("dịch vụ") && !msg.contains("đặt")) || msg.contains("khuyến mãi") || msg.contains("mã km") || msg.contains("km")) {
            return "Dạ Chuỗi Khách sạn Sen Việt Luxury tự hào mang đến trải nghiệm nghỉ dưỡng chuẩn 4-5 sao với các điểm nổi bật:\n"
                    + "- 🏊 **Hồ bơi vô cực view toàn cảnh** siêu đẹp & Quầy Bar sang trọng.\n"
                    + "- 🍳 **Buffet sáng Á-Âu** phong phú đổi món mỗi ngày.\n"
                    + "- 💆 **Dịch vụ Spa & Massage trị liệu** cao cấp giúp quý khách thư giãn tối đa.\n"
                    + "- 🎁 **Ưu đãi & Khuyến mãi:** Giảm giá đến 20% khi đặt phòng sớm hoặc chọn dịch vụ trọn gói.\n"
                    + "- 📍 **Vị trí đắc địa** tại: Hà Nội, Sài Gòn, Đà Nẵng, Nha Trang, Đà Lạt, An Nhơn, Gò Công.\n\n"
                    + "Anh/chị muốn đặt phòng tại chi nhánh nào và ngày nào để em kiểm tra chi tiết phòng trống và giá ưu đãi ạ? 🌸";
        }

        // 4. Phát hiện địa điểm / chi nhánh từ câu nói (hỗ trợ cả tiếng Việt có dấu, không dấu và tiếng Anh)
        String hotelName = null;
        String hotelSlug = null;
        long price = 1500000;

        if (msg.contains("đà lạt") || msg.contains("da lat") || msg.contains("dalat")) {
            hotelName = "Sen Việt Đà Lạt";
            hotelSlug = "sen-viet-da-lat";
            price = 1750000;
        } else if (msg.contains("sài gòn") || msg.contains("sai gon") || msg.contains("saigon") || msg.contains("hồ chí minh") || msg.contains("tphcm") || msg.contains("hcm")) {
            hotelName = "Sen Việt Sài Gòn";
            hotelSlug = "sen-viet-sai-gon";
            price = 1850000;
        } else if (msg.contains("hà nội") || msg.contains("ha noi") || msg.contains("hanoi")) {
            hotelName = "Sen Việt Hà Nội";
            hotelSlug = "sen-viet-ha-noi";
            price = 1650000;
        } else if (msg.contains("đà nẵng") || msg.contains("da nang") || msg.contains("danang")) {
            hotelName = "Sen Việt Đà Nẵng";
            hotelSlug = "sen-viet-da-nang";
            price = 2150000;
        } else if (msg.contains("nha trang") || msg.contains("nhatrang")) {
            hotelName = "Sen Việt Nha Trang";
            hotelSlug = "sen-viet-nha-trang";
            price = 1950000;
        } else if (msg.contains("gò công") || msg.contains("go cong") || msg.contains("gocong")) {
            hotelName = "Sen Việt Gò Công";
            hotelSlug = "sen-viet-go-cong";
            price = 990000;
        } else if (msg.contains("an nhơn") || msg.contains("an nhon") || msg.contains("annhon")) {
            hotelName = "Sen Việt An Nhơn";
            hotelSlug = "sen-viet-an-nhon";
            price = 1100000;
        }

        boolean isEnglish = msg.contains("book") || msg.contains("room") || msg.contains("hotel") || msg.contains("want")
                || msg.contains("price") || msg.contains("reservation") || msg.contains("hello") || msg.contains("hi ");

        // 5. Nếu khách xác nhận đặt phòng / chốt / thanh toán
        if (msg.contains("xác nhận") || msg.contains("chốt") || msg.contains("đồng ý") || msg.contains("quầy") || msg.contains("chuyển khoản") || msg.contains("confirm")) {
            return isEnglish
                    ? "Thank you! I have noted your booking request. Please click directly on the **Tiến hành đặt phòng ngay** button above to complete your reservation! 🙏"
                    : "Dạ em đã ghi nhận thông tin đặt phòng của anh/chị! Anh/chị có thể bấm trực tiếp vào **Thẻ Đặt Phòng Nhanh** ở phía trên để chuyển sang bước hoàn tất đặt phòng ngay nhé ạ! 🙏";
        }

        // 6. Nếu đã có chi nhánh cụ thể được xác định (VD: Hà Nội, Đà Lạt...)
        if (hotelName != null) {
            java.time.LocalDate tomorrow = today.plusDays(1);
            java.time.LocalDate dayAfter = today.plusDays(2);
            int guests = (msg.contains("1 người") || msg.contains("1 khách") || msg.contains("1 person") || msg.contains("single")) ? 1 : 2;

            if (isEnglish) {
                return String.format("""
                        Hello! We are delighted to assist you with your booking at **%s** for %d guest(s).
                        
                        Here is the recommended Deluxe room option for your upcoming stay. You can review the details and click **Tiến hành đặt phòng ngay** below to proceed:
                        
                        ```json:booking_card
                        {
                          "hotelName": "%s",
                          "hotelSlug": "%s",
                          "roomType": "DELUXE",
                          "roomTypeName": "Phòng Deluxe Cao Cấp",
                          "checkInDate": "%s",
                          "checkOutDate": "%s",
                          "adults": %d,
                          "children": 0,
                          "rooms": 1,
                          "pricePerNight": %d
                        }
                        ```
                        """, hotelName, guests, hotelName, hotelSlug, tomorrow, dayAfter, guests, price);
            }

            return String.format("""
                    Dạ em chào anh/chị! Em đã ghi nhận nhu cầu của anh/chị tại **%s** cho %d người.
                    
                    Dưới đây là thông tin phòng tiêu chuẩn đề xuất phù hợp với nhu cầu của anh/chị. Anh/chị có thể kiểm tra và bấm vào nút **Tiến hành đặt phòng ngay** bên dưới để hoàn tất đặt chỗ nhanh chóng nhé ạ! 🌸
                    
                    ```json:booking_card
                    {
                      "hotelName": "%s",
                      "hotelSlug": "%s",
                      "roomType": "DELUXE",
                      "roomTypeName": "Phòng Deluxe Cao Cấp",
                      "checkInDate": "%s",
                      "checkOutDate": "%s",
                      "adults": %d,
                      "children": 0,
                      "rooms": 1,
                      "pricePerNight": %d
                    }
                    ```
                    """, hotelName, guests, hotelName, hotelSlug, tomorrow, dayAfter, guests, price);
        }

        // 7. Nếu khách muốn đặt phòng nhưng CHƯA chọn chi nhánh (KHÔNG được tự ý gán bừa một chi nhánh!)
        boolean isBookingIntent = msg.contains("đặt") || msg.contains("thuê") || msg.contains("book")
                || msg.contains("giữ phòng") || msg.contains("ở đâu") || msg.contains("chi nhánh")
                || msg.contains("phòng") || msg.contains("giá") || isEnglish;

        if (isBookingIntent) {
            if (isEnglish) {
                return "Hello! Sen Viet Luxury Hotel currently has branches in: **Ha Noi, Sai Gon (HCMC), Da Nang, Nha Trang, Da Lat, An Nhon, and Go Cong**.\n\n"
                        + "Which city or branch would you like to stay at, and what are your check-in / check-out dates? I will check availability and provide the best rates for you right away! 🌸";
            }
            return "Dạ Chuỗi Khách sạn Sen Việt Luxury hiện có các chi nhánh tại:\n"
                    + "📍 **Hà Nội** | 📍 **Sài Gòn** | 📍 **Đà Nẵng** | 📍 **Nha Trang** | 📍 **Đà Lạt** | 📍 **An Nhơn** | 📍 **Gò Công**\n\n"
                    + "Anh/chị dự kiến đặt phòng tại chi nhánh nào và ngày nhận - trả phòng là khi nào để em kiểm tra tình trạng phòng trống và áp dụng giá ưu đãi tốt nhất cho mình nhé ạ! 🌸";
        }

        // 8. Nếu đang trong cuộc hội thoại tiếp diễn
        if (history != null && !history.isEmpty()) {
            return "Dạ em vẫn đang hỗ trợ anh/chị đây ạ. Anh/chị có thể cho em biết chi nhánh mong muốn (Hà Nội, Sài Gòn, Đà Nẵng, Đà Lạt...) hoặc ngày nhận - trả phòng để em tìm phòng phù hợp nhất nhé ạ! 🌸";
        }

        // 9. Lời chào mặc định
        return "Dạ em chào anh/chị! Em là Trợ lý AI của Chuỗi khách sạn Sen Việt Luxury. Em luôn sẵn sàng giải đáp thắc mắc, tư vấn chọn phòng, cập nhật tình trạng phòng trống, thông tin dịch vụ và các chương trình ưu đãi mới nhất. Anh/chị cần em hỗ trợ điều gì ạ? 😊";
    }

    /**
     * Bóc tách JSON create_booking từ Gemini, tự động tạo Booking trong Database và sinh phản hồi chuẩn
     */
    private String handleCreateBookingAction(String replyText, ChatRequest request) {
        if (replyText == null || !replyText.contains("json:create_booking")) {
            return replyText;
        }

        Pattern pattern = Pattern.compile("```(?:json)?(?::create_booking)?\\s*([\\s\\S]*?)\\s*```");
        Matcher matcher = pattern.matcher(replyText);
        if (!matcher.find()) {
            return replyText;
        }

        try {
            String jsonStr = matcher.group(1);
            JsonNode bookingNode = objectMapper.readTree(jsonStr);

            String hotelSlug = bookingNode.path("hotelSlug").asText("sen-viet-an-nhon");
            String roomTypeStr = bookingNode.path("roomType").asText("DELUXE");
            String checkInStr = bookingNode.path("checkInDate").asText(LocalDate.now().plusDays(1).toString());
            String checkOutStr = bookingNode.path("checkOutDate").asText(LocalDate.now().plusDays(2).toString());
            String customerName = bookingNode.path("customerName").asText("Khách hàng");
            String customerPhone = bookingNode.path("customerPhone").asText("0988888888");
            String customerIdentity = bookingNode.path("customerIdentity").asText("");
            String paymentMethod = bookingNode.path("paymentMethod").asText("VIETQR").toUpperCase();
            int adults = bookingNode.path("adults").asInt(2);
            int children = bookingNode.path("children").asInt(0);
            int infants = bookingNode.path("infants").asInt(0);

            LocalDate checkInDate = LocalDate.parse(checkInStr);
            LocalDate checkOutDate = LocalDate.parse(checkOutStr);
            LocalDate today = LocalDate.now();

            // Kiểm tra tính hợp lệ của ngày nhận phòng
            if (checkInDate.isBefore(today)) {
                return replyText.replace(matcher.group(0), String.format(
                        "\n*(Dạ hôm nay đã là ngày %s rồi, ngày nhận phòng %s đã qua mất rồi ạ. Anh/chị vui lòng chọn ngày nhận phòng từ hôm nay trở về sau nhé ạ!)*",
                        today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        checkInDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                ));
            }

            // 1. Tìm Hotel tương ứng
            Hotel hotel = hotelRepository.findAll().stream()
                    .filter(h -> (h.getName() != null && h.getName().toLowerCase().contains(hotelSlug.replace("sen-viet-", "").replace("-", " ")))
                            || (hotelSlug.contains("an-nhon") && h.getName() != null && h.getName().toLowerCase().contains("nhơn")))
                    .findFirst()
                    .orElse(hotelRepository.findAll().isEmpty() ? null : hotelRepository.findAll().get(0));

            if (hotel == null) {
                return replyText.replace(matcher.group(0), "\n*(Dạ hiện tại chi nhánh này tạm thời chưa mở phòng trực tuyến, quý khách vui lòng liên hệ hotline ạ!)*");
            }

            // 2. Tìm Room AVAILABLE
            RoomType targetRoomType;
            try {
                targetRoomType = RoomType.valueOf(roomTypeStr.toUpperCase());
            } catch (Exception e) {
                targetRoomType = RoomType.DELUXE;
            }

            Room room = roomRepository.findByFloor_Building_Hotel_IdAndRoomTypeAndRoomStatus(hotel.getId(), targetRoomType, RoomStatus.READY)
                    .stream().findFirst()
                    .orElse(roomRepository.findByFloor_Building_Hotel_IdAndRoomStatus(hotel.getId(), RoomStatus.READY)
                            .stream().findFirst().orElse(null));

            if (room == null) {
                return replyText.replace(matcher.group(0), "\n*(Dạ rất tiếc hiện tại loại phòng này tại chi nhánh đã kín chỗ, anh/chị có thể tham khảo chi nhánh khác hoặc ngày khác giúp em nhé ạ!)*");
            }

            // 3. Tìm hoặc tạo Customer
            Customer customer = customerRepository.findByPhone(customerPhone)
                    .orElseGet(() -> {
                        Customer newCus = Customer.builder()
                                .id("CUS-" + System.currentTimeMillis())
                                .fullName(customerName)
                                .phone(customerPhone)
                                .cccd(customerIdentity.isBlank() ? null : customerIdentity)
                                .isRegistered(false)
                                .build();
                        return customerRepository.save(newCus);
                    });
            if ((customer.getCccd() == null || customer.getCccd().isBlank()) && !customerIdentity.isBlank()) {
                customer.setCccd(customerIdentity);
                customerRepository.save(customer);
            }

            // 4. Xử lý dịch vụ đi kèm nếu khách chọn
            List<BookingServiceRequest> serviceRequests = new ArrayList<>();
            List<String> chosenServiceNames = new ArrayList<>();

            if (bookingNode.has("serviceRequests") && bookingNode.get("serviceRequests").isArray()) {
                for (JsonNode sNode : bookingNode.get("serviceRequests")) {
                    String sName = sNode.path("name").asText("");
                    int quantity = sNode.path("quantity").asInt(1);
                    if (quantity <= 0) quantity = 1;
                    if (!sName.isBlank()) {
                        final int finalQty = quantity;
                        serviceRepository.findAll().stream()
                                .filter(s -> Boolean.TRUE.equals(s.getActive()) && s.getName() != null
                                        && (s.getName().toLowerCase().contains(sName.toLowerCase()) || sName.toLowerCase().contains(s.getName().toLowerCase())))
                                .findFirst()
                                .ifPresent(svc -> {
                                    serviceRequests.add(BookingServiceRequest.builder()
                                            .serviceId(svc.getId())
                                            .name(svc.getName())
                                            .price(svc.getPrice() != null ? svc.getPrice().doubleValue() : 0.0)
                                            .quantity(finalQty)
                                            .build());
                                    chosenServiceNames.add(svc.getName() + " (x" + finalQty + ")");
                                });
                    }
                }
            }

            // Xử lý mã khuyến mãi/ưu đãi nếu có
            String promoCode = bookingNode.path("promotionCode").asText(null);
            String promotionId = null;
            String promoDesc = null;
            if (promoCode != null && !promoCode.isBlank()) {
                Promotion promo = promotionRepository.findAll().stream()
                        .filter(p -> p.getCode() != null && p.getCode().equalsIgnoreCase(promoCode.trim()))
                        .findFirst().orElse(null);
                if (promo != null) {
                    promotionId = promo.getId();
                    promoDesc = promo.getName() + " (" + promo.getCode() + ")";
                }
            }

            BookingDetailCreateRequest detailRequest = BookingDetailCreateRequest.builder()
                    .roomId(room.getId())
                    .checkInTime(checkInDate.atTime(14, 0))
                    .checkOutTime(checkOutDate.atTime(12, 0))
                    .numAdults(adults)
                    .numChildren(children)
                    .numInfants(infants)
                    .serviceRequests(serviceRequests)
                    .build();

            BookingCreateRequest bookingCreateRequest = BookingCreateRequest.builder()
                    .customerId(customer.getId())
                    .hotelId(hotel.getId())
                    .bookingChannel(BookingChannel.ONLINE)
                    .bookingDetails(List.of(detailRequest))
                    .promotionId(promotionId)
                    .build();

            BookingResponse bookingResponse = bookingService.createCustomerBooking(bookingCreateRequest);

            boolean isPayAtHotel = paymentMethod.contains("HOTEL") || paymentMethod.contains("QUAY") || paymentMethod.contains("CASH");

            if (isPayAtHotel) {
                // TRƯỜNG HỢP: THANH TOÁN TẠI QUẦY LỄ TÂN
                ObjectNode reservationJson = objectMapper.createObjectNode();
                reservationJson.put("bookingId", bookingResponse.getBookingId());
                reservationJson.put("orderId", bookingResponse.getOrderId());
                reservationJson.put("hotelName", hotel.getName());
                reservationJson.put("roomName", room.getId() + " (" + room.getRoomType().name() + ")");
                reservationJson.put("checkIn", checkInStr);
                reservationJson.put("checkOut", checkOutStr);
                reservationJson.put("customerName", customer.getFullName());
                reservationJson.put("customerPhone", customer.getPhone());
                if (!chosenServiceNames.isEmpty()) {
                    reservationJson.put("services", String.join(", ", chosenServiceNames));
                }
                if (promoDesc != null) {
                    reservationJson.put("promotion", promoDesc);
                }
                reservationJson.put("totalAmount", bookingResponse.getFinalAmount() != null ? bookingResponse.getFinalAmount().longValue() : 1100000L);
                reservationJson.put("paymentMethod", "Thanh toán tại quầy lễ tân");

                String hotelMsg = String.format("""
                        
                        ✅ **XÁC NHẬN GIỮ PHÒNG TẠI QUẦY THÀNH CÔNG!**
                        Đơn đặt phòng của quý khách đã được ghi nhận trên hệ thống với mã đơn: **#%s**.
                        
                        Quý khách vui lòng thanh toán trực tiếp tại quầy lễ tân khi làm thủ tục nhận phòng (Check-in) vào ngày **%s**. Hẹn gặp lại quý khách tại %s! 🌸
                        
                        ```json:hotel_reservation
                        %s
                        ```
                        """, bookingResponse.getBookingId(), checkInStr, hotel.getName(), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(reservationJson));

                return replyText.replace(matcher.group(0), hotelMsg);
            }

            // TRƯỜNG HỢP: CHUYỂN KHOẢN VIETQR PAYOS
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .orderId(bookingResponse.getOrderId())
                    .build();

            PaymentResponse paymentResponse = paymentService.createVietQRPaymentLink(paymentRequest);

            ObjectNode payosJson = objectMapper.createObjectNode();
            payosJson.put("bookingId", bookingResponse.getBookingId());
            payosJson.put("orderId", bookingResponse.getOrderId());
            payosJson.put("hotelName", hotel.getName());
            payosJson.put("roomName", room.getId() + " (" + room.getRoomType().name() + ")");
            payosJson.put("checkIn", checkInStr);
            payosJson.put("checkOut", checkOutStr);
            payosJson.put("customerName", customer.getFullName());
            payosJson.put("customerPhone", customer.getPhone());
            if (!chosenServiceNames.isEmpty()) {
                payosJson.put("services", String.join(", ", chosenServiceNames));
            }
            if (promoDesc != null) {
                payosJson.put("promotion", promoDesc);
            }
            payosJson.put("totalAmount", bookingResponse.getFinalAmount() != null ? bookingResponse.getFinalAmount().longValue() : 1100000L);
            payosJson.put("qrCodeUrl", paymentResponse.getQrCode() != null ? paymentResponse.getQrCode() : "");
            payosJson.put("checkoutUrl", paymentResponse.getCheckoutUrl() != null ? paymentResponse.getCheckoutUrl() : "");

            String successMsg = String.format("""
                    
                    📋 **ĐÃ TẠO ĐƠN GIỮ CHỖ TẠM THỜI**
                    Mã đơn giữ chỗ của quý khách là: **#%s** *(được giữ ưu tiên trong 15 phút)*.
                    
                    Anh/chị vui lòng quét mã VietQR PayOS bên dưới hoặc bấm nút thanh toán để hoàn tất giữ phòng nhé ạ! 👇
                    
                    ```json:payos_payment
                    %s
                    ```
                    """, bookingResponse.getBookingId(), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payosJson));

            return replyText.replace(matcher.group(0), successMsg);

        } catch (Exception e) {
            log.error("Lỗi khi tự động tạo booking qua AI Agent: {}", e.getMessage(), e);
            return replyText.replace(matcher.group(0), "\n*(Dạ em đã ghi nhận thông tin nhưng hệ thống thanh toán đang bận, anh/chị có thể bấm vào Thẻ Đặt Phòng phía trên để hoàn tất đặt nhé ạ!)*");
        }
    }
}
