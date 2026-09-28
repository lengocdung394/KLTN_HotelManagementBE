-- ============================================================================
-- KHOA LUAN TOT NGHIEP - HE THONG QUAN LY CHUOI KHACH SAN SEN VIET
-- SCRIPT DU LIEU MAU TOAN DIEN (TEST LUONG NHAN VIEN, LE TAN, BUONG PHONG, QUAN LY)
-- ============================================================================
-- File: sample_data_test.sql
-- Thoi diem chay: Chay truc tiep tren PostgreSQL (Database: hotelmanagement)
-- ============================================================================
-- DANH SACH KHACH SAN DONG BO 100% THEO GIAO DIEN UI CUA HE THONG:
-- Miền Bắc:   Sen Việt Hà Nội
-- Miền Trung: Sen Việt An Nhơn, Sen Việt Đà Nẵng, Sen Việt Nha Trang
-- Miền Nam:   Sen Việt Gò Công, Sen Việt Sài Gòn
-- (Kèm thêm:  Sen Việt Đà Lạt)
-- ============================================================================

BEGIN;

-- ============================================================================
-- 1. TINH / THANH PHO (PROVINCES)
-- ============================================================================
INSERT INTO provinces (id, name) VALUES
(1, 'TP. Hồ Chí Minh'),
(2, 'Hà Nội'),
(3, 'Đà Nẵng'),
(4, 'Khánh Hòa'),
(5, 'Lâm Đồng'),
(6, 'Tiền Giang'),
(7, 'Bình Định')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;

-- ============================================================================
-- 2. KHACH SAN & CHI NHANH (HOTELS) - DUNG CHUAN TEN THEO GIAO DIEN UI
-- ============================================================================
INSERT INTO hotels (id, name, address, phone, province_id) VALUES
(1, 'Sen Việt Sài Gòn', '123 Lê Lợi, Quận 1, TP. Hồ Chí Minh', '0283999999', 1),
(2, 'Sen Việt Hà Nội', '45 Tràng Tiền, Hoàn Kiếm, Hà Nội', '0243888888', 2),
(3, 'Sen Việt Đà Nẵng', 'Võ Nguyên Giáp, Ngũ Hành Sơn, Đà Nẵng', '0236399999', 3),
(4, 'Sen Việt Nha Trang', 'Trần Phú, Lộc Thọ, Nha Trang, Khánh Hòa', '0258388888', 4),
(5, 'Sen Việt Đà Lạt', 'Trần Hưng Đạo, Phường 10, Đà Lạt, Lâm Đồng', '0263377777', 5),
(6, 'Sen Việt Gò Công', 'Đường Nguyễn Trãi, TX. Gò Công, Tiền Giang', '0273366666', 6),
(7, 'Sen Việt An Nhơn', 'Đường Quang Trung, TX. An Nhơn, Bình Định', '0256355555', 7)
ON CONFLICT (id) DO UPDATE SET 
    name = EXCLUDED.name, 
    address = EXCLUDED.address, 
    phone = EXCLUDED.phone, 
    province_id = EXCLUDED.province_id;

-- Xóa bỏ các dòng trùng id >= 8 nếu có
DELETE FROM hotels WHERE id > 7;
DELETE FROM provinces WHERE id > 7;

-- ============================================================================
-- 3. TOA NHA & TANG (BUILDINGS & FLOORS)
-- ============================================================================
INSERT INTO buildings (id, name, hotel_id) VALUES
(1, 'Mercury Tower - Sài Gòn', 1),
(2, 'Tòa Hoàn Kiếm - Hà Nội', 2),
(3, 'Tòa Sóng Biển - Đà Nẵng', 3),
(4, 'Tòa Ngọc Biển - Nha Trang', 4),
(5, 'Tòa Thông Reo - Đà Lạt', 5),
(6, 'Tòa Phù Sa - Gò Công', 6),
(7, 'Tòa Tháp Chàm - An Nhơn', 7)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, hotel_id = EXCLUDED.hotel_id;

DELETE FROM buildings WHERE id > 7;

INSERT INTO floors (id, floor_number, building_id) VALUES
(1, 1, 1),
(2, 2, 1),
(3, 1, 2),
(4, 2, 2),
(5, 1, 3),
(6, 2, 3),
(7, 1, 4),
(8, 2, 4),
(9, 1, 5),
(10, 2, 5),
(11, 1, 6),
(12, 2, 6),
(13, 1, 7),
(14, 2, 7)
ON CONFLICT (id) DO UPDATE SET floor_number = EXCLUDED.floor_number, building_id = EXCLUDED.building_id;

DELETE FROM floors WHERE id > 14;

-- ============================================================================
-- 4. CHINH SACH PHONG CHI NHANH (BRANCH_ROOM_POLICIES) - 7 KHACH SAN x 4 LOAI PHONG
-- ============================================================================
INSERT INTO branch_room_policies (id, hotel_id, room_type, standard_capacity, max_extra_guests, extra_adult_fee, extra_child_fee, base_price) VALUES
-- Hotel 1: Sài Gòn
(1, 1, 'STANDARD', 2, 2, 200000.0, 100000.0, 1000000.0),
(2, 1, 'DELUXE',   2, 2, 250000.0, 120000.0, 2000000.0),
(3, 1, 'SUITE',    3, 3, 350000.0, 150000.0, 3000000.0),
(4, 1, 'FAMILY',   4, 4, 400000.0, 200000.0, 3500000.0),
-- Hotel 2: Hà Nội
(5, 2, 'STANDARD', 2, 2, 200000.0, 100000.0, 1000000.0),
(6, 2, 'DELUXE',   2, 2, 250000.0, 120000.0, 2000000.0),
(7, 2, 'SUITE',    3, 3, 350000.0, 150000.0, 3000000.0),
(8, 2, 'FAMILY',   4, 4, 400000.0, 200000.0, 3500000.0),
-- Hotel 3: Đà Nẵng
(9,  3, 'STANDARD', 2, 2, 200000.0, 100000.0, 1200000.0),
(10, 3, 'DELUXE',   2, 2, 250000.0, 120000.0, 2200000.0),
(11, 3, 'SUITE',    3, 3, 350000.0, 150000.0, 3400000.0),
(12, 3, 'FAMILY',   4, 4, 400000.0, 200000.0, 3800000.0),
-- Hotel 4: Nha Trang
(13, 4, 'STANDARD', 2, 2, 200000.0, 100000.0, 1100000.0),
(14, 4, 'DELUXE',   2, 2, 250000.0, 120000.0, 2100000.0),
(15, 4, 'SUITE',    3, 3, 350000.0, 150000.0, 3200000.0),
(16, 4, 'FAMILY',   4, 4, 400000.0, 200000.0, 3600000.0),
-- Hotel 5: Đà Lạt
(17, 5, 'STANDARD', 2, 2, 200000.0, 100000.0, 950000.0),
(18, 5, 'DELUXE',   2, 2, 250000.0, 120000.0, 1850000.0),
(19, 5, 'SUITE',    3, 3, 350000.0, 150000.0, 2900000.0),
(20, 5, 'FAMILY',   4, 4, 400000.0, 200000.0, 3400000.0),
-- Hotel 6: Gò Công
(21, 6, 'STANDARD', 2, 2, 150000.0, 80000.0,  800000.0),
(22, 6, 'DELUXE',   2, 2, 200000.0, 100000.0, 1400000.0),
(23, 6, 'SUITE',    3, 3, 300000.0, 120000.0, 2200000.0),
(24, 6, 'FAMILY',   4, 4, 350000.0, 150000.0, 2600000.0),
-- Hotel 7: An Nhơn
(25, 7, 'STANDARD', 2, 2, 150000.0, 80000.0,  850000.0),
(26, 7, 'DELUXE',   2, 2, 200000.0, 100000.0, 1500000.0),
(27, 7, 'SUITE',    3, 3, 300000.0, 120000.0, 2400000.0),
(28, 7, 'FAMILY',   4, 4, 350000.0, 150000.0, 2800000.0)
ON CONFLICT (hotel_id, room_type) DO UPDATE SET
    standard_capacity = EXCLUDED.standard_capacity,
    max_extra_guests = EXCLUDED.max_extra_guests,
    extra_adult_fee = EXCLUDED.extra_adult_fee,
    extra_child_fee = EXCLUDED.extra_child_fee,
    base_price = EXCLUDED.base_price;

