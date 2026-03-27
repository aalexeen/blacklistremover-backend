package org.bhmc.blacklistremover.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.bhmc.blacklistremover.model.DeletedMac;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeletedMacRepository extends BaseRepository<DeletedMac>, JpaSpecificationExecutor<DeletedMac> {

    List<DeletedMac> findByClientMac(String clientMac);

    Page<DeletedMac> findByClientMacContainingIgnoreCase(String clientMac, Pageable pageable);

    List<DeletedMac> findByDeletedByUserId(Integer userId);

    List<DeletedMac> findByDeletedTimeBetween(LocalDateTime start, LocalDateTime end);

    List<DeletedMac> findByWlcId(Integer wlcId);
}