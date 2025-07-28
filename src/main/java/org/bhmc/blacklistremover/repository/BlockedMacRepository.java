package org.bhmc.blacklistremover.repository;

import org.springframework.transaction.annotation.Transactional;
import org.bhmc.blacklistremover.model.BlockedMac;

@Transactional(readOnly = true)
public interface BlockedMacRepository extends BaseRepository<BlockedMac> {

}
