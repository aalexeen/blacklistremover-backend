package org.bhmc.blacklistremover.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.bhmc.blacklistremover.mapper.Default;

import java.util.EnumSet;
import java.util.Set;


@Entity
@Getter
@Table(name = "blocked_mac", uniqueConstraints = {@UniqueConstraint(columnNames = {"client_mac", "wlc_id"}, name = "uk_mac_wlc")})
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BlockedMac extends BaseEntity {

    @Column(name = "client_mac", nullable = false)
    @NotNull
    @Size(max = 17)
    private String clientMac;

    @Column(name = "block_time", nullable = false)
    @NotNull
    @Size(max = 48)
    private String blockTime;

    @Column(name = "remaining_time", nullable = false)
    @NotNull
    @Size(max = 48)
    private String remainingTime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wlc_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @NotNull
    //@JsonIgnore
    private Wlc wlc;

    @Column(name = "reason", nullable = false)
    @NotNull
    @Size(max = 48)
    private String reason;

    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "mac_address_status",
            joinColumns = @JoinColumn(name = "blockedmac_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"blockedmac_id", "status"}, name = "uk_blockedmac_status"))
    @Column(name = "status")
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<MacAddressStatus> macAddressStatus = EnumSet.noneOf(MacAddressStatus.class);

    @Column(name = "user_id", nullable = true)
    private int userId;

    @Default
    public BlockedMac(String clientMac, String blockTime, String remainingTime, Wlc wlc, String reason) {
        this.clientMac = clientMac;
        this.blockTime = blockTime;
        this.remainingTime = remainingTime;
        this.wlc = wlc;
        this.reason = reason;
    }
}
