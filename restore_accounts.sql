-- Restore accounts missing from ddl-auto=create wipe
INSERT INTO account (account_id, email, password) VALUES
('ACC_104', 'letan.saigon@senviet.vn', '$2a$10$F4tERFmHNEB/wPlByP9qFuOX6c.d.NUY2T6ePRdgjirue.j5Ert1O'),
('ACC_105', 'letan2.saigon@senviet.vn', '$2a$10$F4tERFmHNEB/wPlByP9qFuOX6c.d.NUY2T6ePRdgjirue.j5Ert1O'),
('ACC_106', 'buongphong.saigon@senviet.vn', '$2a$10$v0je6025alNynRTcq1.ahOARxHZp4xAqpMbS3B2p.WQfP4TWugEUi'),
('ACC_107', 'letan.hanoi@senviet.vn', '$2a$10$F4tERFmHNEB/wPlByP9qFuOX6c.d.NUY2T6ePRdgjirue.j5Ert1O'),
('ACC_108', 'buongphong.hanoi@senviet.vn', '$2a$10$v0je6025alNynRTcq1.ahOARxHZp4xAqpMbS3B2p.WQfP4TWugEUi'),
('ACC_109', 'letan.danang@senviet.vn', '$2a$10$F4tERFmHNEB/wPlByP9qFuOX6c.d.NUY2T6ePRdgjirue.j5Ert1O'),
('ACC_110', 'buongphong.danang@senviet.vn', '$2a$10$v0je6025alNynRTcq1.ahOARxHZp4xAqpMbS3B2p.WQfP4TWugEUi'),
('ACC_201', 'customer@senviet.vn', '$2a$10$667bxb/28V3osb4so0APy.5n/xBS70FmPFS6uTpKcDnycVzz11GF.'),
('ACC_202', 'nguyenvana@gmail.com', '$2a$10$667bxb/28V3osb4so0APy.5n/xBS70FmPFS6uTpKcDnycVzz11GF.'),
('ACC_203', 'tranthib@gmail.com', '$2a$10$667bxb/28V3osb4so0APy.5n/xBS70FmPFS6uTpKcDnycVzz11GF.'),
('ACC_204', 'lehoangc@gmail.com', '$2a$10$667bxb/28V3osb4so0APy.5n/xBS70FmPFS6uTpKcDnycVzz11GF.'),
('ACC_205', 'phamminhduc@gmail.com', '$2a$10$667bxb/28V3osb4so0APy.5n/xBS70FmPFS6uTpKcDnycVzz11GF.')
ON CONFLICT (account_id) DO NOTHING;

-- Map roles
INSERT INTO account_role (account_id, role_id) VALUES
('ACC_104', 6), -- letan -> RECEPTIONIST
('ACC_105', 6), -- letan2 -> RECEPTIONIST
('ACC_106', 6), -- buongphong -> fallback RECEPTIONIST
('ACC_107', 6),
('ACC_108', 6),
('ACC_109', 6),
('ACC_110', 6),
('ACC_201', 4), -- customer -> CUSTOMER
('ACC_202', 4),
('ACC_203', 4),
('ACC_204', 4),
('ACC_205', 4)
ON CONFLICT DO NOTHING;

-- Employees
INSERT INTO employees (employee_id, full_name, phone, position, cccd, address, date_of_birth, hotel_id, account_id) VALUES
('EMP_104', 'Lê Thị Thu Thảo', '0903333331', 'Lễ Tân Trưởng (Ca Sáng 06h-14h)', '079301004444', '12 Nguyễn Đình Chiểu, Q.3, TP.HCM', '1998-11-04', 1, 'ACC_104'),
('EMP_105', 'Nguyễn Quốc Bảo', '0903333332', 'Nhân Viên Lễ Tân (Ca Tối 14h-22h)', '079301005555', '56 Cách Mạng Tháng 8, Q.10, TP.HCM', '2000-02-18', 1, 'ACC_105'),
('EMP_106', 'Đặng Thị Cẩm Tú', '0904444441', 'Nhân Viên Buồng Phòng', '079301006666', '78 Bình Thới, Q.11, TP.HCM', '1995-09-25', 1, 'ACC_106'),
('EMP_107', 'Phạm Quỳnh Nga', '0905555551', 'Nhân Viên Lễ Tân', '001301007777', '24 Hàng Bông, Hoàn Kiếm, Hà Nội', '1999-07-10', 2, 'ACC_107'),
('EMP_108', 'Bùi Văn Hùng', '0906666661', 'Nhân Viên Buồng Phòng', '001301008888', '15 Đại Cồ Việt, Hai Bà Trưng, Hà Nội', '1996-12-30', 2, 'ACC_108'),
('EMP_109', 'Trần Đình Trọng', '0907777771', 'Nhân Viên Lễ Tân', '048301009999', '28 Bạch Đằng, Hải Châu, Đà Nẵng', '1997-04-12', 3, 'ACC_109'),
('EMP_110', 'Hoàng Thị Mỹ Linh', '0908888881', 'Nhân Viên Buồng Phòng', '048301001010', '42 Nguyễn Văn Linh, Đà Nẵng', '1996-08-22', 3, 'ACC_110')
ON CONFLICT (employee_id) DO NOTHING;

-- Customers
INSERT INTO customers (customer_id, full_name, email, phone, cccd, loyalty_tier, total_spent, total_bookings, account_id, is_registered) VALUES
('CUS_201', 'Huỳnh Văn Hiếu', 'customer@senviet.vn', '0901234567', '079203001234', 'BRONZE', 1000000.0, 1, 'ACC_201', true),
('CUS_202', 'Nguyễn Văn An', 'nguyenvana@gmail.com', '0912345678', '079203005678', 'SILVER', 4500000.0, 3, 'ACC_202', true),
('CUS_203', 'Trần Thị Bích', 'tranthib@gmail.com', '0923456789', '079203009876', 'GOLD', 12800000.0, 6, 'ACC_203', true),
('CUS_204', 'Lê Hoàng Cường', 'lehoangc@gmail.com', '0934567890', '079203004321', 'PLATINUM', 35600000.0, 14, 'ACC_204', true),
('CUS_205', 'Phạm Minh Đức', 'phamminhduc@gmail.com', '0945678901', '079203008765', 'BRONZE', 0.0, 0, 'ACC_205', true)
ON CONFLICT (customer_id) DO NOTHING;
