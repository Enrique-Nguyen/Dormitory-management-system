package vn.edu.tlu.dms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

import vn.edu.tlu.dms.common.constants.DmsConstants;

import java.util.TimeZone;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class DmsApplication {

    public static void main(String[] args) {
        // Đặt trước khi Spring tạo DataSource/Flyway. Một số JDK trên Windows mặc định
        // dùng tên cũ Asia/Saigon mà bộ PostgreSQL local không nhận khi mở kết nối.
        TimeZone.setDefault(TimeZone.getTimeZone(DmsConstants.APPLICATION_TIME_ZONE));
        SpringApplication.run(DmsApplication.class, args);
    }
}
