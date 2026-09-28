package iuh.fit.se.hotelmanagement_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO) // <-- Thêm dòng này vào

public class HotelManagementBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelManagementBeApplication.class, args);
    }

}
