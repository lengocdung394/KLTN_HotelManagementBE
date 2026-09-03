package iuh.fit.se.hotelmanagement_be.shared;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloudinaryService {

    // 💡 Tự động tiêm (Inject) đối tượng cấu hình Cloudinary kết nối tài khoản 'ddkokspkn' của bạn
    Cloudinary cloudinary;

    /**
     * Hàm dùng chung để tải ảnh lên Cloudinary và đưa vào đúng thư mục (Folder)
     * @param file: File ảnh nhị phân nhận từ client gửi lên (nhân viên, phòng...)
     * @param folderName: Tên thư mục con muốn lưu trên Web (ví dụ: "avatars", "rooms")
     * @return Chuỗi đường dẫn URL tuyệt đối có giao thức https:// để lưu vào Database
     */
    public String uploadImage(MultipartFile file, String folderName) {
        try {
            // 1. Cấu hình các thông số upload (Đút ảnh vào cây thư mục: "hotel-management/tên_thư_mục_con")
            Map<?, ?> options = ObjectUtils.asMap(
                    "folder", "hotel-management/" + folderName, // Tên package/folder trên Cloudinary
                    "overwrite", true,                           // Nếu trùng tên thì ghi đè lên ảnh cũ
                    "resource_type", "image"                     // Định dạng file là hình ảnh
            );

            // 2. Chuyển file ảnh sang dạng mảng Byte (file.getBytes()) và đẩy thẳng lên Cloudinary
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);

            // 3. Bốc chuỗi URL bảo mật (secure_url có chữ https) trả về từ Cloudinary để đem đi lưu DB
            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            // Trả về lỗi tường minh nếu quá trình truyền file qua internet gặp sự cố
            throw new RuntimeException("Lỗi nghiêm trọng khi truyền file lên Cloudinary: " + e.getMessage());
        }
    }
}