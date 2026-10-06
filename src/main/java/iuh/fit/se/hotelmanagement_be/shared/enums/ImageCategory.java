package iuh.fit.se.hotelmanagement_be.shared.enums;

public enum ImageCategory {
    ROOMS("rooms"),
    STAFFS("staffs"),
    PROMOTIONS("promotions"),
    SERVICES("services");

    private final String folderName;

    ImageCategory(String folderName) {
        this.folderName = folderName;
    }

    public String getFolderName() {
        return folderName;
    }
}
