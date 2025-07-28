package org.bhmc.blacklistremover.to;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.bhmc.blacklistremover.mapper.Default;

import java.time.LocalDateTime;

@Value
@EqualsAndHashCode(callSuper = true)
public class DeletedMacTo extends BaseTo {

    @NotBlank
    @Size(max = 17)
    String clientMac;

    @NotNull
    LocalDateTime deletedTime;

    Integer deletedByUserId;

    int wlcId;

    @NotBlank
    @Size(max = 48)
    String reason;

    @NotBlank
    @Size(max = 48)
    String originalBlockTime;

    @Default
    public DeletedMacTo(Integer id, String clientMac, LocalDateTime deletedTime, Integer deletedByUserId,
                       int wlcId, String reason, String originalBlockTime) {
        super(id);
        this.clientMac = clientMac;
        this.deletedTime = deletedTime;
        this.deletedByUserId = deletedByUserId;
        this.wlcId = wlcId;
        this.reason = reason;
        this.originalBlockTime = originalBlockTime;
    }

    // Constructor to create from BlockedMacTo
    public DeletedMacTo(BlockedMacTo blockedMacTo, LocalDateTime deletedTime, Integer deletedByUserId) {
        this(null, blockedMacTo.getClientMac(), deletedTime, deletedByUserId,
             blockedMacTo.getWlcId(), blockedMacTo.getReason(), blockedMacTo.getBlockTime());
    }

    @Override
    public String toString() {
        return "DeletedMacTo:" + id + ":" + clientMac + ":" + deletedTime +
               ":" + deletedByUserId + ":" + wlcId + ":" + reason;
    }
}