package vn.edu.tlu.dms;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import vn.edu.tlu.dms.common.constants.DmsConstants;

import java.util.TimeZone;

/** PostgreSQL thật cho test tích hợp (SAD mục 11, AD-09). Cần Docker đang chạy. */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /** Test không gọi main; chuẩn hóa múi giờ trước khi Flyway mở kết nối như lúc chạy ứng dụng. */
    @Bean
    static BeanFactoryPostProcessor applicationTimeZone() {
        return beanFactory ->
                TimeZone.setDefault(TimeZone.getTimeZone(DmsConstants.APPLICATION_TIME_ZONE));
    }

    @Bean
    @ServiceConnection
    @ConditionalOnProperty(
            name = "dms.test.external-db",
            havingValue = "false",
            matchIfMissing = true)
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    }
}
