package iuh.fit.se.hotelmanagement_be.config.rbac;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class RbacConfigLoader {

    @Bean
    public RbacConfig rbacConfig() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream input = getClass().getClassLoader().getResourceAsStream("rbac.json");
        return mapper.readValue(input, RbacConfig.class);
    }
}
