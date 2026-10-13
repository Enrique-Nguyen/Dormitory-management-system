package vn.edu.tlu.dms.account.service;

import vn.edu.tlu.dms.account.api.AccountDtos.Create;
import vn.edu.tlu.dms.account.api.AccountDtos.Profile;
import vn.edu.tlu.dms.account.api.AccountDtos.ProfileInput;
import vn.edu.tlu.dms.account.api.AccountDtos.TemporaryPassword;
import vn.edu.tlu.dms.account.api.AccountDtos.Update;
import vn.edu.tlu.dms.common.dto.PageResponse;

import java.util.List;

public interface UserService {

    PageResponse<Profile> list(int page, int size, List<String> sort, String q, String role);

    Profile get(Long id);

    TemporaryPassword create(Create input);

    Profile update(Long id, Update input);

    void lock(Long id, String reason);

    void unlock(Long id);

    TemporaryPassword resetPassword(Long id);

    void crossUnitAccess(Long id, boolean enabled);

    Profile profile();

    Profile updateProfile(ProfileInput input);
}
