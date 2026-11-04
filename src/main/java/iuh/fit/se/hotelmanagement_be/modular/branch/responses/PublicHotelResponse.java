package iuh.fit.se.hotelmanagement_be.modular.branch.responses;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PublicHotelResponse {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private String provinceName;
    private Double startingPrice;
    private List<String> imageUrls;
}
