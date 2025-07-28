package org.bhmc.blacklistremover.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.bhmc.blacklistremover.model.Wlc;
import org.bhmc.blacklistremover.repository.WlcRepository;

@Service
@RequiredArgsConstructor
public class WlcService {

    private final WlcRepository repository;

    public Wlc getWlcById(int id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Wlc not found with id: " + id));
    }
}
