package org.bhmc.blacklistremover.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "wlc", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "ip_address"}, name = "uk_name_ip"))
public class Wlc extends NamedEntity {

    @Column(name = "description")
    private String description;

    @Column(name = "ip_address", nullable = false)
    @NotNull
    private String ipAddress;

}