DELETE FROM branch_room_policies WHERE hotel_id > 7;

-- ============================================================================
-- 5. TIEN NGHI (AMENITIES) - GIỮ NGUYÊN 100% 8 TIỆN NGHI GỐC
-- ============================================================================
INSERT INTO amenities (amenity_id, name, price) VALUES
(1, 'Wifi 6 High-Speed 500Mbps', 0.0),
(2, 'Smart TV 55 inch 4K Ultra HD', 0.0),
(3, 'Bồn tắm nằm thảo dược cao cấp', 50000.0),
(4, 'Ban công view toàn cảnh thành phố', 0.0),
(5, 'Két sắt điện tử bảo mật cao', 0.0),
(6, 'Máy pha cà phê Espresso tự động', 40000.0),
(7, 'Tủ lạnh Minibar tiêu chuẩn', 0.0),
(8, 'Máy sấy tóc ion âm & Bàn là hơi', 0.0)
ON CONFLICT (amenity_id) DO UPDATE SET 
    name = EXCLUDED.name, 
    price = EXCLUDED.price;

DELETE FROM room_amenities WHERE amenity_id > 8;
DELETE FROM amenities WHERE amenity_id > 8;

-- ============================================================================
-- 6. DANH MUC DICH VU KHACH SAN (SERVICES) - 15 DICH VU
-- ============================================================================
INSERT INTO services (service_id, name, description, price, unit, category, active, image_url, hotel_id) VALUES
(1, 'Buffet sáng đặc sản Sen Việt', 'Buffet hơn 50 món Âu - Á kết hợp đặc sản 3 miền, nước ép trái cây nhiệt đới tươi mới', 150000.0, 'suất', 'Ẩm thực & Nhà hàng', true, 'https://images.unsplash.com/photo-1533089860892-a7c6f0a88666?q=80&w=800', NULL),
(2, 'Bữa tối Set Menu lãng mạn Rooftop', 'Bữa tối 5 món fine-dining kèm 2 ly vang đỏ tại tầng thượng ngắm hoàng hôn lung linh', 650000.0, 'set', 'Ẩm thực & Nhà hàng', true, 'https://images.unsplash.com/photo-1544025162-d76694265947?q=80&w=800', NULL),
(3, 'Massage trị liệu thảo dược toàn thân', 'Massage bấm huyệt cổ truyền 60 phút với tinh dầu hoa sen độc quyền và đá nóng', 350000.0, 'suất', 'Spa & Chăm sóc sức khỏe', true, 'https://images.unsplash.com/photo-1540555700478-4be289fbecef?q=80&w=800', NULL),
(4, 'Xông hơi đá muối Himalaya giải độc', 'Xông hơi khô kết hợp ion âm từ đá muối Himalaya giúp thanh lọc cơ thể và thư giãn sâu (45 phút)', 200000.0, 'lượt', 'Spa & Chăm sóc sức khỏe', true, 'https://images.unsplash.com/photo-1515377905703-c4788e51af15?q=80&w=800', NULL),
(5, 'Xe riêng đón/tiễn sân bay (Sedan 4 chỗ)', 'Xe sedan đời mới đưa đón tận sảnh, tài xế lịch thiệp, phục vụ nước suối và khăn lạnh', 350000.0, 'chuyến', 'Vận chuyển & Đưa đón', true, 'https://images.unsplash.com/photo-1549317661-bd32c8ce0db2?q=80&w=800', NULL),
(6, 'Xe riêng đón/tiễn sân bay (SUV/MPV 7 chỗ)', 'Xe cao cấp 7 chỗ khoang hành lý rộng rãi, phù hợp gia đình hoặc nhóm bạn nhiều vali', 500000.0, 'chuyến', 'Vận chuyển & Đưa đón', true, 'https://images.unsplash.com/photo-1550355291-bbee04a92027?q=80&w=800', NULL),
(7, 'Thuê xe máy tay ga (AirBlade/Vision)', 'Bao gồm 2 nón bảo hiểm tiêu chuẩn, 1 lít xăng và bản đồ du lịch địa phương bỏ túi', 150000.0, 'ngày', 'Vận chuyển & Đưa đón', true, 'https://images.unsplash.com/photo-1558981403-c5f9899a28bc?q=80&w=800', NULL),
(8, 'Giặt sấy lấy liền cao cấp trong 4 giờ', 'Giặt sạch khử khuẩn, sấy thơm, ủi phẳng phiu và gấp gọn giao tận phòng ngủ', 50000.0, 'kg', 'Giặt là & Tiện ích', true, 'https://images.unsplash.com/photo-1517677208171-0bc6725a3e60?q=80&w=800', NULL),
(9, 'Bia thủ công Sen Việt Craft Beer', 'Bia thủ công ủ men đặc biệt với hương lúa mạch thượng hạng và hoa bia thơm mát', 45000.0, 'lon', 'Minibar phòng', true, 'https://images.unsplash.com/photo-1608270119365-1d4a04d3c33d?q=80&w=800', NULL),
(10, 'Nước khoáng thiên nhiên chai thủy tinh', 'Nước khoáng tự nhiên tinh khiết đóng chai thủy tinh cao cấp thân thiện môi trường', 25000.0, 'chai', 'Minibar phòng', true, 'https://images.unsplash.com/photo-1548839140-29a749e1bc4e?q=80&w=800', NULL),
(11, 'Tiệc trà chiều Hoàng Gia kiểu Anh (Afternoon Tea)', 'Set bánh ngọt macaron, scone, tart trái cây kèm trà Bá Tước thơm lừng ngắm cảnh', 320000.0, 'set', 'Ẩm thực & Nhà hàng', true, 'https://images.unsplash.com/photo-1576092768241-dec231879fc3?q=80&w=800', NULL),
(12, 'Rượu vang đỏ Chile Reserva hảo hạng', 'Chai vang đỏ Cabernet Sauvignon đậm đà hương trái mọng chín, phục vụ kèm ly pha lê', 750000.0, 'chai', 'Minibar phòng', true, 'https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?q=80&w=800', NULL),
(13, 'Tour du thuyền ngắm hoàng hôn lãng mạn', 'Trải nghiệm du thuyền 2 tiếng ngắm hoàng hôn, phục vụ finger food và cocktail đặc sắc', 850000.0, 'người', 'Tour & Trải nghiệm', true, 'https://images.unsplash.com/photo-1544551763-46a013bb70d5?q=80&w=800', NULL),
(14, 'Dịch vụ trang trí phòng trăng mật / kỷ niệm', 'Rải cánh hoa hồng tự nhiên xếp hình trái tim, nến thơm cao cấp và giỏ hoa quả tươi', 450000.0, 'gói', 'Dịch vụ đặc biệt', true, 'https://images.unsplash.com/photo-1519741497674-611481863552?q=80&w=800', NULL),
(15, 'Hướng dẫn viên du lịch bản địa riêng', 'Hướng dẫn viên am hiểu lịch sử, ẩm thực và văn hóa đồng hành suốt nửa ngày (4 tiếng)', 500000.0, 'buổi', 'Tour & Trải nghiệm', true, 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?q=80&w=800', NULL)
ON CONFLICT (service_id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    price = EXCLUDED.price,
    unit = EXCLUDED.unit,
    category = EXCLUDED.category,
    active = EXCLUDED.active,
    image_url = EXCLUDED.image_url;

-- ============================================================================
-- 7. CHUONG TRINH KHUYEN MAI (PROMOTIONS) - HIEU LUC 01/09/2026 DEN 31/12/2026
-- ============================================================================
INSERT INTO promotions (id, code, name, description, type, discount_value, max_discount_amount, min_booking_value, start_date, end_date, usage_limit, used_count, status, deleted, is_exclusive, hotel_id, image_url, created_at, updated_at) VALUES
(1, 'CHAOMUNG2026', 'Ưu đãi Khách hàng mới 2026', 'Giảm ngay 10% trên tổng giá trị hóa đơn đặt phòng và dịch vụ cho khách lần đầu trải nghiệm', 'TOTAL_PERCENTAGE', 10.0, 300000.0, 500000.0, '2026-09-01 00:00:00', '2026-12-31 23:59:59', 500, 12, 'ACTIVE', false, false, NULL, 'https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?q=80&w=800', NOW(), NOW()),
(2, 'VIPROOM20', 'Tri ân Thành viên Sen Việt Luxury', 'Giảm 20% tiền phòng cho hội viên đặt phòng trực tuyến qua website Sen Việt', 'ROOM_PERCENTAGE', 20.0, 500000.0, 1000000.0, '2026-09-01 00:00:00', '2026-12-31 23:59:59', 200, 35, 'ACTIVE', false, false, NULL, 'https://images.unsplash.com/photo-1590381105924-c72589b9ef3f?q=80&w=800', NOW(), NOW()),
(3, 'SPALOVER15', 'Ưu đãi Thư giãn Spa & Sức khỏe', 'Giảm 15% tất cả các liệu trình Spa, Xông hơi thảo dược đá muối tại khách sạn', 'SERVICE_PERCENTAGE', 15.0, 200000.0, 300000.0, '2026-09-01 00:00:00', '2026-12-31 23:59:59', 300, 8, 'ACTIVE', false, false, NULL, 'https://images.unsplash.com/photo-1540555700478-4be289fbecef?q=80&w=800', NOW(), NOW()),
(4, 'THUVANG2026', 'Mùa Thu Vàng Rực Rỡ 2026', 'Giảm ngay 15% tiền phòng mùa lá đỏ, áp dụng cho mọi chi nhánh Sen Việt trên toàn quốc', 'ROOM_PERCENTAGE', 15.0, 400000.0, 800000.0, '2026-09-01 00:00:00', '2026-12-31 23:59:59', 400, 18, 'ACTIVE', false, false, NULL, 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=800', NOW(), NOW()),
(5, 'GIANHOM30', 'Kỳ nghỉ Vui vẻ Nhóm & Gia đình', 'Giảm trực tiếp 300.000 VNĐ cho hóa đơn đặt phòng từ 2 phòng trở lên hoặc phòng Suite/Family', 'FIXED_AMOUNT', 300000.0, 300000.0, 2500000.0, '2026-09-01 00:00:00', '2026-12-31 23:59:59', 150, 9, 'ACTIVE', false, false, NULL, 'https://images.unsplash.com/photo-1511632765486-a01980e01a18?q=80&w=800', NOW(), NOW()),
(6, 'EARLYBIRD12', 'Đặt sớm Giữ phòng Đẹp - Rẻ hơn 12%', 'Ưu đãi giảm 12% tổng tiền khi quý khách lên lịch chuyến đi và đặt phòng trước ít nhất 14 ngày', 'TOTAL_PERCENTAGE', 12.0, 350000.0, 1000000.0, '2026-09-01 00:00:00', '2026-12-31 23:59:59', 250, 14, 'ACTIVE', false, false, NULL, 'https://images.unsplash.com/photo-1436491865332-7a61a109cc05?q=80&w=800', NOW(), NOW()),
(7, 'YEAREND2026', 'Lễ hội Cuối năm Rộn ràng', 'Đón Giáng Sinh và Chào Năm Mới 2027 cùng ưu đãi 25% tối đa 1.000.000 VNĐ cho phòng Suite', 'ROOM_PERCENTAGE', 25.0, 1000000.0, 2000000.0, '2026-09-01 00:00:00', '2026-12-31 23:59:59', 100, 5, 'ACTIVE', false, false, NULL, 'https://images.unsplash.com/photo-1512389142860-9c449e58a543?q=80&w=800', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    type = EXCLUDED.type,
    discount_value = EXCLUDED.discount_value,
    max_discount_amount = EXCLUDED.max_discount_amount,
    min_booking_value = EXCLUDED.min_booking_value,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    usage_limit = EXCLUDED.usage_limit,
    status = EXCLUDED.status,
    image_url = EXCLUDED.image_url;

-- ============================================================================
-- 8. TAI KHOAN, QUYEN, NHAN VIEN & KHACH HANG
-- ============================================================================
UPDATE account SET password = '$2a$10$UBgcc01a6swgBSz2GaNWquWsGdA.LVltVEX67N/tq.AEaO.VphzDK' WHERE email = 'admin@senviet.vn';
UPDATE account SET password = '$2a$10$UBgcc01a6swgBSz2GaNWquWsGdA.LVltVEX67N/tq.AEaO.VphzDK' WHERE email = 'admin.hanoi@senviet.vn';

INSERT INTO account (account_id, email, password) VALUES
(102, 'admin.saigon@senviet.vn', '$2a$10$UBgcc01a6swgBSz2GaNWquWsGdA.LVltVEX67N/tq.AEaO.VphzDK'),
(104, 'letan.saigon@senviet.vn', '$2a$10$F4tERFmHNEB/wPlByP9qFuOX6c.d.NUY2T6ePRdgjirue.j5Ert1O'),
(105, 'letan2.saigon@senviet.vn', '$2a$10$F4tERFmHNEB/wPlByP9qFuOX6c.d.NUY2T6ePRdgjirue.j5Ert1O'),
(106, 'buongphong.saigon@senviet.vn', '$2a$10$v0je6025alNynRTcq1.ahOARxHZp4xAqpMbS3B2p.WQfP4TWugEUi'),
(107, 'letan.hanoi@senviet.vn', '$2a$10$F4tERFmHNEB/wPlByP9qFuOX6c.d.NUY2T6ePRdgjirue.j5Ert1O'),
(108, 'buongphong.hanoi@senviet.vn', '$2a$10$v0je6025alNynRTcq1.ahOARxHZp4xAqpMbS3B2p.WQfP4TWugEUi'),
(109, 'letan.danang@senviet.vn', '$2a$10$F4tERFmHNEB/wPlByP9qFuOX6c.d.NUY2T6ePRdgjirue.j5Ert1O'),
(110, 'buongphong.danang@senviet.vn', '$2a$10$v0je6025alNynRTcq1.ahOARxHZp4xAqpMbS3B2p.WQfP4TWugEUi'),
(201, 'customer@senviet.vn', '$2a$10$667bxb/28V3osb4so0APy.5n/xBS70FmPFS6uTpKcDnycVzz11GF.'),
(202, 'nguyenvana@gmail.com', '$2a$10$667bxb/28V3osb4so0APy.5n/xBS70FmPFS6uTpKcDnycVzz11GF.'),
(203, 'tranthib@gmail.com', '$2a$10$667bxb/28V3osb4so0APy.5n/xBS70FmPFS6uTpKcDnycVzz11GF.'),
(204, 'lehoangc@gmail.com', '$2a$10$667bxb/28V3osb4so0APy.5n/xBS70FmPFS6uTpKcDnycVzz11GF.'),
(205, 'phamminhduc@gmail.com', '$2a$10$667bxb/28V3osb4so0APy.5n/xBS70FmPFS6uTpKcDnycVzz11GF.')
ON CONFLICT (account_id) DO UPDATE SET password = EXCLUDED.password, email = EXCLUDED.email;

INSERT INTO account_role (account_id, role_id)
SELECT a.account_id, r.role_id
FROM (VALUES 
    (1, 'ROLE_ADMIN'),
    (2, 'ROLE_ADMIN'),
    (102, 'ROLE_ADMIN'),
    (104, 'ROLE_EMPLOYEE'),
    (105, 'ROLE_EMPLOYEE'),
    (106, 'ROLE_EMPLOYEE'),
    (107, 'ROLE_EMPLOYEE'),
    (108, 'ROLE_EMPLOYEE'),
    (109, 'ROLE_EMPLOYEE'),
    (110, 'ROLE_EMPLOYEE'),
    (201, 'ROLE_CUSTOMER'),
    (202, 'ROLE_CUSTOMER'),
    (203, 'ROLE_CUSTOMER'),
    (204, 'ROLE_CUSTOMER'),
    (205, 'ROLE_CUSTOMER')
) AS v(account_id, role_name)
JOIN roles r ON r.name = v.role_name
JOIN account a ON a.account_id = v.account_id
ON CONFLICT DO NOTHING;

INSERT INTO employees (employee_id, full_name, phone, position, cccd, address, date_of_birth, hotel_id, account_id) VALUES
(101, 'Võ Minh Trí', '0901234567', 'Tổng Quản Lý Hệ Thống', '079201001111', '123 Nguyễn Huệ, Q.1, TP.HCM', '1988-05-12', NULL, 1),
(102, 'Trần Minh Tuấn', '0901111111', 'Giám Đốc Chi Nhánh Sài Gòn', '079201002222', '45 Lê Duẩn, Q.1, TP.HCM', '1990-08-20', 1, 102),
(103, 'Nguyễn Hoàng Nam', '0902222222', 'Giám Đốc Chi Nhánh Hà Nội', '001201003333', '88 Phố Huế, Hoàn Kiếm, Hà Nội', '1991-03-15', 2, 2),
(104, 'Lê Thị Thu Thảo', '0903333331', 'Lễ Tân Trưởng (Ca Sáng 06h-14h)', '079301004444', '12 Nguyễn Đình Chiểu, Q.3, TP.HCM', '1998-11-04', 1, 104),
(105, 'Nguyễn Quốc Bảo', '0903333332', 'Nhân Viên Lễ Tân (Ca Tối 14h-22h)', '079301005555', '56 Cách Mạng Tháng 8, Q.10, TP.HCM', '2000-02-18', 1, 105),
(106, 'Đặng Thị Cẩm Tú', '0904444441', 'Nhân Viên Buồng Phòng', '079301006666', '78 Bình Thới, Q.11, TP.HCM', '1995-09-25', 1, 106),
(107, 'Phạm Quỳnh Nga', '0905555551', 'Nhân Viên Lễ Tân', '001301007777', '24 Hàng Bông, Hoàn Kiếm, Hà Nội', '1999-07-10', 2, 107),
(108, 'Bùi Văn Hùng', '0906666661', 'Nhân Viên Buồng Phòng', '001301008888', '15 Đại Cồ Việt, Hai Bà Trưng, Hà Nội', '1996-12-30', 2, 108),
(109, 'Trần Đình Trọng', '0907777771', 'Nhân Viên Lễ Tân', '048301009999', '28 Bạch Đằng, Hải Châu, Đà Nẵng', '1997-04-12', 3, 109),
(110, 'Hoàng Thị Mỹ Linh', '0908888881', 'Nhân Viên Buồng Phòng', '048301001010', '42 Nguyễn Văn Linh, Đà Nẵng', '1996-08-22', 3, 110)
ON CONFLICT (employee_id) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    position = EXCLUDED.position,
    cccd = EXCLUDED.cccd;

INSERT INTO customers (customer_id, full_name, email, phone, cccd, loyalty_tier, total_spent, total_bookings, account_id) VALUES
(201, 'Huỳnh Văn Hiếu', 'customer@senviet.vn', '0901234567', '079203001234', 'BRONZE', 1000000.0, 1, 201),
(202, 'Nguyễn Văn An', 'nguyenvana@gmail.com', '0912345678', '079203005678', 'SILVER', 4500000.0, 3, 202),
(203, 'Trần Thị Bích', 'tranthib@gmail.com', '0923456789', '079203009876', 'GOLD', 12800000.0, 6, 203),
(204, 'Lê Hoàng Cường', 'lehoangc@gmail.com', '0934567890', '079203004321', 'PLATINUM', 35600000.0, 14, 204),
(205, 'Phạm Minh Đức', 'phamminhduc@gmail.com', '0945678901', '079203008765', 'BRONZE', 0.0, 0, 205)
ON CONFLICT (customer_id) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    loyalty_tier = EXCLUDED.loyalty_tier,
    phone = EXCLUDED.phone,
    cccd = EXCLUDED.cccd;

-- ============================================================================
-- 9. DANH SACH PHONG (ROOMS) - 56 PHONG CHO 7 CHI NHANH (MOI PHONG CO 4-8 ANH)
-- ============================================================================
DO $$
DECLARE
    img_standard json := '[
        {"url": "https://images.unsplash.com/photo-1618773928121-c32242e63f39?q=80&w=1200&auto=format&fit=crop", "isDefault": true},
        {"url": "https://images.unsplash.com/photo-1591088398332-8a7791972843?q=80&w=1200&auto=format&fit=crop", "isDefault": false},
        {"url": "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?q=80&w=1200&auto=format&fit=crop", "isDefault": false},
        {"url": "https://images.unsplash.com/photo-1566665797739-1674de7a421a?q=80&w=1200&auto=format&fit=crop", "isDefault": false},
        {"url": "https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1200&auto=format&fit=crop", "isDefault": false}
    ]';
    img_deluxe json := '[
        {"url": "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?q=80&w=1200&auto=format&fit=crop", "isDefault": true},
        {"url": "https://images.unsplash.com/photo-1595576508898-0ad5c879a061?q=80&w=1200&auto=format&fit=crop", "isDefault": false},
        {"url": "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?q=80&w=1200&auto=format&fit=crop", "isDefault": false},
        {"url": "https://images.unsplash.com/photo-1616046229478-9901c5536a45?q=80&w=1200&auto=format&fit=crop", "isDefault": false},
        {"url": "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?q=80&w=1200&auto=format&fit=crop", "isDefault": false},
        {"url": "https://images.unsplash.com/photo-1507652313519-d4e9174996dd?q=80&w=1200&auto=format&fit=crop", "isDefault": false}
    ]';
    img_suite json := '[
        {"url": "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?q=80&w=1200&auto=format&fit=crop", "isDefault": true},
        {"url": "https://images.unsplash.com/photo-1590381105924-c72589b9ef3f?q=80&w=1200&auto=format&fit=crop", "isDefault": false},
        {"url": "https://images.unsplash.com/photo-1618773928121-c32242e63f39?q=80&w=1200&auto=format&fit=crop", "isDefault": false},
        {"url": "https://images.unsplash.com/photo-1582719508461-905c673771fd?q=80&w=1200&auto=format&fit=crop", "isDefault": false},
        {"url": "https://images.unsplash.com/photo-1540518614846-7eded433c457?q=80&w=1200&auto=format&fit=crop", "isDefault": false},
        {"url": "https://images.unsplash.com/photo-1584132967334-10e028bd69f7?q=80&w=1200&auto=format&fit=crop", "isDefault": false},
        {"url": "https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?q=80&w=1200&auto=format&fit=crop", "isDefault": false}
    ]';
    img_family json := '[
        {"url": "https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1200&auto=format&fit=crop", "isDefault": true},
        {"url": "https://images.unsplash.com/photo-1598928506311-c55ded91a20c?q=80&w=1200&auto=format&fit=crop", "isDefault": false},
        {"url": "https://images.unsplash.com/photo-1596394516093-501ba68a0ba6?q=80&w=1200&auto=format&fit=crop", "isDefault": false},
        {"url": "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=1200&auto=format&fit=crop", "isDefault": false},
        {"url": "https://images.unsplash.com/photo-1566073771259-6a8506099945?q=80&w=1200&auto=format&fit=crop", "isDefault": false},
        {"url": "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?q=80&w=1200&auto=format&fit=crop", "isDefault": false}
    ]';
