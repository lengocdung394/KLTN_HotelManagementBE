-- Tạo các booking đã hoàn thành để gắn với đánh giá
INSERT INTO bookings (booking_id, customer_id, booking_status, booking_channel, created_at) VALUES
(601, 202, 4, 0, CURRENT_TIMESTAMP - INTERVAL '15 days'),
(602, 203, 4, 0, CURRENT_TIMESTAMP - INTERVAL '12 days'),
(603, 204, 4, 0, CURRENT_TIMESTAMP - INTERVAL '8 days'),
(604, 205, 4, 0, CURRENT_TIMESTAMP - INTERVAL '20 days'),
(605, 201, 4, 0, CURRENT_TIMESTAMP - INTERVAL '14 days'),
(606, 202, 4, 0, CURRENT_TIMESTAMP - INTERVAL '6 days'),
(607, 203, 4, 0, CURRENT_TIMESTAMP - INTERVAL '18 days'),
(608, 204, 4, 0, CURRENT_TIMESTAMP - INTERVAL '5 days'),
(609, 205, 4, 0, CURRENT_TIMESTAMP - INTERVAL '22 days'),
(610, 201, 4, 0, CURRENT_TIMESTAMP - INTERVAL '10 days'),
(611, 202, 4, 0, CURRENT_TIMESTAMP - INTERVAL '7 days'),
(612, 203, 4, 0, CURRENT_TIMESTAMP - INTERVAL '4 days')
ON CONFLICT (booking_id) DO NOTHING;

-- Nạp các bài đánh giá mẫu chất lượng cao
INSERT INTO reviews (booking_id, customer_id, hotel_id, room_type_name, rating, cleanliness_rating, service_rating, facilities_rating, location_rating, title, comment, created_at) VALUES
-- Sen Việt Sài Gòn (hotel_id = 1)
(601, 202, 1, 'Phòng Suite Ban Công Triệu Đô', 5, 5, 5, 5, 5, 'Trải nghiệm thượng lưu ngắm trọn sông Sài Gòn', 'Khách sạn có tầm nhìn panorama sông Sài Gòn tuyệt đẹp, hồ bơi vô cực dát vàng trên tầng thượng rất đẳng cấp. Nhân viên phục vụ chu đáo, trà sen chào đón thơm ngát.', CURRENT_TIMESTAMP - INTERVAL '14 days'),
(602, 203, 1, 'Phòng Deluxe Hướng Phố Hoa Lệ', 5, 5, 5, 4, 5, 'Vị trí đắc địa ngay trung tâm Quận 1', 'Nằm ngay trung tâm nên rất tiện đi dạo phố đi bộ và mua sắm. Phòng ốc cực kỳ sạch sẽ, cách âm hoàn hảo, giường nằm êm ái ngủ rất ngon giấc.', CURRENT_TIMESTAMP - INTERVAL '11 days'),
(603, 204, 1, 'Phòng Standard Thanh Lịch', 4, 5, 4, 4, 5, 'Dịch vụ chuẩn 5 sao, đồ ăn sáng phong phú', 'Khách sạn rất đẹp, buffet sáng đa dạng các món Á - Âu ngon miệng. Chỉ có thang máy vào giờ cao điểm hơi đông một chút nhưng nhân viên điều phối rất nhanh nhẹn.', CURRENT_TIMESTAMP - INTERVAL '7 days'),

