-- Script hạ giá phòng xuống dưới 5.000 VNĐ để test thanh toán PayOS
-- Lưu ý: PayOS yêu cầu số tiền tối thiểu >= 2.000 VNĐ

UPDATE rooms
SET base_price = CASE
    WHEN UPPER(room_type) = 'STANDARD' THEN 2000
    WHEN UPPER(room_type) = 'DELUXE' THEN 3000
    WHEN UPPER(room_type) = 'FAMILY' THEN 4000
    WHEN UPPER(room_type) = 'SUITE' THEN 4500
    ELSE 3000
END;

UPDATE branch_room_policies
SET base_price = CASE
    WHEN UPPER(room_type) = 'STANDARD' THEN 2000
    WHEN UPPER(room_type) = 'DELUXE' THEN 3000
    WHEN UPPER(room_type) = 'FAMILY' THEN 4000
    WHEN UPPER(room_type) = 'SUITE' THEN 4500
    ELSE 3000
END;

UPDATE room_seasonal_rates
SET price = CASE
    WHEN UPPER(room_type) = 'STANDARD' THEN 2000
    WHEN UPPER(room_type) = 'DELUXE' THEN 3000
    WHEN UPPER(room_type) = 'FAMILY' THEN 4000
    WHEN UPPER(room_type) = 'SUITE' THEN 4500
    ELSE 3000
END;
