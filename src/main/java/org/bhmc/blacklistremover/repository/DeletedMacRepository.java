package org.bhmc.blacklistremover.repository;

import org.springframework.stereotype.Repository;
import org.bhmc.blacklistremover.model.DeletedMac;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeletedMacRepository extends BaseRepository<DeletedMac> {

    List<DeletedMac> findByClientMac(String clientMac);

    List<DeletedMac> findByDeletedByUserId(Integer userId);

    List<DeletedMac> findByDeletedTimeBetween(LocalDateTime start, LocalDateTime end);

    List<DeletedMac> findByWlcId(Integer wlcId);
}