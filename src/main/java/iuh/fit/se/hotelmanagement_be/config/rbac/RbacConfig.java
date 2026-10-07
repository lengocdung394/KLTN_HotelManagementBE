package iuh.fit.se.hotelmanagement_be.config.rbac;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class RbacConfig {
    private List<PermissionConfig> permissions = new ArrayList<>();

    @Getter
    @Setter
    public static class PermissionConfig {
        private String code;
        private String name;
        private String description;
    }
}