BEGIN
    -- 1. SÀI GÒN (Floor 1, 2)
    INSERT INTO rooms (room_id, floor_id, room_type, room_status, price, base_price, avatar_url) VALUES
    (5, 1, 0, 0, 1000000.0, 1000000.0, img_standard),
    (6, 1, 1, 2, 2000000.0, 2000000.0, img_deluxe),
    (7, 1, 2, 3, 3000000.0, 3000000.0, img_suite),
    (8, 1, 3, 1, 3500000.0, 3500000.0, img_family),
    (9,  2, 0, 0, 1000000.0, 1000000.0, img_standard),
    (10, 2, 1, 0, 2000000.0, 2000000.0, img_deluxe),
    (11, 2, 2, 2, 3000000.0, 3000000.0, img_suite),
    (12, 2, 3, 3, 3500000.0, 3500000.0, img_family)
    ON CONFLICT (room_id) DO UPDATE SET floor_id = EXCLUDED.floor_id, room_type = EXCLUDED.room_type, room_status = EXCLUDED.room_status, price = EXCLUDED.price, base_price = EXCLUDED.base_price, avatar_url = EXCLUDED.avatar_url;

    -- 2. HÀ NỘI (Floor 3, 4)
    INSERT INTO rooms (room_id, floor_id, room_type, room_status, price, base_price, avatar_url) VALUES
    (13, 3, 0, 0, 1000000.0, 1000000.0, img_standard),
    (14, 3, 1, 0, 2000000.0, 2000000.0, img_deluxe),
    (15, 3, 2, 0, 3000000.0, 3000000.0, img_suite),
    (16, 3, 3, 0, 3500000.0, 3500000.0, img_family),
    (17, 4, 0, 0, 1000000.0, 1000000.0, img_standard),
    (18, 4, 1, 0, 2000000.0, 2000000.0, img_deluxe),
    (19, 4, 2, 0, 3000000.0, 3000000.0, img_suite),
    (20, 4, 3, 0, 3500000.0, 3500000.0, img_family)
    ON CONFLICT (room_id) DO UPDATE SET floor_id = EXCLUDED.floor_id, room_type = EXCLUDED.room_type, room_status = EXCLUDED.room_status, price = EXCLUDED.price, base_price = EXCLUDED.base_price, avatar_url = EXCLUDED.avatar_url;

    -- 3. ĐÀ NẴNG (Floor 5, 6)
    INSERT INTO rooms (room_id, floor_id, room_type, room_status, price, base_price, avatar_url) VALUES
    (21, 5, 0, 0, 1200000.0, 1200000.0, img_standard),
    (22, 5, 1, 0, 2200000.0, 2200000.0, img_deluxe),
    (23, 5, 2, 2, 3400000.0, 3400000.0, img_suite),
    (24, 5, 3, 0, 3800000.0, 3800000.0, img_family),
    (25, 6, 0, 0, 1200000.0, 1200000.0, img_standard),
    (26, 6, 1, 3, 2200000.0, 2200000.0, img_deluxe),
    (27, 6, 2, 0, 3400000.0, 3400000.0, img_suite),
    (28, 6, 3, 1, 3800000.0, 3800000.0, img_family)
    ON CONFLICT (room_id) DO UPDATE SET floor_id = EXCLUDED.floor_id, room_type = EXCLUDED.room_type, room_status = EXCLUDED.room_status, price = EXCLUDED.price, base_price = EXCLUDED.base_price, avatar_url = EXCLUDED.avatar_url;

    -- 4. NHA TRANG (Floor 7, 8)
    INSERT INTO rooms (room_id, floor_id, room_type, room_status, price, base_price, avatar_url) VALUES
    (29, 7, 0, 0, 1100000.0, 1100000.0, img_standard),
    (30, 7, 1, 2, 2100000.0, 2100000.0, img_deluxe),
    (31, 7, 2, 0, 3200000.0, 3200000.0, img_suite),
    (32, 7, 3, 0, 3600000.0, 3600000.0, img_family),
    (33, 8, 0, 0, 1100000.0, 1100000.0, img_standard),
    (34, 8, 1, 0, 2100000.0, 2100000.0, img_deluxe),
    (35, 8, 2, 3, 3200000.0, 3200000.0, img_suite),
    (36, 8, 3, 0, 3600000.0, 3600000.0, img_family)
    ON CONFLICT (room_id) DO UPDATE SET floor_id = EXCLUDED.floor_id, room_type = EXCLUDED.room_type, room_status = EXCLUDED.room_status, price = EXCLUDED.price, base_price = EXCLUDED.base_price, avatar_url = EXCLUDED.avatar_url;

    -- 5. ĐÀ LẠT (Floor 9, 10)
    INSERT INTO rooms (room_id, floor_id, room_type, room_status, price, base_price, avatar_url) VALUES
    (37, 9, 0, 0, 950000.0, 950000.0, img_standard),
    (38, 9, 1, 0, 1850000.0, 1850000.0, img_deluxe),
    (39, 9, 2, 0, 2900000.0, 2900000.0, img_suite),
    (40, 9, 3, 2, 3400000.0, 3400000.0, img_family),
    (41, 10, 0, 0, 950000.0, 950000.0, img_standard),
    (42, 10, 1, 3, 1850000.0, 1850000.0, img_deluxe),
    (43, 10, 2, 0, 2900000.0, 2900000.0, img_suite),
    (44, 10, 3, 0, 3400000.0, 3400000.0, img_family)
    ON CONFLICT (room_id) DO UPDATE SET floor_id = EXCLUDED.floor_id, room_type = EXCLUDED.room_type, room_status = EXCLUDED.room_status, price = EXCLUDED.price, base_price = EXCLUDED.base_price, avatar_url = EXCLUDED.avatar_url;

    -- 6. GÒ CÔNG (Floor 11, 12)
    INSERT INTO rooms (room_id, floor_id, room_type, room_status, price, base_price, avatar_url) VALUES
    (45, 11, 0, 0, 800000.0, 800000.0, img_standard),
    (46, 11, 1, 0, 1400000.0, 1400000.0, img_deluxe),
    (47, 11, 2, 0, 2200000.0, 2200000.0, img_suite),
    (48, 11, 3, 0, 2600000.0, 2600000.0, img_family),
    (49, 12, 0, 0, 800000.0, 800000.0, img_standard),
    (50, 12, 1, 0, 1400000.0, 1400000.0, img_deluxe),
    (51, 12, 2, 0, 2200000.0, 2200000.0, img_suite),
    (52, 12, 3, 0, 2600000.0, 2600000.0, img_family)
    ON CONFLICT (room_id) DO UPDATE SET floor_id = EXCLUDED.floor_id, room_type = EXCLUDED.room_type, room_status = EXCLUDED.room_status, price = EXCLUDED.price, base_price = EXCLUDED.base_price, avatar_url = EXCLUDED.avatar_url;

    -- 7. AN NHƠN (Floor 13, 14)
    INSERT INTO rooms (room_id, floor_id, room_type, room_status, price, base_price, avatar_url) VALUES
    (53, 13, 0, 0, 850000.0, 850000.0, img_standard),
    (54, 13, 1, 0, 1500000.0, 1500000.0, img_deluxe),
    (55, 13, 2, 0, 2400000.0, 2400000.0, img_suite),
    (56, 13, 3, 0, 2800000.0, 2800000.0, img_family),
    (57, 14, 0, 0, 850000.0, 850000.0, img_standard),
    (58, 14, 1, 0, 1500000.0, 1500000.0, img_deluxe),
    (59, 14, 2, 0, 2400000.0, 2400000.0, img_suite),
    (60, 14, 3, 0, 2800000.0, 2800000.0, img_family)
    ON CONFLICT (room_id) DO UPDATE SET floor_id = EXCLUDED.floor_id, room_type = EXCLUDED.room_type, room_status = EXCLUDED.room_status, price = EXCLUDED.price, base_price = EXCLUDED.base_price, avatar_url = EXCLUDED.avatar_url;

