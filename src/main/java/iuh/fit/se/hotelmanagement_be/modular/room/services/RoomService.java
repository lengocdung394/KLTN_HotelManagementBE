package iuh.fit.se.hotelmanagement_be.modular.room.services;

import iuh.fit.se.hotelmanagement_be.modular.room.requests.RoomCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomCreateResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RoomService {
    RoomCreateResponse createRoom(RoomCreateRequest dto, List<MultipartFile> imageFiles);
}
