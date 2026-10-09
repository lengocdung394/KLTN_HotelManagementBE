package iuh.fit.se.hotelmanagement_be.modular.room.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.BedType;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.BedTypeRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.BedTypeRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.BedTypeGetAllResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.services.BedTypeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    @Override
    @Transactional
    public BedTypeGetAllResponse create(BedTypeRequest request) {
        validateRequest(request);
        String name = request.getName().trim();
        if (bedTypeRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên loại giường đã tồn tại.");
        }
        return toResponse(bedTypeRepository.save(toEntity(request, name)));
    }

    @Override
    @Transactional
    public BedTypeGetAllResponse update(Long id, BedTypeRequest request) {
        validateRequest(request);
        BedType bedType = bedTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy loại giường."));
        String name = request.getName().trim();
        if (bedTypeRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên loại giường đã tồn tại.");
        }
        bedType.setName(name);
        bedType.setDescription(normalizeDescription(request.getDescription()));
        bedType.setCapacity(request.getCapacity());
        bedType.setIsExtraBed(request.getIsExtraBed());
        return toResponse(bedTypeRepository.save(bedType));
    }

    @Override
    @Transactional
    public List<BedTypeGetAllResponse> importAll(List<BedTypeRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File không có dữ liệu loại giường.");
        }
        Set<String> names = new HashSet<>();
        for (BedTypeRequest request : requests) {
            validateRequest(request);
            String name = request.getName().trim();
            if (!names.add(name.toLowerCase(Locale.ROOT)) || bedTypeRepository.existsByNameIgnoreCase(name)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Tên loại giường \"" + name + "\" bị trùng trong file hoặc đã tồn tại.");
            }
        }
        List<BedType> saved = bedTypeRepository.saveAll(
                requests.stream().map(request -> toEntity(request, request.getName().trim())).toList());
        return saved.stream().map(this::toResponse).toList();
    }

    private void validateRequest(BedTypeRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()
                || request.getCapacity() == null || request.getCapacity() < 1
                || request.getIsExtraBed() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Mỗi loại giường cần tên, sức chứa từ 1 trở lên và trạng thái giường phụ.");
        }
    }

    private BedType toEntity(BedTypeRequest request, String name) {
        return BedType.builder()
                .name(name)
                .description(normalizeDescription(request.getDescription()))
                .capacity(request.getCapacity())
                .isExtraBed(request.getIsExtraBed())
                .build();
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    private BedTypeGetAllResponse toResponse(BedType bedType) {
        return BedTypeGetAllResponse.builder()
                .id(bedType.getId())
                .name(bedType.getName())
                .description(bedType.getDescription())
                .capacity(bedType.getCapacity())
                .isExtraBed(bedType.getIsExtraBed())
                .build();
    }


}
