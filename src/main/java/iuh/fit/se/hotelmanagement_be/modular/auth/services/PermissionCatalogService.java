package iuh.fit.se.hotelmanagement_be.modular.auth.services;

import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ImportPermissionCatalogRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.PermissionCatalogItemRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.PermissionCatalogResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.PermissionResponse;

import java.util.List;

public interface PermissionCatalogService {
    List<PermissionCatalogResponse> getPermissions();

    PermissionResponse createPermission(PermissionCatalogItemRequest request);

    List<PermissionCatalogResponse> importPermissions(
            ImportPermissionCatalogRequest request
    );
}
