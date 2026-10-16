package vn.edu.tlu.dms.common.security;

/** account cung cấp implementation; common không phụ thuộc module tầng cao hơn. */
public interface CurrentUser {

    Actor get();
}
