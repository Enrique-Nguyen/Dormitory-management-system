package vn.edu.tlu.dms.notification.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.NotificationSource;
import vn.edu.tlu.dms.contract.domain.ContractMovedOutEvent;
import vn.edu.tlu.dms.contract.domain.StorageRecordChangedEvent;
import vn.edu.tlu.dms.notification.service.NotificationService;

import java.util.List;

/**
 * Gửi thông báo hộp thư cho sinh viên sau khi contract đã commit việc trả phòng hoặc gửi/nhận đồ.
 * Listener chạy sau commit, chỉ gọi Service công khai của notification và bắt lỗi để nghiệp vụ đã
 * lưu không bị ảnh hưởng.
 */
@Component
public class ContractNotificationListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ContractNotificationListener.class);

    private final NotificationService service;

    public ContractNotificationListener(NotificationService service) {
        this.service = service;
    }

    /**
     * Thông báo cho sinh viên khi hợp đồng đã trả phòng, kèm liên kết tới chi tiết hợp đồng để xem
     * trạng thái kết thúc và công nợ còn lại.
     *
     * @param event hợp đồng và sinh viên vừa trả phòng sau khi nghiệp vụ đã commit
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContractMovedOut(ContractMovedOutEvent event) {
        if (event == null || event.contractId() == null || event.studentId() == null) {
            return;
        }
        try {
            service.notifyUsers(
                    List.of(event.studentId()),
                    "Hợp đồng #" + event.contractId() + " đã trả phòng",
                    "Hợp đồng #"
                            + event.contractId()
                            + " đã được ghi nhận trả phòng. Vui lòng mở mục Hợp đồng để xem chi"
                            + " tiết.",
                    NotificationSource.CONTRACT_MOVED_OUT,
                    event.contractId());
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Không tạo được thông báo trả phòng hợp đồng {}",
                    event.contractId(),
                    exception);
        }
    }

    /**
     * Thông báo cho sinh viên khi Staff lập phiếu gửi đồ hoặc ghi nhận đã nhận lại đồ. Mã nguồn
     * dùng chung mã hợp đồng để liên kết mở đúng màn hợp đồng; tiêu đề nêu mã phiếu gửi đồ nên mỗi
     * phiếu của cùng hợp đồng vẫn có thông báo riêng và không bị chống trùng nhầm.
     *
     * @param event phiếu gửi đồ và hành động vừa thực hiện sau khi nghiệp vụ đã commit
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStorageRecordChanged(StorageRecordChangedEvent event) {
        if (event == null || event.storageRecordId() == null || event.studentId() == null) {
            return;
        }
        try {
            boolean returned = BusinessAction.RETURN_STORAGE.equals(event.action());
            String title =
                    returned
                            ? "Đã nhận lại đồ gửi #" + event.storageRecordId()
                            : "Đã tạo phiếu gửi đồ #" + event.storageRecordId();
            String body =
                    returned
                            ? "Phiếu gửi đồ #"
                                    + event.storageRecordId()
                                    + " đã được ghi nhận nhận lại đồ. Vui lòng mở mục Hợp đồng"
                                    + " để xem chi tiết."
                            : "Phiếu gửi đồ #"
                                    + event.storageRecordId()
                                    + " đã được tạo cho hợp đồng #"
                                    + event.contractId()
                                    + ". Vui lòng mở mục Hợp đồng để xem chi tiết.";
            service.notifyUsers(
                    List.of(event.studentId()),
                    title,
                    body,
                    NotificationSource.STORAGE_RECORD_CHANGED,
                    event.storageRecordId());
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Không tạo được thông báo phiếu gửi đồ {} của hợp đồng {}",
                    event.storageRecordId(),
                    event.contractId(),
                    exception);
        }
    }
}
