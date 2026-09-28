# TÀI LIỆU HƯỚNG DẪN DỮ LIỆU MẪU KIỂM THỬ TOÀN DIỆN & SWAGGER API
## HỆ THỐNG QUẢN LÝ CHUỖI KHÁCH SẠN SEN VIỆT LUXURY
**Dự án:** Khóa luận tốt nghiệp - Hệ thống Quản lý Chuỗi Khách sạn Sen Việt  
**Người chuẩn bị:** Nhóm KLTN  
**Gửi cho:** Lê Ngọc Dung & Bạn (Phục vụ buổi họp test luồng nhân viên, khách hàng và viết BE)  
**Tệp SQL đính kèm:** `C:\Users\admin\Downloads\KLTN_HotelManagementBE\sample_data_test.sql`

---

## 1. TỔNG HỢP DỮ LIỆU ĐÃ NẠP (DATABASE POSTGRESQL) - ĐỒNG BỘ 100% GIAO DIỆN HỆ THỐNG

Danh sách khách sạn và tỉnh thành được **đồng bộ 100% nguyên bản theo đúng hình ảnh giao diện UI của bạn**:

| Khu vực | Tên Khách sạn | Tỉnh / Thành phố | Địa chỉ |
| :--- | :--- | :--- | :--- |
| **Miền Bắc** | **Sen Việt Hà Nội** | Hà Nội | 45 Tràng Tiền, Hoàn Kiếm, Hà Nội |
| **Miền Trung** | **Sen Việt An Nhơn** | Bình Định | Đường Quang Trung, TX. An Nhơn, Bình Định |
| **Miền Trung** | **Sen Việt Đà Nẵng** | Đà Nẵng | Võ Nguyên Giáp, Ngũ Hành Sơn, Đà Nẵng |
| **Miền Trung** | **Sen Việt Nha Trang** | Khánh Hòa | Trần Phú, Lộc Thọ, Nha Trang, Khánh Hòa |
| **Miền Trung** | **Sen Việt Đà Lạt** | Lâm Đồng | Trần Hưng Đạo, Phường 10, Đà Lạt, Lâm Đồng |
| **Miền Nam** | **Sen Việt Gò Công** | Tiền Giang | Đường Nguyễn Trãi, TX. Gò Công, Tiền Giang |
| **Miền Nam** | **Sen Việt Sài Gòn** | TP. Hồ Chí Minh | 123 Lê Lợi, Quận 1, TP. Hồ Chí Minh |

---

## 2. BẢNG THỐNG KÊ CHI TIẾT CÁC HẠNG MỤC DỮ LIỆU

| Hạng mục dữ liệu | Số lượng | Chi tiết / Điểm nhấn |
| :--- | :---: | :--- |
| **Tỉnh / Thành phố** | **7** | TP.HCM, Hà Nội, Đà Nẵng, Khánh Hòa, Lâm Đồng, Tiền Giang, Bình Định |
| **Khách sạn** | **7** | Đúng tên nguyên bản theo giao diện thẻ khách sạn của bạn |
| **Chính sách phòng (Policies)** | **28** | Mỗi khách sạn có đủ 4 loại phòng: STANDARD, DELUXE, SUITE, FAMILY |
| **Danh mục phòng (Rooms)** | **56 phòng** | Trải đều khắp các chi nhánh, có đủ các trạng thái để test |
| **Hình ảnh phòng (`avatar_url`)** | **4 - 8 ảnh / phòng** | Mỗi phòng đều có **từ 5 đến 7 ảnh 4K Unsplash** lưu dạng JSON array |
| **Tiện nghi phòng (Amenities)** | **8 tiện ích gốc** | **GIỮ NGUYÊN 100% BỘ 8 TIỆN NGHI GỐC**, không thay đổi ID hay thêm tiện ích lạ |
| **Dịch vụ khách sạn (Services)** | **15 dịch vụ** | Buffet sáng, Set Menu Rooftop, Massage Sen Việt, Xe đón sân bay, Thuê xe máy... |
| **Khuyến mãi (Promotions)** | **7 ưu đãi** | **Hiệu lực mở rộng từ tháng 9 đến tháng 12: 01/09/2026 – 31/12/2026** |
| **Khách hàng & Nhân viên** | **15 tài khoản** | **100% có số CCCD 12 số thực tế**, phân hạng Loyalty (Bronze, Silver, Gold, Platinum) |

