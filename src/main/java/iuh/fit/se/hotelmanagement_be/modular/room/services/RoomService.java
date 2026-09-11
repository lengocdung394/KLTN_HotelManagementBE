package iuh.fit.se.hotelmanagement_be.modular.room.services;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.RoomCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomCreateResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomTypeDetailResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RoomService {
    RoomCreateResponse createRoom(RoomCreateRequest dto, List<MultipartFile> imageFiles);
    List<RoomResponse> getRoomsByFloorId(Long floorId);
    List<RoomResponse> getRoomsByHotelId(Long hotelId);
    RoomTypeDetailResponse getRoomTypeDetailByHotelAndType(Long hotelId, RoomType roomType);
}
