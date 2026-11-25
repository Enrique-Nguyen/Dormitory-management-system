package vn.edu.tlu.dms.account.api;

/** Thông tin đơn vị quản lý; headUserName chỉ để hiển thị, ghi luôn dùng headUserId. */
public record ManagementUnitResponse(
        Long id,
        String code,
        String name,
        String shortName,
        String address,
        String hotline,
        String email,
        Long headUserId,
        String headUserName,
        String workingHours,
        Boolean active,
        String note) {}
