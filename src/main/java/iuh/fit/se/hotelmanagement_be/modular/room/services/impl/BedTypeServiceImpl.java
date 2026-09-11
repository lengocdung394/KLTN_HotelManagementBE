package iuh.fit.se.hotelmanagement_be.modular.room.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.BedType;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.BedTypeRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.BedTypeGetAllResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.services.BedTypeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BedTypeServiceImpl implements BedTypeService {

    BedTypeRepository bedTypeRepository;

    @Override
    public List<BedTypeGetAllResponse> findAll() {
        List<BedType> bedTypeList = bedTypeRepository.findAll();

        return bedTypeList.stream().map(
                bedType -> {

                    return BedTypeGetAllResponse.builder()
                            .id(bedType.getId())
                            .name(bedType.getName())
                            .description(bedType.getDescription()).build();
                }

        ).toList();

    }
}
