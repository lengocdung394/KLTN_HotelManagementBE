package iuh.fit.se.hotelmanagement_be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
@Configuration
public class AsyncConfig {
    @Bean(name = "importTaskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);   // Số luồng tối thiểu luôn chạy sẵn
        executor.setMaxPoolSize(8);    // Số luồng tối đa được phép phình ra khi hệ thống bận
        executor.setQueueCapacity(50); // Hàng đợi chứa các tác vụ phải đợi nếu tất cả luồng đang bận
        executor.setThreadNamePrefix("ImportThread-");
        executor.initialize();
        return executor;
    }
}
