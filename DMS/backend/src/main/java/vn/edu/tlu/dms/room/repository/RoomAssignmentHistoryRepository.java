package vn.edu.tlu.dms.room.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.room.domain.RoomAssignmentHistory;

public interface RoomAssignmentHistoryRepository
        extends JpaRepository<RoomAssignmentHistory, Long>,
                JpaSpecificationExecutor<RoomAssignmentHistory> {}
