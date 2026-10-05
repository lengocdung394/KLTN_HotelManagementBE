package iuh.fit.se.hotelmanagement_be.shared.entities;

public class ImportTaskStatus {
    private int percent;
    private String message;
    private String status;

    public ImportTaskStatus(int percent, String message, String status) {
        this.percent = percent;
        this.message = message;
        this.status = status;
    }

    public int getPercent() { return percent; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
}
