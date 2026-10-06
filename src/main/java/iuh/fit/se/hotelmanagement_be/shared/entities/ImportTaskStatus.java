package iuh.fit.se.hotelmanagement_be.shared.entities;

import java.util.List;

public class ImportTaskStatus {
    private int percent;
    private String message;
    private String status;
    private List<?> details;

    public ImportTaskStatus(int percent, String message, String status) {
        this(percent, message, status, List.of());
    }

    public ImportTaskStatus(int percent, String message, String status, List<?> details) {
        this.percent = percent;
        this.message = message;
        this.status = status;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public int getPercent() {
        return percent;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public List<?> getDetails() {
        return details;
    }
}