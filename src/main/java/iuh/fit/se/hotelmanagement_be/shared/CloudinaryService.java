package iuh.fit.se.hotelmanagement_be.shared;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import iuh.fit.se.hotelmanagement_be.shared.enums.ImageCategory;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloudinaryService {

//    // 💡 Tự động tiêm (Inject) đối tượng cấu hình Cloudinary kết nối tài khoản 'ddkokspkn' của bạn
//    Cloudinary cloudinary;
//
//    /**
//     * Hàm dùng chung để tải ảnh lên Cloudinary và đưa vào đúng thư mục (Folder)
//     *
//     * @param file:       File ảnh nhị phân nhận từ client gửi lên (nhân viên, phòng...)
//     * @param folderName: Tên thư mục con muốn lưu trên Web (ví dụ: "avatars", "rooms")
//     * @return Chuỗi đường dẫn URL tuyệt đối có giao thức https:// để lưu vào Database
//     */
//    public String uploadImage(MultipartFile file, String folderName) {
//        try {
//            // 1. Cấu hình các thông số upload (Đút ảnh vào cây thư mục: "hotel-management/tên_thư_mục_con")
//            Map<?, ?> options = ObjectUtils.asMap(
//                    "folder", "hotel-management/" + folderName, // Tên package/folder trên Cloudinary
//                    "overwrite", true,                           // Nếu trùng tên thì ghi đè lên ảnh cũ
//                    "resource_type", "image"                     // Định dạng file là hình ảnh
//            );
//
//            // 2. Chuyển file ảnh sang dạng mảng Byte (file.getBytes()) và đẩy thẳng lên Cloudinary
//            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
//
//            // 3. Bốc chuỗi URL bảo mật (secure_url có chữ https) trả về từ Cloudinary để đem đi lưu DB
//            return uploadResult.get("secure_url").toString();
//
//        } catch (IOException e) {
//            // Trả về lỗi tường minh nếu quá trình truyền file qua internet gặp sự cố
//            throw new RuntimeException("Lỗi nghiêm trọng khi truyền file lên Cloudinary: " + e.getMessage());
//        }
//    }
//
//    // Upload nhiều ảnh
//    public List<String> uploadMultipleImages(List<MultipartFile> files, String folderName) {
//        if (files == null || files.isEmpty()) {
//            return List.of();
//        }
//        return files.stream()
//                .filter(file -> file != null && !file.isEmpty())
//                .map(file -> uploadImage(file, folderName)) // Gọi lại hàm upload 1 ảnh lẻ đã có
//                .collect(Collectors.toList());
//    }

    Cloudinary cloudinary;

    // Pattern để lọc dấu tiếng Việt khi làm slug folder
    static final Pattern DIACRITIC_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public String uploadImage(MultipartFile file, String folderName) {
        try {
            Map<?, ?> options = ObjectUtils.asMap(
                    "folder", "hotel-management/" + folderName,
                    "overwrite", true,
                    "resource_type", "image"
            );

            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            throw new RuntimeException("Lỗi nghiêm trọng khi truyền file lên Cloudinary: " + e.getMessage());
        }
    }

    public List<String> uploadMultipleImages(List<MultipartFile> files, String folderName) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .map(file -> uploadImage(file, folderName))
                .collect(Collectors.toList());
    }

    /**
     * 💡 HÀM CHÍNH: Upload ảnh phân loại theo Chi Nhánh và Danh Mục (Phòng, Nhân viên, Khuyến mãi, Dịch vụ)
     *
     * @param files        Danh sách file ảnh cần tải lên
     * @param branchName   Tên chi nhánh (Ví dụ: "Chi nhánh Quận 1")
     * @param category     Loại danh mục sử dụng Enum (ROOMS, STAFFS, PROMOTIONS, SERVICES)
     * @return Danh sách URL ảnh trên Cloudinary
     */
    public List<String> uploadBranchImages(List<MultipartFile> files, String branchName, ImageCategory category) {
        String safeBranch = toSlug(branchName);

        // Tạo đường dẫn chuẩn: branches/chi-nhanh-quan-1/rooms (hoặc staffs, promotions, services)
        String folderPath = String.format("branches/%s/%s", safeBranch, category.getFolderName());

        return uploadMultipleImages(files, folderPath);
    }

    /**
     * Hàm phụ trợ biến đổi tiếng Việt có dấu thành slug không dấu (An toàn cho đường dẫn URL)
     */
    private String toSlug(String input) {
        if (input == null) return "unknown";
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        String slug = DIACRITIC_PATTERN.matcher(normalized).replaceAll("");
        return slug.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-");
    }
}