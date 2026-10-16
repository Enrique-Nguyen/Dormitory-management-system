package vn.edu.tlu.dms.common.security;

import vn.edu.tlu.dms.common.constants.DmsConstants.Role;

public record Actor(
        Long id, String role, Long unitId, boolean crossUnitAccess, boolean mustChangePassword) {

    public boolean is(String value) {
        return role.equals(value);
    }

    /** Admin được làm nghiệp vụ ở mọi đơn vị; không giả làm Staff hoặc Student. */
    public boolean isAdmin() {
        return is(Role.ADMIN);
    }

    /** Đọc liên đơn vị: Admin luôn được đọc; Staff cần cờ tổng hợp do Admin cấp. */
    public boolean canReadAllUnits() {
        return isAdmin() || is(Role.STAFF) && crossUnitAccess;
    }
}
