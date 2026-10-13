package vn.edu.tlu.dms.account.security;

import java.io.Serializable;

public record DmsPrincipal(Long id, String username) implements Serializable {

    private static final long serialVersionUID = 1L;
}
