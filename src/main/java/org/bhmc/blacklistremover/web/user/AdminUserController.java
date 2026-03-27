package org.bhmc.blacklistremover.web.user;

import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.bhmc.blacklistremover.error.IllegalRequestDataException;
import org.bhmc.blacklistremover.model.User;
import org.bhmc.blacklistremover.to.AdminUserUpdateTo;
import org.bhmc.blacklistremover.web.AuthUser;

import java.net.URI;
import java.util.List;

import static org.bhmc.blacklistremover.validation.ValidationUtil.assureIdConsistent;
import static org.bhmc.blacklistremover.validation.ValidationUtil.checkNew;

@RestController
@RequestMapping(value = AdminUserController.REST_URL, produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminUserController extends AbstractUserController {

    static final String REST_URL = "/api/admin/users";

    @Override
    @GetMapping("/{id}")
    public User get(@PathVariable int id) {
        return super.get(id);
    }

    // Deleting users is disabled — users are linked to audit history and must only be disabled
//    @Override
//    @DeleteMapping("/{id}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public void delete(@PathVariable int id) {
//        super.delete(id);
//    }

    @GetMapping
    public List<User> getAll() {
        log.info("getAll");
        return repository.findAll(Sort.by(Sort.Direction.ASC, "name", "email"));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> createWithLocation(@Valid @RequestBody User user) {
        log.info("create {}", user);
        checkNew(user);
        User created = repository.prepareAndSave(user);
        URI uriOfNewResource = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(REST_URL + "/{id}")
                .buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(uriOfNewResource).body(created);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void update(@Valid @RequestBody AdminUserUpdateTo updateTo, @PathVariable int id) {
        log.info("update {} with id={}", updateTo, id);
        assureIdConsistent(updateTo, id);
        User user = repository.getExisted(id);
        user.setName(updateTo.getName());
        user.setEmail(updateTo.getEmail().toLowerCase());
        user.setRoles(updateTo.getRoles());
        user.setEnabled(updateTo.isEnabled());
        if (org.springframework.util.StringUtils.hasText(updateTo.getPassword())) {
            user.setPassword(updateTo.getPassword());
            repository.prepareAndSave(user);
        }
    }

    @GetMapping("/by-email")
    public User getByEmail(@RequestParam String email) {
        log.info("getByEmail {}", email);
        return repository.getExistedByEmail(email);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void enable(@PathVariable int id, @RequestParam boolean enabled,
                       @AuthenticationPrincipal AuthUser authUser) {
        if (authUser.id() == id) {
            throw new IllegalRequestDataException("You cannot change the enabled status of your own account");
        }
        log.info(enabled ? "enable {}" : "disable {}", id);
        User user = repository.getExisted(id);
        user.setEnabled(enabled);
    }
}