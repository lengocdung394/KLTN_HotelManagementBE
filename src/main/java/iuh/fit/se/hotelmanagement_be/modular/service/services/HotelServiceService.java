package iuh.fit.se.hotelmanagement_be.modular.service.services;

import iuh.fit.se.hotelmanagement_be.modular.service.requests.CreateServiceRequest;
import iuh.fit.se.hotelmanagement_be.modular.service.requests.UpdateServiceRequest;
import iuh.fit.se.hotelmanagement_be.modular.service.responses.ServiceResponse;

import java.util.List;

public interface HotelServiceService {

    List<ServiceResponse> getAllServices(Long hotelId, String category, Boolean activeOnly);

    ServiceResponse getServiceById(Long id);

    ServiceResponse createService(CreateServiceRequest request);

    ServiceResponse updateService(Long id, UpdateServiceRequest request);

    void deleteService(Long id);

    ServiceResponse toggleServiceStatus(Long id);
}
