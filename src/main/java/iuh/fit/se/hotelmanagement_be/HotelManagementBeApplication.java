package iuh.fit.se.hotelmanagement_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HotelManagementBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelManagementBeApplication.class, args);
    }

}
