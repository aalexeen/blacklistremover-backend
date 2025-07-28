package org.bhmc.blacklistremover.web.blockedmac;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.bhmc.blacklistremover.model.BlockedMac;
import org.bhmc.blacklistremover.repository.BlockedMacRepository;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class AbstractBlockMacController {
    protected final Logger log = getLogger(getClass());

    @Autowired
    protected BlockedMacRepository repository;

    public BlockedMac get(int id) {
        log.info("get blocked mac with id: {}", id);
        return repository.getExisted(id);
    }

    public void delete(int id) {
        log.info("delete blocked mac with id: {}", id);
        repository.deleteExisted(id);
    }
}
