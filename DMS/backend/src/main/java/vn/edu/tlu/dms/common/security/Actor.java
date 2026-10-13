package vn.edu.tlu.dms.common.security;

public record Actor(
        Long id, String role, Long unitId, boolean crossUnitAccess, boolean mustChangePassword) {

    public boolean is(String value) {
        return role.equals(value);
    }
}
