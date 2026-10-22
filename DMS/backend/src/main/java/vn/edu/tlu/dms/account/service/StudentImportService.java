package vn.edu.tlu.dms.account.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Nhập hồ sơ sinh viên bằng Excel, chỉ Admin; mật khẩu chỉ trả một lần khi ghi thành công. */
public interface StudentImportService {

    /**
     * Kiểm tra toàn bộ file; dryRun không ghi, lần ghi tạo các dòng hợp lệ trong một transaction.
     */
    Result importStudents(MultipartFile file, boolean dryRun);

    /** Tạo file mẫu với tiêu đề chuẩn, không chứa tài khoản hoặc dữ liệu cá nhân. */
    byte[] downloadTemplate();

    record CellError(String column, String code) {}

    record RowResult(int rowNumber, String studentCode, String result, List<CellError> errors) {}

    record CreatedAccount(Long id, String username, String temporaryPassword) {
        @Override
        public String toString() {
            return "CreatedAccount[id="
                    + id
                    + ", username="
                    + username
                    + ", temporaryPassword=REDACTED]";
        }
    }

    record Result(
            boolean dryRun,
            int totalRows,
            int validRows,
            int createdRows,
            int existingRows,
            int errorRows,
            List<RowResult> rows,
            List<CreatedAccount> createdAccounts) {}
}