END $$;

-- ============================================================================
-- 10. GẮN TIỆN ÍCH CHO PHÒNG (ROOM_AMENITIES) - GIỮ NGUYÊN BỘ 8 TIỆN ÍCH GỐC
-- ============================================================================
-- Standard: Wifi (1), TV (2), Két sắt (5), Minibar (7), Máy sấy (8)
-- Deluxe:   Wifi (1), TV (2), Bồn tắm (3), Ban công (4), Két sắt (5), Minibar (7), Máy sấy (8)
-- Suite:    Đầy đủ 8 tiện ích gốc: (1, 2, 3, 4, 5, 6, 7, 8)
-- Family:   Wifi (1), TV (2), Ban công (4), Két sắt (5), Minibar (7), Máy sấy (8)
INSERT INTO room_amenities (room_id, amenity_id)
SELECT r.room_id, a.amenity_id
FROM rooms r
CROSS JOIN amenities a
WHERE (
    (r.room_type = 0 AND a.amenity_id IN (1, 2, 5, 7, 8))
    OR (r.room_type = 1 AND a.amenity_id IN (1, 2, 3, 4, 5, 7, 8))
    OR (r.room_type = 2 AND a.amenity_id IN (1, 2, 3, 4, 5, 6, 7, 8))
    OR (r.room_type = 3 AND a.amenity_id IN (1, 2, 4, 5, 7, 8))
)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 11. KỊCH BẢN ĐẶT PHÒNG TEST FLOW NHÂN VIÊN (BOOKINGS, ORDERS, TRANSACTIONS)
-- ============================================================================
-- KỊCH BẢN 1: ĐƠN CHỜ CHECK-IN HÔM NAY (CONFIRMED)
INSERT INTO orders (order_id, issue_date, close_date, room_total_amount, service_total_amount, discount_room_amount, discount_service_amount, discount_amount_total, paid_amount, order_status) VALUES
(301, NOW() - INTERVAL '1 day', NULL, 2000000.0, 0.0, 0.0, 0.0, 0.0, 1000000.0, 0)
ON CONFLICT (order_id) DO UPDATE SET order_status = EXCLUDED.order_status, paid_amount = EXCLUDED.paid_amount;

