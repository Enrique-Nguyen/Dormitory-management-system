package vn.edu.tlu.dms.billing.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.api.UtilityRateDtos;
import vn.edu.tlu.dms.billing.domain.UtilityRate;
import vn.edu.tlu.dms.billing.repository.UtilityRateRepository;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;

import java.util.List;

/** Đơn giá có hiệu lực theo thời gian; thêm bản mới thay vì ghi đè giá đã được dùng. */
@Service
@Transactional(readOnly = true)
public class UtilityRateService {

    private final UtilityRateRepository repository;

    private final CurrentUser currentUser;

    private final ScopeGuard scopeGuard;

    public UtilityRateService(
            UtilityRateRepository repository, CurrentUser currentUser, ScopeGuard scopeGuard) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.scopeGuard = scopeGuard;
    }

    public List<UtilityRateDtos.Response> getUtilityRates(String meterType) {
        scopeGuard.requireRole("STAFF");
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
    public UtilityRateDtos.Response createUtilityRate(UtilityRateDtos.Create input) {
        scopeGuard.requireRole("STAFF");
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
