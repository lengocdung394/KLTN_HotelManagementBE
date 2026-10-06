package iuh.fit.se.hotelmanagement_be.shared.entities;

import java.io.InputStream;
import java.util.Map;

public class ZipExtractionResult {
    private final InputStream excelInputStream;
    private final Map<String, byte[]> imageFilesMap;

    public ZipExtractionResult(InputStream excelInputStream, Map<String, byte[]> imageFilesMap) {
        this.excelInputStream = excelInputStream;
        this.imageFilesMap = imageFilesMap;
    }

    public InputStream getExcelInputStream() { return excelInputStream; }
    public Map<String, byte[]> getImageFilesMap() { return imageFilesMap; }
}
