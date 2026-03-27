package org.bhmc.blacklistremover.to;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bhmc.blacklistremover.HasIdAndEmail;
import org.bhmc.blacklistremover.model.Role;
import org.bhmc.blacklistremover.validation.NoHtml;

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminUserUpdateTo extends NamedTo implements HasIdAndEmail {

    @Email
    @NotBlank
    @Size(max = 128)
    @NoHtml
    private String email;

    @Size(min = 5, max = 32)
    private String password;

    @NotEmpty
    private Set<Role> roles;

    private boolean enabled;

    public AdminUserUpdateTo(Integer id, String name, String email, String password, Set<Role> roles, boolean enabled) {
        super(id, name);
        this.email = email;
        this.password = password;
        this.roles = roles;
        this.enabled = enabled;
    }
}
