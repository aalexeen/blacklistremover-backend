package org.bhmc.blacklistremover.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.security.core.GrantedAuthority;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public enum Role implements GrantedAuthority {
    USER,
    ADMIN;

    @Override
    public String getAuthority() {
        //   https://stackoverflow.com/a/19542316/548473
        return "ROLE_" + name();
    }

    @JsonValue
    public String toJson() {
        return name();
    }

    @JsonCreator
    public static Role fromJson(String value) {
        return Role.valueOf(value);
    }
}