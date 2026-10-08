package iuh.fit.se.hotelmanagement_be.modular.auth.services;

import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ImportPermissionCatalogRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.PermissionCatalogItemRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.PermissionCatalogResponse;

import java.util.List;

public interface PermissionCatalogService {
    List<PermissionCatalogResponse> getPermissions();

    PermissionCatalogResponse createPermission(PermissionCatalogItemRequest request);

    List<PermissionCatalogResponse> importPermissions(
            ImportPermissionCatalogRequest request
    );
}
