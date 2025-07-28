package org.bhmc.blacklistremover.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bhmc.blacklistremover.mapper.Default;

import java.time.LocalDateTime;

@Entity
@Table(name = "deleted_mac", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"client_mac", "deleted_time"}, name = "uk_mac_deletedtime")
})
@Getter
@Setter
@NoArgsConstructor
public class DeletedMac extends BaseEntity {

    @Column(name = "client_mac", nullable = false)
    @NotNull
    @Size(max = 17)
    private String clientMac;

    @Column(name = "deleted_time", nullable = false)
    @NotNull
    private LocalDateTime deletedTime;

    @Column(name = "deleted_by_user_id")
    private Integer deletedByUserId;

    @Column(name = "wlc_id", nullable = false)
    @NotNull
    private Integer wlcId;

    @Column(name = "reason", nullable = false)
    @NotNull
    @Size(max = 48)
    private String reason;

    @Column(name = "original_block_time", nullable = false)
    @NotNull
    @Size(max = 48)
    private String originalBlockTime;

    @Default
    public DeletedMac(Integer id, String clientMac, LocalDateTime deletedTime, Integer deletedByUserId,
                      Integer wlcId, String reason, String originalBlockTime) {
        super(id);
        this.clientMac = clientMac;
        this.deletedTime = deletedTime;
        this.deletedByUserId = deletedByUserId;
        this.wlcId = wlcId;
        this.reason = reason;
        this.originalBlockTime = originalBlockTime;
    }
}