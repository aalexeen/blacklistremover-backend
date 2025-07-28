package org.bhmc.blacklistremover.web.blockedmac;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.bhmc.blacklistremover.mapper.BlockedMapMapper;
import org.bhmc.blacklistremover.model.BlockedMac;
import org.bhmc.blacklistremover.service.BlockedMacService;
import org.bhmc.blacklistremover.to.BlockedMacTo;

import java.net.URI;
import java.util.List;

import static org.bhmc.blacklistremover.validation.ValidationUtil.checkNew;

@RestController
@RequestMapping(value = BlockedMacController.REST_URL, produces = MediaType.APPLICATION_JSON_VALUE)
@AllArgsConstructor
@Slf4j
public class BlockedMacController extends AbstractBlockMacController {
    static final String REST_URL = "/api/blocked_macs";

    private final BlockedMacService service;
    private final BlockedMapMapper mapper;

    @Override
    @GetMapping("/{id}")
    public BlockedMac get(@PathVariable int id) {
        return super.get(id);
    }

    @GetMapping
    public List<BlockedMac> getAll() {
        return service.findAll();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void delete(@PathVariable int id) {
        log.info("delete blocked mac with id: {}", id);

        // Call the service's custom delete method only once
        boolean deleted = service.deleteBlockedMac(id);

        if (deleted) {
            log.info("Successfully deleted blocked mac with id={}", id);
        } else {
            log.warn("Failed to delete blocked mac with id={}", id);
            // You might want to throw an exception here for failed deletions
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<BlockedMac> create(@Valid @RequestBody BlockedMacTo blockedMacTo) {
        log.info("create {}", blockedMacTo);
        log.info("blackedMacTo id {}", blockedMacTo.getId());
        checkNew(blockedMacTo);
        BlockedMac blockedMac = mapper.toEntity(blockedMacTo, service.getWlcById(blockedMacTo.getWlcId()));
        log.info("created id {}", blockedMac.getId());
        BlockedMac created = repository.save(blockedMac);

        URI uriOfNewResource = ServletUriComponentsBuilder.fromCurrentRequest()
                                                          .path(REST_URL).build().toUri();
        return ResponseEntity.created(uriOfNewResource).body(created);
    }
}