---

## 3. DANH SÁCH 8 TIỆN NGHI GỐC (GIỮ NGUYÊN)

| ID | Tên tiện ích | Đơn giá | Áp dụng cho loại phòng |
| :---: | :--- | :---: | :--- |
| **1** | Wifi 6 High-Speed 500Mbps | 0 đ | Tất cả các phòng (Standard, Deluxe, Suite, Family) |
| **2** | Smart TV 55 inch 4K Ultra HD | 0 đ | Tất cả các phòng (Standard, Deluxe, Suite, Family) |
| **3** | Bồn tắm nằm thảo dược cao cấp | 50.000 đ | Deluxe, Suite |
| **4** | Ban công view toàn cảnh thành phố | 0 đ | Deluxe, Suite, Family |
| **5** | Két sắt điện tử bảo mật cao | 0 đ | Tất cả các phòng (Standard, Deluxe, Suite, Family) |
| **6** | Máy pha cà phê Espresso tự động | 40.000 đ | Suite |
| **7** | Tủ lạnh Minibar tiêu chuẩn | 0 đ | Tất cả các phòng (Standard, Deluxe, Suite, Family) |
| **8** | Máy sấy tóc ion âm & Bàn là hơi | 0 đ | Tất cả các phòng (Standard, Deluxe, Suite, Family) |

---

## 4. DANH SÁCH TÀI KHOẢN ĐĂNG NHẬP THEO PHÂN QUYỀN

| STT | Vai trò | Họ tên | Email đăng nhập | Mật khẩu | Chi nhánh / Chức vụ | Ghi chú |
| :---: | :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | **Admin Tổng** | Võ Minh Trí | `admin@senviet.vn` | `admin123` | Toàn hệ thống | Quản lý hệ thống, phân quyền, cấu hình |
| **2** | **Giám Đốc CN Sài Gòn** | Trần Minh Tuấn | `admin.saigon@senviet.vn` | `admin123` | CN Sen Việt Sài Gòn | Quản lý vận hành chi nhánh TP.HCM |
| **3** | **Lễ Tân (Ca Sáng)** | Lê Thị Thu Thảo | `letan.saigon@senviet.vn` | `letan123` | CN Sen Việt Sài Gòn | Ca 06:00 - 14:00 (Check-in, Check-out) |
| **4** | **Lễ Tân (Ca Tối)** | Nguyễn Quốc Bảo | `letan2.saigon@senviet.vn` | `letan123` | CN Sen Việt Sài Gòn | Ca 14:00 - 22:00 (Thêm dịch vụ, giao ca) |
| **5** | **Buồng Phòng Sài Gòn** | Đặng Thị Cẩm Tú | `buongphong.saigon@senviet.vn` | `staff123` | CN Sen Việt Sài Gòn | Xem phòng bẩn, bấm hoàn tất dọn |
| **6** | **Khách hàng thân thiết** | Huỳnh Văn Hiếu | `customer@senviet.vn` | `customer123` | Khách hàng cá nhân | Hội viên Bronze (CCCD: `079203001234`) |
| **7** | **Khách hàng VIP** | Trần Thị Bích | `tranthib@gmail.com` | `customer123` | Khách hàng cá nhân | Hội viên Gold (CCCD: `079203009876`) |
| **8** | **Khách hàng Platinum**| Lê Hoàng Cường | `lehoangc@gmail.com` | `customer123` | Khách hàng cá nhân | Hội viên Platinum (CCCD: `079203004321`) |