INSERT INTO payment_transactions (payment_transaction_id, order_order_id, amount, payment_type, cash_flow_type, note, created_at) VALUES
(401, 301, 1000000.0, 'BANK', 'RECEIPT', 'Khách cọc trước 50% tiền phòng qua chuyển khoản VNPAY', NOW() - INTERVAL '1 day')
ON CONFLICT (payment_transaction_id) DO UPDATE SET amount = EXCLUDED.amount;

INSERT INTO bookings (booking_id, customer_id, employee_id, booking_status, booking_channel, created_at, apply_amount, apply_at, promotion_id, order_id) VALUES
(501, 202, 104, 1, 0, NOW() - INTERVAL '1 day', NULL, NULL, NULL, 301)
ON CONFLICT (booking_id) DO UPDATE SET booking_status = EXCLUDED.booking_status;

INSERT INTO booking_details (booking_detail_id, booking_id, room_id, checkin_time, checkout_time, num_adults, num_children, num_infants, base_room_price_per_night, extra_adult_fee_per_night, extra_child_fee_per_night, room_sub_total, service_sub_total, total_price) VALUES
(601, 501, 10, CURRENT_DATE + TIME '14:00:00', CURRENT_DATE + INTERVAL '1 day' + TIME '12:00:00', 2, 0, 0, 2000000.0, 0.0, 0.0, 2000000.0, 0.0, 2000000.0)
ON CONFLICT (booking_detail_id) DO UPDATE SET checkin_time = EXCLUDED.checkin_time, room_id = 10;

