package vn.edu.tlu.dms.registration.api;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.registration.service.PendingItemService;

import java.time.OffsetDateTime;

/** Hộp yêu cầu chờ xử lý của Staff/Admin; chỉ đọc và tổng hợp qua Service công khai. */
@RestController
public class PendingItemController {

    /** Loại yêu cầu trong hộp việc; tên wire cố định cho bộ lọc type. */
    public enum Type {
        APPLICATION,
        EXTENSION,
        TRANSFER,
        SUMMER_CHOICE,
        CONTRACT
    }

    /** Một dòng hộp việc; studentId có thể trống khi nguồn chỉ trả tên sinh viên. */
    public record Item(
            String type,
            Long entityId,
            Long studentId,
            String studentName,
            String status,
            OffsetDateTime submittedAt) {}

    private final PendingItemService service;

    public PendingItemController(PendingItemService service) {
        this.service = service;
    }

    @RequirePrivilege({Permission.CONTRACT_READ, Permission.ROOM_APPLICATION_READ})
    @PostMapping("/api/pending-items/search-by-page")
    public PageResponse<Item> searchByPage(@Valid @RequestBody SearchRequest input) {
        return service.searchByPage(input);
    }
}
