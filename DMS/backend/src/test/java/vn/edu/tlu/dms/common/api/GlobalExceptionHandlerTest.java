package vn.edu.tlu.dms.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;

/** Mã lỗi sổ cọc nhất quán giữa kiểm tra nghiệp vụ và ràng buộc PostgreSQL. */
class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void duplicateDepositConstraintsHaveSpecificCodes() {
        assertCode("ux_deposit_once", "23505", ErrorCode.DUPLICATE_DEPOSIT, HttpStatus.CONFLICT);
        assertCode("ux_refund_once", "23505", ErrorCode.DUPLICATE_REFUND, HttpStatus.CONFLICT);
        assertCode(
                "ux_deduction_ticket", "23505", ErrorCode.DUPLICATE_DEDUCTION, HttpStatus.CONFLICT);
    }

    @Test
    void missingMaintenanceTicketIsValidationInsteadOfDuplicateDeduction() {
        assertCode(
                "deposit_transaction_maintenance_request_id_fkey",
                "23503",
                ErrorCode.VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST);
    }

    private void assertCode(String constraint, String sqlState, String code, HttpStatus status) {
        String error = "SERROR\0C" + sqlState + "\0n" + constraint + "\0Minvalid ledger\0\0";
        var cause = new PSQLException(new ServerErrorMessage(error));
        var request = new MockHttpServletRequest("POST", "/api/contracts/1/deposit-deduct");
        var response =
                handler.integrity(new DataIntegrityViolationException("ledger", cause), request);
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo(code);
    }
}
