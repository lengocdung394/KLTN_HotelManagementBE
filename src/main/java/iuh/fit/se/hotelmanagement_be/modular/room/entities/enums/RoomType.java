package iuh.fit.se.hotelmanagement_be.modular.room.entities.enums;

public enum RoomType {
    STANDARD("Phòng Tiêu Chuẩn"),
    DELUXE("Phòng Cao Cấp"),
    SUITE("Phòng Thượng Hạng"),
    FAMILY("Phòng Gia Đình");

    private final String displayName;

    RoomType(String displayName) {
        this.displayName = displayName;
    }
}