-- Sen Việt Hà Nội (hotel_id = 2)
(604, 205, 2, 'Phòng Suite Cung Đình Indochine', 5, 5, 5, 5, 5, 'Kiến trúc Đông Dương quý tộc ngay cạnh Hồ Gươm', 'Không gian mang đậm nét cổ kính hoàng gia Thăng Long rất ấn tượng. Bước chân ra là phố cổ, sảnh đón thơm ngát hương sen Tây Hồ, trà thất cung đình phục vụ rất tinh tế.', CURRENT_TIMESTAMP - INTERVAL '19 days'),
(605, 201, 2, 'Phòng Deluxe Ban Công Phố Cổ', 5, 5, 5, 5, 5, 'Kỳ nghỉ tuyệt vời cùng gia đình tại thủ đô', 'Nhân viên lễ tân rất ân cần, hỗ trợ gia đình tôi đặt tour tham quan Hà Nội rất chu đáo. Phòng ngủ ấm cúng, nội thất gỗ lim sang trọng và thơm mùi gỗ tự nhiên.', CURRENT_TIMESTAMP - INTERVAL '13 days'),
(606, 202, 2, 'Phòng Standard Hoàn Kiếm', 5, 5, 5, 4, 5, 'Chất lượng xuất sắc vượt kỳ vọng', 'Phòng ốc gọn gàng ngăn nắp, trang thiết bị hiện đại. Vị trí đi bộ vài bước là đến tháp Rùa và kem Tràng Tiền, lần sau ra Hà Nội chắc chắn tôi sẽ quay lại.', CURRENT_TIMESTAMP - INTERVAL '5 days'),

-- Sen Việt Đà Nẵng (hotel_id = 3)
(607, 203, 3, 'Phòng Suite Biệt Thự Hướng Biển', 5, 5, 5, 5, 5, 'Bãi biển Mỹ Khê riêng biệt và hồ bơi tràn bờ mê hoặc', 'Resort nằm ngay sát bãi cát trắng mịn, ngắm trọn bình minh bán đảo Sơn Trà từ ban công phòng. Hồ bơi nước mặn rất sạch và thư giãn.', CURRENT_TIMESTAMP - INTERVAL '17 days'),
(608, 204, 3, 'Phòng Deluxe View Biển Mỹ Khê', 5, 5, 5, 5, 5, 'Kỳ nghỉ trăng mật không thể nào quên', 'Cảm ơn Sen Việt Đà Nẵng đã chuẩn bị hoa tươi và bánh kem chúc mừng trăng mật rất bất ngờ. Dịch vụ spa đá muối Himalaya ở đây cực kỳ thư thái.', CURRENT_TIMESTAMP - INTERVAL '4 days'),

-- Sen Việt Nha Trang (hotel_id = 4)
(609, 205, 4, 'Phòng Suite Ban Công Vịnh Biển', 5, 5, 5, 5, 5, 'Tầm nhìn triệu đô ôm trọn vịnh Nha Trang', '100% phòng đều có view biển cực xịn. Bồn tắm ngâm thảo dược ngắm hoàng hôn vịnh biển là điểm nhấn đáng giá nhất chuyến đi. Sky Bar buổi tối rất chill.', CURRENT_TIMESTAMP - INTERVAL '21 days'),
(610, 201, 4, 'Phòng Deluxe Hướng Đại Dương', 4, 4, 5, 4, 5, 'Dịch vụ chu đáo, hải sản tươi ngon', 'Vị trí mặt đường Trần Phú qua đường là tới bãi biển. Buffet hải sản tối tươi ngon, nhân viên luôn mỉm cười chào đón khách.', CURRENT_TIMESTAMP - INTERVAL '9 days'),

-- Sen Việt Gò Công (hotel_id = 6)
(611, 202, 6, 'Phòng Suite Ven Sông Tiền Giang', 5, 5, 5, 5, 5, 'Bản hòa tấu thanh bình miền Tây sông nước', 'Không gian yên bình giữa miệt vườn Nam Bộ, buổi sáng ngắm sông Tiền êm đềm và nghe đờn ca tài tử rất mộc mạc và thư thái.', CURRENT_TIMESTAMP - INTERVAL '6 days'),

-- Sen Việt An Nhơn (hotel_id = 7)
(612, 203, 7, 'Phòng Deluxe Hào Khí Tây Sơn', 5, 5, 5, 4, 5, 'Đậm đà bản sắc văn hóa Champa và Bình Định', 'Khách sạn bài trí nghệ thuật gốm nung và đá sa thạch rất độc đáo, hồ sen tĩnh lặng, ẩm thực đặc sản địa phương được chế biến vô cùng tinh tế.', CURRENT_TIMESTAMP - INTERVAL '3 days')
ON CONFLICT (booking_id) DO NOTHING;
