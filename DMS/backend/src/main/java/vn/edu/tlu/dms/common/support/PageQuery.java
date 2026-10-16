package vn.edu.tlu.dms.common.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.exception.BusinessException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PageQuery {

    private PageQuery() {}

    public static Pageable of(
            int page, int size, List<String> sort, Set<String> allowed, String fallback) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw BusinessException.invalid(ApiField.PAGE, ValidationMessage.PAGE_RANGE);
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (String item : normalizeSortParameters(sort, fallback)) {
            String[] parts = item.split(",", -1);
            if (parts.length > 2 || !allowed.contains(parts[0])) {
                throw BusinessException.invalid(
                        ApiField.SORT, ValidationMessage.SORT_FIELD_INVALID);
            }
            String direction = parts.length == 2 ? parts[1] : "asc";
            if (!direction.equals("asc") && !direction.equals("desc")) {
                throw BusinessException.invalid(
                        ApiField.SORT, ValidationMessage.SORT_DIRECTION_INVALID);
            }
            orders.add(new Sort.Order(Sort.Direction.fromString(direction), parts[0]));
        }
        if (orders.stream().noneMatch(order -> order.getProperty().equals("id"))) {
            orders.add(Sort.Order.asc("id"));
        }
        return PageRequest.of(page, size, Sort.by(orders));
    }

    /**
     * Spring MVC có thể tách code,desc thành hai phần khi bind List; ghép lại trước khi kiểm tra.
     */
    private static List<String> normalizeSortParameters(List<String> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return List.of(fallback);
        }
        List<String> result = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            String value = values.get(index);
            if (value == null || value.isBlank()) {
                throw BusinessException.invalid(ApiField.SORT, ValidationMessage.SORT_REQUIRED);
            }
            if (!value.contains(",")
                    && index + 1 < values.size()
                    && Set.of("asc", "desc").contains(values.get(index + 1))) {
                value = value + "," + values.get(++index);
            }
            result.add(value);
        }
        return result;
    }
}
