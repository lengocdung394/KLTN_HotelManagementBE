package iuh.fit.se.hotelmanagement_be.modular.branch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VietnamProvinceCatalogTest {
    @Test
    void containsExactlyTheFixedCurrentProvinceList() {
        assertEquals(34, VietnamProvinceCatalog.names().size());
        assertTrue(VietnamProvinceCatalog.names().stream()
                .allMatch(name -> VietnamProvinceCatalog.canonicalName(name).orElse("").equals(name)));
    }

    @Test
    void canonicalizesProvinceNameFormatting() {
        assertEquals(
                "Thành phố Hồ Chí Minh",
                VietnamProvinceCatalog.canonicalName("  TP. Hồ Chí Minh  ").orElseThrow());
    }

    @Test
    void rejectsNamesOutsideTheFixedProvinceList() {
        assertTrue(VietnamProvinceCatalog.canonicalName("Bình Dương").isEmpty());
        assertTrue(VietnamProvinceCatalog.canonicalName("Unknown Province").isEmpty());
        assertTrue(VietnamProvinceCatalog.canonicalName(null).isEmpty());
    }
}
