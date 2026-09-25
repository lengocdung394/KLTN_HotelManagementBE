-- ==============================================================================
-- 1. DROP CONSTRAINTS & IDENTITY TO PERMIT CONVERTING TO VARCHAR
-- ==============================================================================

-- SERVICES
ALTER TABLE booking_services DROP CONSTRAINT IF EXISTS fkhhofk6n050slfqp0v6e65axk3;
ALTER TABLE services ALTER COLUMN service_id DROP IDENTITY IF EXISTS;
ALTER TABLE services ALTER COLUMN service_id TYPE character varying(50) USING service_id::character varying(50);
ALTER TABLE booking_services ALTER COLUMN service_id TYPE character varying(50) USING service_id::character varying(50);
ALTER TABLE booking_services ADD CONSTRAINT fkhhofk6n050slfqp0v6e65axk3 FOREIGN KEY (service_id) REFERENCES services(service_id);

-- PROMOTIONS
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS fkk4byobgkjv3y3952wwpxyep7o;
ALTER TABLE saved_promotions DROP CONSTRAINT IF EXISTS fk9nlbj1oyc8vta1h2bdte7agc7;
ALTER TABLE customer_promotions DROP CONSTRAINT IF EXISTS fkmbci9tiiki0g3fih6r0onkr5l;

ALTER TABLE promotions ALTER COLUMN id DROP IDENTITY IF EXISTS;
ALTER TABLE promotions ALTER COLUMN id TYPE character varying(50) USING id::character varying(50);
ALTER TABLE bookings ALTER COLUMN promotion_id TYPE character varying(50) USING promotion_id::character varying(50);
ALTER TABLE saved_promotions ALTER COLUMN promotion_id TYPE character varying(50) USING promotion_id::character varying(50);
ALTER TABLE customer_promotions ALTER COLUMN promotion_id TYPE character varying(50) USING promotion_id::character varying(50);

ALTER TABLE bookings ADD CONSTRAINT fkk4byobgkjv3y3952wwpxyep7o FOREIGN KEY (promotion_id) REFERENCES promotions(id);
ALTER TABLE saved_promotions ADD CONSTRAINT fk9nlbj1oyc8vta1h2bdte7agc7 FOREIGN KEY (promotion_id) REFERENCES promotions(id);
ALTER TABLE customer_promotions ADD CONSTRAINT fkmbci9tiiki0g3fih6r0onkr5l FOREIGN KEY (promotion_id) REFERENCES promotions(id);

-- ROOMS
ALTER TABLE booking_details DROP CONSTRAINT IF EXISTS fk8pcpg1hs4kqwqcvq79i1esg5;
ALTER TABLE room_amenities DROP CONSTRAINT IF EXISTS fkps6ofup9gxhn8juqvproxbaud;
ALTER TABLE rooms_amenities DROP CONSTRAINT IF EXISTS fks6hyhrhhyefpij5kyigapgbom;

ALTER TABLE rooms ALTER COLUMN room_id DROP IDENTITY IF EXISTS;
ALTER TABLE rooms ALTER COLUMN room_id TYPE character varying(50) USING room_id::character varying(50);
ALTER TABLE booking_details ALTER COLUMN room_id TYPE character varying(50) USING room_id::character varying(50);
ALTER TABLE room_amenities ALTER COLUMN room_id TYPE character varying(50) USING room_id::character varying(50);
ALTER TABLE rooms_amenities ALTER COLUMN room_room_id TYPE character varying(50) USING room_room_id::character varying(50);

ALTER TABLE booking_details ADD CONSTRAINT fk8pcpg1hs4kqwqcvq79i1esg5 FOREIGN KEY (room_id) REFERENCES rooms(room_id);
ALTER TABLE room_amenities ADD CONSTRAINT fkps6ofup9gxhn8juqvproxbaud FOREIGN KEY (room_id) REFERENCES rooms(room_id);
ALTER TABLE rooms_amenities ADD CONSTRAINT fks6hyhrhhyefpij5kyigapgbom FOREIGN KEY (room_room_id) REFERENCES rooms(room_id);

