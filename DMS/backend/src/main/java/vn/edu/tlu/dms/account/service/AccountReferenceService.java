package vn.edu.tlu.dms.account.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.repository.AppUserRepository;
import vn.edu.tlu.dms.account.repository.ManagementUnitRepository;
import vn.edu.tlu.dms.account.repository.StudentProfileRepository;
import vn.edu.tlu.dms.common.exception.BusinessException;

@Service
@Transactional(readOnly = true)
public class AccountReferenceService {

    private final ManagementUnitRepository units;

    private final AppUserRepository users;

    private final StudentProfileRepository students;

    public AccountReferenceService(
            ManagementUnitRepository units,
            AppUserRepository users,
            StudentProfileRepository students) {
        this.units = units;
        this.users = users;
        this.students = students;
    }

    public void requireUnit(Long id) {
        if (id == null || !units.existsById(id)) {
            throw BusinessException.notFound();
        }
    }

    public record UserRef(
            Long id, String fullName, String username, String role, Long unitId, String gender) {}

    public UserRef user(Long id) {
        var u = users.findById(id).orElseThrow(BusinessException::notFound);
        String gender = students.findById(id).map(p -> p.getGender()).orElse(null);
        return new UserRef(
                u.getId(),
                u.getFullName(),
                u.getUsername(),
                u.getRole(),
                u.getManagementUnitId(),
                gender);
    }
}
