package vn.edu.tlu.dms.common.config;

import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình Jackson từ chối ép kiểu chuỗi/boolean sang số ở ranh giới DTO (ID, phân trang, số
 * lượng, đơn giá), trả về HTTP 400 VALIDATION_FAILED thay vì ép ngầm.
 */
@Configuration
public class JacksonConfig {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer strictNumericDeserializationCustomizer() {
        return builder ->
                builder.postConfigurer(
                        mapper -> {
                            for (LogicalType type :
                                    new LogicalType[] {LogicalType.Integer, LogicalType.Float}) {
                                mapper.coercionConfigFor(type)
                                        .setCoercion(
                                                CoercionInputShape.String, CoercionAction.Fail)
                                        .setCoercion(
                                                CoercionInputShape.EmptyString,
                                                CoercionAction.Fail)
                                        .setCoercion(
                                                CoercionInputShape.Boolean, CoercionAction.Fail);
                            }
                        });
    }
}