-- KỊCH BẢN 2: ĐƠN ĐANG LƯU TRÚ CÓ PHÁT SINH DỊCH VỤ (IN_HOUSE / IN_USE)
INSERT INTO orders (order_id, issue_date, close_date, room_total_amount, service_total_amount, discount_room_amount, discount_service_amount, discount_amount_total, paid_amount, order_status) VALUES
(302, NOW() - INTERVAL '2 days', NULL, 4000000.0, 1050000.0, 0.0, 0.0, 0.0, 2000000.0, 0)
ON CONFLICT (order_id) DO UPDATE SET 
    room_total_amount = EXCLUDED.room_total_amount,
    service_total_amount = EXCLUDED.service_total_amount,
    paid_amount = EXCLUDED.paid_amount,
    order_status = 0;

INSERT INTO payment_transactions (payment_transaction_id, order_order_id, amount, payment_type, cash_flow_type, note, created_at) VALUES
(402, 302, 2000000.0, 'BANK', 'RECEIPT', 'Đặt cọc ngày đầu nhận phòng', NOW() - INTERVAL '2 days')
ON CONFLICT (payment_transaction_id) DO UPDATE SET amount = EXCLUDED.amount;

INSERT INTO bookings (booking_id, customer_id, employee_id, booking_status, booking_channel, created_at, apply_amount, apply_at, promotion_id, order_id) VALUES
(502, 203, 104, 3, 0, NOW() - INTERVAL '2 days', NULL, NULL, NULL, 302)
ON CONFLICT (booking_id) DO UPDATE SET booking_status = EXCLUDED.booking_status;