-- ROOM NUMBER, STATUS, TYPE, PRICE
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS room_number character varying(20);
ALTER TABLE rooms DROP CONSTRAINT IF EXISTS rooms_room_status_check;
ALTER TABLE rooms DROP CONSTRAINT IF EXISTS rooms_room_type_check;

ALTER TABLE rooms ALTER COLUMN room_status TYPE character varying(50) USING room_status::text;
ALTER TABLE rooms ALTER COLUMN room_type TYPE character varying(50) USING room_type::text;
ALTER TABLE rooms ALTER COLUMN price DROP NOT NULL;
ALTER TABLE rooms ALTER COLUMN base_price DROP NOT NULL;

-- ACCOUNT_ID IN ROOM_PRICE_HISTORIES
ALTER TABLE room_price_histories DROP CONSTRAINT IF EXISTS fkhgjs3u9ourvby2obfudp8whwk;
ALTER TABLE room_price_histories ALTER COLUMN account_id TYPE character varying(50) USING account_id::character varying(50);

-- ==============================================================================
-- 2. INSERT DUNG'S DATA
-- ==============================================================================

-- AMENITIES
INSERT INTO amenities (name, price) VALUES 
('Tivi màn hình phẳng 43 inch', 67000.0),
('Tủ lạnh mini (Mini Fridge)', 60000.0),
('Điều hòa nhiệt độ (Air Conditioner)', 500000.0),
('Máy sấy tóc (Hair Dryer)', 40000.0),
('Két sắt an toàn (Safety Box)', 500000.0),
('Bình đun siêu tốc (Electric Kettle)', 200000.0),
('Bàn làm việc và ghế', 100000.0),
('Gần thang máy (Near Elevator)', 1200000.0),
('Phòng góc (Corner Room)', 5000000.0),
('View hướng biển (Ocean View)', 600000.0),
('View hướng thành phố (City View)', 7000000.0),
('Ban công riêng (Private Balcony)', 900000.0),
('Tầng cao (High Floor)', 200000.0),
('Cửa sổ lớn đón ánh sáng tự nhiên', 5555550.0)
ON CONFLICT DO NOTHING;

-- SERVICES
INSERT INTO services (service_id, name, description, price, unit, category, active, hotel_id) 
VALUES 
('SRV202609221001', 'Giặt ủi cao cấp', 'Dịch vụ giặt và sấy khô quần áo nhanh chóng trong ngày', 50000, 'lần', 'Giặt ủi', true, 1),
('SRV202609221002', 'Buffet sáng', 'Thực đơn ăn sáng tự chọn phong phú món Á - Âu', 150000, 'người', 'Nhà hàng', true, 1),
('SRV202609221003', 'Đưa đón sân bay', 'Xe ô tô 4 chỗ đưa đón tận nơi từ sân bay về khách sạn', 300000, 'lần', 'Đưa đón', true, 1),
('SRV202609221004', 'Massage thư giãn toàn thân', 'Liệu trình massage tinh dầu giúp giảm căng thẳng, mệt mỏi', 250000, 'người', 'Spa', true, 1),
('SRV202609221005', 'Thuê xe máy', 'Thuê xe máy đời mới di chuyển tự túc trong thành phố', 150000, 'lần', 'Thuê xe', true, 1)
ON CONFLICT (service_id) DO UPDATE SET 
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    price = EXCLUDED.price,
    active = EXCLUDED.active;

