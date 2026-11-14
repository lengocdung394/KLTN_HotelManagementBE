package iuh.fit.se.hotelmanagement_be.modular.branch.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.PublicHotelResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.services.PublicHotelService;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicHotelServiceImpl implements PublicHotelService {
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PublicHotelResponse> getHotels() {
        return hotelRepository.findAllWithPublicDetails().stream()
                .map(this::toResponse)
                .toList();
    }

    private PublicHotelResponse toResponse(Hotel hotel) {
        Map<iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType, BranchRoomPolicy> policyMap =
                hotel.getRoomPolicies() == null
                        ? Map.of()
                        : hotel.getRoomPolicies().stream()
                                .filter(p -> p.getRoomType() != null)
                                .collect(Collectors.toMap(
                                        BranchRoomPolicy::getRoomType,
                                        p -> p,
                                        (a, b) -> a
                                ));

        List<Room> hotelRooms = hotel.getId() == null
                ? List.of()
                : roomRepository.findByFloor_Building_Hotel_Id(hotel.getId());

        Double minRoomTotalPrice = hotelRooms == null || hotelRooms.isEmpty()
                ? null
                : hotelRooms.stream()
                        .map(room -> room.calculateRoomTotalPrice(room, policyMap.get(room.getRoomType())))
                        .filter(price -> price != null && price > 0)
                        .min(Comparator.naturalOrder())
                        .orElse(null);

        Double startingPrice = minRoomTotalPrice != null
                ? minRoomTotalPrice
                : (hotel.getRoomPolicies() == null
                        ? null
                        : hotel.getRoomPolicies().stream()
                                .map(BranchRoomPolicy::getBasePrice)
                                .filter(price -> price != null)
                                .min(Comparator.naturalOrder())
                                .orElse(null));

        return PublicHotelResponse.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .address(hotel.getAddress())
                .phone(hotel.getPhone())
                .provinceName(hotel.getProvince() == null ? null : hotel.getProvince().getName())
                .startingPrice(startingPrice)
                .imageUrls(hotel.getImageUrls() == null ? List.of() : List.copyOf(hotel.getImageUrls()))
                .build();
    }
}
