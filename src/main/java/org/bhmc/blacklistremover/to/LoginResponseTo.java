package org.bhmc.blacklistremover.to;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.bhmc.blacklistremover.HasIdAndEmail;
import org.bhmc.blacklistremover.model.Role;

import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
public class LoginResponseTo extends NamedTo implements HasIdAndEmail {
    String email;
    Set<Role> roles;

    public LoginResponseTo(Integer id, String name, String email, Set<Role> roles) {
        super(id, name);
        this.email = email;
        this.roles = roles;
    }

    @Override
    public String toString() {
        return "LoginResponseTo:" + id + '[' + email + ']' + " roles:" + roles;
    }
}