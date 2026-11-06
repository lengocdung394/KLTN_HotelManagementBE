package iuh.fit.se.hotelmanagement_be.modular.branch.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.PublicHotelResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.services.PublicHotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicHotelServiceImpl implements PublicHotelService {
    private final HotelRepository hotelRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PublicHotelResponse> getHotels() {
        return hotelRepository.findAllWithPublicDetails().stream()
                .map(this::toResponse)
                .toList();
    }

    private PublicHotelResponse toResponse(Hotel hotel) {
        Double startingPrice = hotel.getRoomPolicies() == null
                ? null
                : hotel.getRoomPolicies().stream()
                        .map(policy -> policy.getBasePrice())
                        .filter(price -> price != null)
                        .min(Comparator.naturalOrder())
                        .orElse(null);

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
