package org.bhmc.blacklistremover.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.bhmc.blacklistremover.model.BlockedMac;
import org.bhmc.blacklistremover.model.Wlc;
import org.bhmc.blacklistremover.to.BlockedMacTo;

@Mapper(config = MapStructConfig.class)
public interface BlockedMapMapper extends BaseMapper<BlockedMac, BlockedMacTo> {


    @Mapping(target = "clientMac", expression = "java(to.getClientMac().toLowerCase())")
    @Mapping(target = "macAddressStatus", expression = "java(java.util.Collections.singleton(org.bhmc.blacklistremover.model.MacAddressStatus.BLOCKED))")
    @Mapping(target = "userId", expression = "java(0)")
    @Mapping(target = "blockTime", source = "to.blockTime")
    @Mapping(target = "remainingTime", source = "to.remainingTime")
    @Mapping(target = "reason", source = "to.reason")
    @Mapping(target = "wlc", source = "wlc")
    @Mapping(target = "id", ignore = true)
        //@Override
    BlockedMac toEntity(BlockedMacTo to, Wlc wlc);
}
