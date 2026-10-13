package vn.edu.tlu.dms.billing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.billing.domain.InvoiceItem;

public interface InvoiceItemRepository
        extends JpaRepository<InvoiceItem, Long>, JpaSpecificationExecutor<InvoiceItem> {}