INSERT INTO booking_details (booking_detail_id, booking_id, room_id, checkin_time, checkout_time, num_adults, num_children, num_infants, base_room_price_per_night, extra_adult_fee_per_night, extra_child_fee_per_night, room_sub_total, service_sub_total, total_price) VALUES
(602, 502, 6, NOW() - INTERVAL '2 days', CURRENT_DATE + TIME '12:00:00', 2, 1, 0, 2000000.0, 0.0, 0.0, 4000000.0, 1050000.0, 5050000.0)
ON CONFLICT (booking_detail_id) DO UPDATE SET service_sub_total = EXCLUDED.service_sub_total, total_price = EXCLUDED.total_price, room_id = 6;

INSERT INTO booking_services (booking_service_id, booking_detail_id, service_id, quantity, price, used_at) VALUES
(701, 602, 5, 1, 350000.0, NOW() - INTERVAL '1 day'),
(702, 602, 3, 2, 350000.0, NOW() - INTERVAL '12 hours')
ON CONFLICT (booking_service_id) DO UPDATE SET quantity = EXCLUDED.quantity, price = EXCLUDED.price;

-- KỊCH BẢN 3: ĐƠN ĐÃ HOÀN TẤT LƯU TRÚ & XUẤT HÓA ĐƠN (COMPLETED)
INSERT INTO orders (order_id, issue_date, close_date, room_total_amount, service_total_amount, discount_room_amount, discount_service_amount, discount_amount_total, paid_amount, order_status) VALUES
(303, NOW() - INTERVAL '5 days', NOW() - INTERVAL '3 days', 6000000.0, 300000.0, 500000.0, 0.0, 500000.0, 5800000.0, 1)
ON CONFLICT (order_id) DO UPDATE SET order_status = 1, paid_amount = EXCLUDED.paid_amount;