---

## 5. HƯỚNG DẪN SWAGGER: BỘ LINK HÌNH ẢNH & PAYLOAD TẠO PHÒNG

Dung có thể test tạo phòng có từ 4 đến 8 tấm ảnh trên Swagger UI (`http://localhost:8081/swagger-ui/index.html`):

### Bộ URL hình ảnh 4-8 tấm cho từng loại phòng:
- **Phòng Standard (5 ảnh):**
  1. `https://images.unsplash.com/photo-1618773928121-c32242e63f39?q=80&w=1200&auto=format&fit=crop` (Ảnh chính)
  2. `https://images.unsplash.com/photo-1591088398332-8a7791972843?q=80&w=1200&auto=format&fit=crop`
  3. `https://images.unsplash.com/photo-1584622650111-993a426fbf0a?q=80&w=1200&auto=format&fit=crop`
  4. `https://images.unsplash.com/photo-1566665797739-1674de7a421a?q=80&w=1200&auto=format&fit=crop`
  5. `https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1200&auto=format&fit=crop`
- **Phòng Deluxe (6 ảnh):**
  1. `https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?q=80&w=1200&auto=format&fit=crop` (Ảnh chính)
  2. `https://images.unsplash.com/photo-1595576508898-0ad5c879a061?q=80&w=1200&auto=format&fit=crop`
  3. `https://images.unsplash.com/photo-1578683010236-d716f9a3f461?q=80&w=1200&auto=format&fit=crop`
  4. `https://images.unsplash.com/photo-1616046229478-9901c5536a45?q=80&w=1200&auto=format&fit=crop`
  5. `https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?q=80&w=1200&auto=format&fit=crop`
  6. `https://images.unsplash.com/photo-1507652313519-d4e9174996dd?q=80&w=1200&auto=format&fit=crop`
- **Phòng Suite (7 ảnh):**
  1. `https://images.unsplash.com/photo-1631049307264-da0ec9d70304?q=80&w=1200&auto=format&fit=crop` (Ảnh chính)
  2. `https://images.unsplash.com/photo-1590381105924-c72589b9ef3f?q=80&w=1200&auto=format&fit=crop`
  3. `https://images.unsplash.com/photo-1618773928121-c32242e63f39?q=80&w=1200&auto=format&fit=crop`
  4. `https://images.unsplash.com/photo-1582719508461-905c673771fd?q=80&w=1200&auto=format&fit=crop`
  5. `https://images.unsplash.com/photo-1540518614846-7eded433c457?q=80&w=1200&auto=format&fit=crop`
  6. `https://images.unsplash.com/photo-1584132967334-10e028bd69f7?q=80&w=1200&auto=format&fit=crop`
  7. `https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?q=80&w=1200&auto=format&fit=crop`
- **Phòng Family (6 ảnh):**
  1. `https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1200&auto=format&fit=crop` (Ảnh chính)
  2. `https://images.unsplash.com/photo-1598928506311-c55ded91a20c?q=80&w=1200&auto=format&fit=crop`
  3. `https://images.unsplash.com/photo-1596394516093-501ba68a0ba6?q=80&w=1200&auto=format&fit=crop`
  4. `https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=1200&auto=format&fit=crop`
  5. `https://images.unsplash.com/photo-1566073771259-6a8506099945?q=80&w=1200&auto=format&fit=crop`
  6. `https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?q=80&w=1200&auto=format&fit=crop`

### Payload JSON mẫu gọi `POST /room/create` trên Swagger:
```json
{
  "floorId": 1,
  "roomStatus": "AVAILABLE",
  "roomType": "DELUXE",
  "defaultImageIndex": 0,
  "amenityIds": [1, 2, 3, 4, 5, 7, 8]
}
```
*(Tiện nghi giữ nguyên đúng các ID tiện ích gốc 1, 2, 3, 4, 5, 7, 8)*
