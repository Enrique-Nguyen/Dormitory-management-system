package vn.edu.tlu.dms.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.registration.api.ApplicationDtos;
import vn.edu.tlu.dms.room.api.BuildingRequest;

import java.time.LocalDate;

/**
 * Kiểm tra {@link JacksonConfig} từ chối chuỗi số, chuỗi rỗng và boolean ép kiểu sang trường số ở
 * ranh giới DTO, nhưng vẫn giữ nguyên parse ngày giờ và số đúng kiểu.
 */
class DtoTypeCoercionTest {

    private final ObjectMapper mapper = configuredMapper();

    private static ObjectMapper configuredMapper() {
        var builder = new Jackson2ObjectMapperBuilder().modulesToInstall(new JavaTimeModule());
        new JacksonConfig().strictNumericDeserializationCustomizer().customize(builder);
        return builder.build();
    }

    @Test
    void rejectsNumericStringsAndEmptyStringsForIntegerFields() {
        assertThatThrownBy(() -> mapper.readValue("{\"page\":\"1\"}", SearchRequest.class))
                .isInstanceOf(MismatchedInputException.class);
        assertThatThrownBy(() -> mapper.readValue("{\"size\":\"2\"}", SearchRequest.class))
                .isInstanceOf(MismatchedInputException.class);
        assertThatThrownBy(() -> mapper.readValue("{\"id\":\"\"}", SearchRequest.class))
                .isInstanceOf(MismatchedInputException.class);
        assertThatThrownBy(() -> mapper.readValue("{\"page\":true}", SearchRequest.class))
                .isInstanceOf(MismatchedInputException.class);
        assertThatThrownBy(
                        () -> mapper.readValue("{\"id\":\"123\"}", ApplicationDtos.Input.class))
                .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void rejectsNumericStringForDecimalField() {
        assertThatThrownBy(
                        () -> mapper.readValue("{\"floorAreaM2\":\"120.5\"}", BuildingRequest.class))
                .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void acceptsValidNumericAndIsoDateFields() throws Exception {
        SearchRequest req =
                mapper.readValue(
                        "{\"page\":1,\"size\":10,\"billingMonth\":\"2026-11-01\"}",
                        SearchRequest.class);

        assertThat(req.page()).isEqualTo(1);
        assertThat(req.size()).isEqualTo(10);
        assertThat(req.billingMonth()).isEqualTo(LocalDate.of(2026, 11, 1));
    }
}
