package iuh.fit.se.hotelmanagement_be.modular.room.responses.responseForExcelRoom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportTaskStatus {

    private int percent;
    private String message;
    private String status; // PROCESSING, SUCCESS, FAILED
    private List<ImportDetailItem> details; // 💡 Danh sách chi tiết lỗi/thành công từng dòng

    // Constructor phụ cho các trạng thái thông thường
    public ImportTaskStatus(int percent, String message, String status) {
        this.percent = percent;
        this.message = message;
        this.status = status;
    }
}