-- PROMOTIONS
INSERT INTO promotions (
    id, code, name, description, type, discount_value, max_discount_amount, 
    min_booking_value, start_date, end_date, usage_limit, used_count, 
    status, deleted, created_at, updated_at, is_exclusive, hotel_id
) 
VALUES 
(
    'PRO20260922101', 'SUMMERROOM', 'Giảm giá phòng mùa hè', 'Giảm 15% trực tiếp trên tổng tiền phòng đặt trước', 
    'ROOM_PERCENTAGE', 15.00, 200000.00, 500000.00, '2026-06-01 00:00:00', '2026-12-31 23:59:59', 
    100, 5, 'ACTIVE', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 1
),
(
    'PRO20260922102', 'SPASVC', 'Ưu đãi dịch vụ Spa', 'Giảm 20% khi sử dụng các dịch vụ đi kèm trong khách sạn', 
    'SERVICE_PERCENTAGE', 20.00, 150000.00, 200000.00, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 
    500, 42, 'ACTIVE', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true, 1
),
(
    'PRO20260922103', 'TOTALSALE', 'Siêu sale tổng hóa đơn', 'Giảm 10% cho toàn bộ hóa đơn bao gồm cả phòng và dịch vụ', 
    'TOTAL_PERCENTAGE', 10.00, 500000.00, 1000000.00, '2026-01-15 00:00:00', '2026-02-15 23:59:59', 
    50, 50, 'EXPIRED', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 1
)
ON CONFLICT (code) DO UPDATE SET 
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    discount_value = EXCLUDED.discount_value,
    status = EXCLUDED.status;

-- ROOMS
INSERT INTO rooms (
    room_id, room_number, room_status, room_type, floor_id, avatar_url, price, base_price
) 
VALUES 
(
    'ROOM20260922101', '101', 'READY', 'STANDARD', 1, 
    '[
      {"url": "https://res.cloudinary.com/ddkokspkn/image/upload/v1789055313/hotel-management/room/ap30nco1yo7admcgczyp.jpg", "isDefault": true},
      {"url": "https://res.cloudinary.com/ddkokspkn/image/upload/v1789055316/hotel-management/room/vjqueicnl4hux6hhzory.jpg", "isDefault": false}
    ]'::json, 600000.0, 600000.0
),
(
    'ROOM20260922102', '102', 'READY', 'STANDARD', 1, 
    '[
      {"url": "https://res.cloudinary.com/ddkokspkn/image/upload/v1789055310/hotel-management/room/mc1m8gll1in8eueiirlt.png", "isDefault": true},
      {"url": "https://res.cloudinary.com/ddkokspkn/image/upload/v1789055320/hotel-management/room/qkapou4p8hcfj35t2sde.jpg", "isDefault": false}
    ]'::json, 620000.0, 620000.0
),
(
    'ROOM20260922103', '103', 'READY', 'DELUXE', 1, 
    '[
      {"url": "https://res.cloudinary.com/ddkokspkn/image/upload/v1789055310/hotel-management/room/mc1m8gll1in8eueiirlt.png", "isDefault": false},
      {"url": "https://res.cloudinary.com/ddkokspkn/image/upload/v1789055313/hotel-management/room/ap30nco1yo7admcgczyp.jpg", "isDefault": true}
    ]'::json, 900000.0, 900000.0
),
(
    'ROOM20260922104', '104', 'READY', 'DELUXE', 1, 
    '[
      {"url": "https://res.cloudinary.com/ddkokspkn/image/upload/v1789055316/hotel-management/room/vjqueicnl4hux6hhzory.jpg", "isDefault": true},
      {"url": "https://res.cloudinary.com/ddkokspkn/image/upload/v1789055320/hotel-management/room/qkapou4p8hcfj35t2sde.jpg", "isDefault": false}
    ]'::json, 930000.0, 930000.0
),
(
    'ROOM20260922201', '201', 'READY', 'SUITE', 2, 
    '[
      {"url": "https://res.cloudinary.com/ddkokspkn/image/upload/v1789055310/hotel-management/room/mc1m8gll1in8eueiirlt.png", "isDefault": true},
      {"url": "https://res.cloudinary.com/ddkokspkn/image/upload/v1789055313/hotel-management/room/ap30nco1yo7admcgczyp.jpg", "isDefault": false}
    ]'::json, 1500000.0, 1500000.0
),
(
    'ROOM20260922202', '202', 'READY', 'SUITE', 2, 
    '[
      {"url": "https://res.cloudinary.com/ddkokspkn/image/upload/v1789055316/hotel-management/room/vjqueicnl4hux6hhzory.jpg", "isDefault": true},
      {"url": "https://res.cloudinary.com/ddkokspkn/image/upload/v1789055320/hotel-management/room/qkapou4p8hcfj35t2sde.jpg", "isDefault": false}
    ]'::json, 1500000.0, 1500000.0
),
(
    'ROOM20260922203', '203', 'READY', 'FAMILY', 2, 
    '[
      {"url": "https://res.cloudinary.com/ddkokspkn/image/upload/v1789055313/hotel-management/room/ap30nco1yo7admcgczyp.jpg", "isDefault": false},
      {"url": "https://res.cloudinary.com/ddkokspkn/image/upload/v1789055310/hotel-management/room/mc1m8gll1in8eueiirlt.png", "isDefault": true}
    ]'::json, 1800000.0, 1800000.0
),
(
    'ROOM20260922204', '204', 'READY', 'FAMILY', 2, 
    '[
      {"url": "https://res.cloudinary.com/ddkokspkn/image/upload/v1789055320/hotel-management/room/qkapou4p8hcfj35t2sde.jpg", "isDefault": true},
      {"url": "https://res.cloudinary.com/ddkokspkn/image/upload/v1789055316/hotel-management/room/vjqueicnl4hux6hhzory.jpg", "isDefault": false}
    ]'::json, 1800000.0, 1800000.0
)
ON CONFLICT (room_id) DO UPDATE SET 
    room_number = EXCLUDED.room_number,
    room_status = EXCLUDED.room_status,
    room_type = EXCLUDED.room_type,
    avatar_url = EXCLUDED.avatar_url;

