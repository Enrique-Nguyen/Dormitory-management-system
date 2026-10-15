package vn.edu.tlu.dms.billing.service.impl;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.api.UtilityRateDtos;
import vn.edu.tlu.dms.billing.domain.UtilityRate;
import vn.edu.tlu.dms.billing.repository.UtilityRateRepository;
import vn.edu.tlu.dms.billing.service.UtilityRateService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;

import java.util.List;

/** Đơn giá có hiệu lực theo thời gian; thêm bản mới thay vì ghi đè giá đã được dùng. */
@Service
@Transactional(readOnly = true)
public class UtilityRateServiceImpl implements UtilityRateService {

    private final UtilityRateRepository repository;

    private final CurrentUser currentUser;

    private final ScopeGuard scopeGuard;

    public UtilityRateServiceImpl(
            UtilityRateRepository repository, CurrentUser currentUser, ScopeGuard scopeGuard) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.scopeGuard = scopeGuard;
    }

    @Override
    public List<UtilityRateDtos.Response> getUtilityRates(String meterType) {
        scopeGuard.requirePermission(Permission.UTILITY_RATE_READ);
        scopeGuard.requireRole(Role.STAFF);
        return repository
                .findAll(
                        (root, query, builder) ->
                                meterType == null
                                        ? builder.conjunction()
                                        : builder.equal(root.get("meterType"), meterType),
                        Sort.by(Sort.Direction.DESC, "effectiveFrom", "id"))
                .stream()
                .map(this::toUtilityRateResponse)
                .toList();
    }

    @Transactional
    @Override
    public UtilityRateDtos.Response createUtilityRate(UtilityRateDtos.Create input) {
        scopeGuard.requirePermission(Permission.UTILITY_RATE_CREATE);
        scopeGuard.requireRole(Role.STAFF);
        UtilityRate rate = new UtilityRate();
        rate.setMeterType(input.meterType());
        rate.setUnitPrice(input.unitPrice());
        rate.setEffectiveFrom(input.effectiveFrom());
        rate.setCreatedBy(currentUser.get().id());
        return toUtilityRateResponse(repository.saveAndFlush(rate));
    }

    private UtilityRateDtos.Response toUtilityRateResponse(UtilityRate rate) {
        return new UtilityRateDtos.Response(
                rate.getId(), rate.getMeterType(), rate.getUnitPrice(), rate.getEffectiveFrom());
    }
}