INSERT INTO payment_transactions (payment_transaction_id, order_order_id, amount, payment_type, cash_flow_type, note, created_at) VALUES
(403, 303, 3000000.0, 'BANK', 'RECEIPT', 'Thanh toán cọc trực tuyến', NOW() - INTERVAL '5 days'),
(404, 303, 2800000.0, 'CASH', 'RECEIPT', 'Thanh toán phần còn lại khi Check-out tại quầy lễ tân', NOW() - INTERVAL '3 days')
ON CONFLICT (payment_transaction_id) DO UPDATE SET amount = EXCLUDED.amount;

INSERT INTO bookings (booking_id, customer_id, employee_id, booking_status, booking_channel, created_at, apply_amount, apply_at, promotion_id, order_id) VALUES
(503, 204, 105, 4, 0, NOW() - INTERVAL '5 days', 500000.0, NOW() - INTERVAL '5 days', 2, 303)
ON CONFLICT (booking_id) DO UPDATE SET booking_status = 4;

INSERT INTO booking_details (booking_detail_id, booking_id, room_id, checkin_time, checkout_time, num_adults, num_children, num_infants, base_room_price_per_night, extra_adult_fee_per_night, extra_child_fee_per_night, room_sub_total, service_sub_total, total_price) VALUES
(603, 503, 7, NOW() - INTERVAL '5 days', NOW() - INTERVAL '3 days', 2, 0, 0, 3000000.0, 0.0, 0.0, 6000000.0, 300000.0, 5800000.0)
ON CONFLICT (booking_detail_id) DO UPDATE SET total_price = EXCLUDED.total_price, room_id = 7;

INSERT INTO booking_services (booking_service_id, booking_detail_id, service_id, quantity, price, used_at) VALUES
(703, 603, 1, 2, 150000.0, NOW() - INTERVAL '4 days')
ON CONFLICT (booking_service_id) DO UPDATE SET price = EXCLUDED.price;

-- KỊCH BẢN 4: ĐƠN ĐÃ HỦY (CANCELLED)
INSERT INTO orders (order_id, issue_date, close_date, room_total_amount, service_total_amount, discount_room_amount, discount_service_amount, discount_amount_total, paid_amount, order_status) VALUES
(304, NOW() - INTERVAL '7 days', NOW() - INTERVAL '6 days', 1000000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2)
ON CONFLICT (order_id) DO UPDATE SET order_status = 2;

INSERT INTO bookings (booking_id, customer_id, employee_id, booking_status, booking_channel, created_at, apply_amount, apply_at, promotion_id, order_id) VALUES
(504, 205, 104, 2, 0, NOW() - INTERVAL '7 days', NULL, NULL, NULL, 304)
ON CONFLICT (booking_id) DO UPDATE SET booking_status = 2;

INSERT INTO booking_details (booking_detail_id, booking_id, room_id, checkin_time, checkout_time, num_adults, num_children, num_infants, base_room_price_per_night, extra_adult_fee_per_night, extra_child_fee_per_night, room_sub_total, service_sub_total, total_price) VALUES
(604, 504, 5, NOW() - INTERVAL '6 days', NOW() - INTERVAL '5 days', 1, 0, 0, 1000000.0, 0.0, 0.0, 1000000.0, 0.0, 1000000.0)
ON CONFLICT (booking_detail_id) DO UPDATE SET room_id = 5;

-- ============================================================================
-- 12. ĐỒNG BỘ CÁC SEQUENCES
-- ============================================================================
SELECT setval(pg_get_serial_sequence('provinces', 'id'), COALESCE((SELECT MAX(id) FROM provinces), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('hotels', 'id'), COALESCE((SELECT MAX(id) FROM hotels), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('buildings', 'id'), COALESCE((SELECT MAX(id) FROM buildings), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('floors', 'id'), COALESCE((SELECT MAX(id) FROM floors), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('branch_room_policies', 'id'), COALESCE((SELECT MAX(id) FROM branch_room_policies), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('amenities', 'amenity_id'), COALESCE((SELECT MAX(amenity_id) FROM amenities), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('services', 'service_id'), COALESCE((SELECT MAX(service_id) FROM services), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('promotions', 'id'), COALESCE((SELECT MAX(id) FROM promotions), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('account', 'account_id'), COALESCE((SELECT MAX(account_id) FROM account), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('employees', 'employee_id'), COALESCE((SELECT MAX(employee_id) FROM employees), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('customers', 'customer_id'), COALESCE((SELECT MAX(customer_id) FROM customers), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('rooms', 'room_id'), COALESCE((SELECT MAX(room_id) FROM rooms), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('orders', 'order_id'), COALESCE((SELECT MAX(order_id) FROM orders), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('payment_transactions', 'payment_transaction_id'), COALESCE((SELECT MAX(payment_transaction_id) FROM payment_transactions), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('bookings', 'booking_id'), COALESCE((SELECT MAX(booking_id) FROM bookings), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('booking_details', 'booking_detail_id'), COALESCE((SELECT MAX(booking_detail_id) FROM booking_details), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('booking_services', 'booking_service_id'), COALESCE((SELECT MAX(booking_service_id) FROM booking_services), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('reviews', 'review_id'), COALESCE((SELECT MAX(review_id) FROM reviews), 0) + 1, false);

COMMIT;