-- ROOM SEASONAL RATES
INSERT INTO room_seasonal_rates (
    id, hotel_id, room_type, rate_name, start_date, end_date, price
) 
VALUES 
(1, 1, 'STANDARD', 'Giá ngày thường 22/09', '2026-09-22', '2026-09-22', 600000.00),
(2, 1, 'STANDARD', 'Giá ngày thường 23/09', '2026-09-23', '2026-09-23', 620000.00),
(3, 1, 'STANDARD', 'Giá ngày thường 24/09', '2026-09-24', '2026-09-24', 650000.00),
(4, 1, 'STANDARD', 'Giá cận cuối tuần 25/09', '2026-09-25', '2026-09-25', 750000.00),
(5, 1, 'STANDARD', 'Giá cuối tuần Thứ Bảy 26/09', '2026-09-26', '2026-09-26', 900000.00),
(6, 1, 'STANDARD', 'Giá Chủ Nhật 27/09', '2026-09-27', '2026-09-27', 700000.00),
(7, 1, 'DELUXE', 'Giá ngày thường 22/09', '2026-09-22', '2026-09-22', 900000.00),
(8, 1, 'DELUXE', 'Giá ngày thường 23/09', '2026-09-23', '2026-09-23', 930000.00),
(9, 1, 'DELUXE', 'Giá ngày thường 24/09', '2026-09-24', '2026-09-24', 950000.00),
(10, 1, 'DELUXE', 'Giá cận cuối tuần 25/09', '2026-09-25', '2026-09-25', 1100000.00),
(11, 1, 'DELUXE', 'Giá cuối tuần Thứ Bảy 26/09', '2026-09-26', '2026-09-26', 1350000.00),
(12, 1, 'DELUXE', 'Giá Chủ Nhật 27/09', '2026-09-27', '2026-09-27', 1000000.00)
ON CONFLICT (id) DO UPDATE SET 
    price = EXCLUDED.price,
    rate_name = EXCLUDED.rate_name;

-- ROOM PRICE HISTORIES
INSERT INTO room_price_histories (
    seasonal_rate_id, account_id, old_price, new_price, changed_at
) 
VALUES 
(1, 'ACC202609222262', 550000.00, 600000.00, '2026-09-22 09:30:00'),
(2, 'ACC202609222262', 580000.00, 620000.00, '2026-09-22 09:32:00'),
(7, 'ACC202609222262', 850000.00, 900000.00, '2026-09-22 09:35:00')
ON CONFLICT DO NOTHING;
