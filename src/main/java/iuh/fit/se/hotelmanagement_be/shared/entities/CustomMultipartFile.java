package iuh.fit.se.hotelmanagement_be.shared.entities;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;

public class CustomMultipartFile implements MultipartFile {

    private final byte[] fileData;
    private final String fileName;
    private final String contentType;

    public CustomMultipartFile(byte[] fileData, String fileName) {
        this.fileData = fileData;
        this.fileName = fileName;
        this.contentType = determineContentType(fileName);
    }

    private String determineContentType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg"; // Mặc định cho jpg, jpeg
    }

    @Override
    public String getName() {
        return fileName;
    }

    @Override
    public String getOriginalFilename() {
        return fileName;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return fileData == null || fileData.length == 0;
    }

    @Override
    public long getSize() {
        return fileData.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return fileData;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(fileData);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(fileData);
        }
    }
}
