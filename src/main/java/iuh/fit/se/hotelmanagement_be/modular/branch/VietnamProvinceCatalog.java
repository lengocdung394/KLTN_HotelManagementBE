package iuh.fit.se.hotelmanagement_be.modular.branch;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class VietnamProvinceCatalog {
    private static final List<String> NAMES = List.of(
            "Lai Châu",
            "Điện Biên",
            "Lào Cai",
            "Tuyên Quang",
            "Cao Bằng",
            "Sơn La",
            "Lạng Sơn",
            "Thái Nguyên",
            "Phú Thọ",
            "Bắc Ninh",
            "Quảng Ninh",
            "Hà Nội",
            "Hải Phòng",
            "Hưng Yên",
            "Ninh Bình",
            "Thanh Hóa",
            "Nghệ An",
            "Hà Tĩnh",
            "Quảng Trị",
            "Thành phố Huế",
            "Thành phố Đà Nẵng",
            "Quảng Ngãi",
            "Gia Lai",
            "Đắk Lắk",
            "Khánh Hòa",
            "Lâm Đồng",
            "Đồng Nai",
            "Tây Ninh",
            "Thành phố Hồ Chí Minh",
            "Đồng Tháp",
            "An Giang",
            "Thành phố Cần Thơ",
            "Vĩnh Long",
            "Cà Mau"
    );
    private static final Map<String, String> CANONICAL_NAMES = createCanonicalNames();

    private VietnamProvinceCatalog() {}

    public static List<String> names() {
        return NAMES;
    }

    public static Optional<String> canonicalName(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        return Optional.ofNullable(CANONICAL_NAMES.get(normalizeName(value)));
    }

    public static String normalizeName(String value) {
        return Pattern.compile("\\p{M}+")
                .matcher(Normalizer.normalize(value, Normalizer.Form.NFD))
                .replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .trim()
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("\\s+", " ")
                .replaceFirst("^(tinh|thanh pho|tp)\\.?\\s+", "");
    }

    private static Map<String, String> createCanonicalNames() {
        Map<String, String> namesByNormalizedValue = new LinkedHashMap<>();
        for (String name : NAMES) {
            namesByNormalizedValue.put(normalizeName(name), name);
        }
        return Map.copyOf(namesByNormalizedValue);
    }
}
