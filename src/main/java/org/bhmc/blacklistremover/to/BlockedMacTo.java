package org.bhmc.blacklistremover.to;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.bhmc.blacklistremover.mapper.Default;

@Value
@EqualsAndHashCode(callSuper = true)
public class BlockedMacTo extends BaseTo {

    @NotBlank
    @Size(max = 128)
    String clientMac;

    @NotBlank
    @Size(max = 128)
    String blockTime;

    @NotBlank
    @Size(max = 128)
    String remainingTime;

    @NotBlank
    @Size(max = 128)
    String reason;

    /*@Size(max = 128)
    String macAddressStatus;*/

    int wlcId;

    int userId;

    @Default
    public BlockedMacTo(Integer id, String clientMac, String blockTime, String remainingTime, String reason, int wlcId, int userId) {
        super(id);
        this.clientMac = clientMac;
        this.blockTime = blockTime;
        this.remainingTime = remainingTime;
        this.reason = reason;
        this.wlcId = wlcId;
        this.userId = userId;
    }

    public BlockedMacTo(String clientMac, String blockTime, String remainingTime, String reason, int wlcId, int userId) {
        this(null, clientMac, blockTime, remainingTime, reason, wlcId, userId);
    }

    @Override
    public String toString() {
        return "BlockedMacTo:" + id + ":" + clientMac + ":" + blockTime + ":" + remainingTime + ":" + reason + ":" + wlcId + ":" + userId;
    }
}
