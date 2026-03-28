package org.bhmc.blacklistremover.web.deletedmac;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.bhmc.blacklistremover.model.DeletedMac;
import org.bhmc.blacklistremover.model.User;
import org.bhmc.blacklistremover.repository.DeletedMacRepository;
import org.bhmc.blacklistremover.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = AdminDeletedMacController.REST_URL, produces = MediaType.APPLICATION_JSON_VALUE)
@AllArgsConstructor
@Slf4j
public class AdminDeletedMacController {

    static final String REST_URL = "/api/admin/deleted_macs";

    private final DeletedMacRepository deletedMacRepository;
    private final UserRepository userRepository;

    @GetMapping
    public Page<DeletedMacResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String mac,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        log.info("getAll deleted_macs page={} size={} mac={} userId={} reason={} from={} to={}",
                page, size, mac, userId, reason, dateFrom, dateTo);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "deletedTime"));
        Specification<DeletedMac> spec = buildSpec(mac, userId, reason, dateFrom, dateTo);
        return toResponsePage(deletedMacRepository.findAll(spec, pageable));
    }

    private Specification<DeletedMac> buildSpec(String mac, Integer userId, String reason,
                                                 LocalDate dateFrom, LocalDate dateTo) {
        Specification<DeletedMac> spec = Specification.where(null);

        if (mac != null && !mac.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("clientMac")), "%" + mac.trim().toLowerCase() + "%"));
        }
        if (userId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("deletedByUserId"), userId));
        }
        if (reason != null && !reason.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("reason")), "%" + reason.trim().toLowerCase() + "%"));
        }
        if (dateFrom != null) {
            LocalDateTime from = dateFrom.atStartOfDay();
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("deletedTime"), from));
        }
        if (dateTo != null) {
            LocalDateTime to = dateTo.atTime(LocalTime.MAX);
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("deletedTime"), to));
        }
        return spec;
    }

    private Page<DeletedMacResponse> toResponsePage(Page<DeletedMac> recordsPage) {
        Map<Integer, String> userNames = recordsPage.getContent().stream()
                .map(DeletedMac::getDeletedByUserId)
                .filter(id -> id != null && id != 0)
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> userRepository.findById(id)
                                .map(User::getName)
                                .orElse("Unknown")
                ));
        return recordsPage.map(dm -> new DeletedMacResponse(dm, userNames.get(dm.getDeletedByUserId())));
    }

    public record DeletedMacResponse(
            Integer id,
            String clientMac,
            LocalDateTime deletedTime,
            Integer deletedByUserId,
            String deletedByUserName,
            Integer wlcId,
            String reason,
            String originalBlockTime
    ) {
        public DeletedMacResponse(DeletedMac dm, String userName) {
            this(
                    dm.getId(),
                    dm.getClientMac(),
                    dm.getDeletedTime(),
                    dm.getDeletedByUserId(),
                    (dm.getDeletedByUserId() == null || dm.getDeletedByUserId() == 0) ? "System" : (userName != null ? userName : "Unknown"),
                    dm.getWlcId(),
                    dm.getReason(),
                    dm.getOriginalBlockTime()
            );
        }
    }
}
