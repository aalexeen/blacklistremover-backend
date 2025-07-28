package org.bhmc.blacklistremover.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.bhmc.blacklistremover.model.BlockedMac;
import org.bhmc.blacklistremover.model.DeletedMac;
import org.bhmc.blacklistremover.to.DeletedMacTo;

@Mapper(config = MapStructConfig.class)
public interface DeletedMacMapper extends BaseMapper<DeletedMac, DeletedMacTo> {

    @Override
    @Mapping(target = "id", ignore = true)
    DeletedMac toEntity(DeletedMacTo to);

    @Override
    DeletedMacTo toTo(DeletedMac entity);

    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "clientMac", source = "blockedMac.clientMac"),
        @Mapping(target = "deletedTime", expression = "java(java.time.LocalDateTime.now())"),
        @Mapping(target = "deletedByUserId", source = "deletedByUserId"),
        @Mapping(target = "wlcId", expression = "java(blockedMac.getWlc().getId())"),
        @Mapping(target = "reason", source = "blockedMac.reason"),
        @Mapping(target = "originalBlockTime", source = "blockedMac.blockTime")
    })
    DeletedMac fromBlockedMac(BlockedMac blockedMac, Integer deletedByUserId);

    @Mappings({
        @Mapping(target = "clientMac", source = "blockedMac.clientMac"),
        @Mapping(target = "deletedTime", expression = "java(java.time.LocalDateTime.now())"),
        @Mapping(target = "deletedByUserId", source = "deletedByUserId"),
        @Mapping(target = "wlcId", expression = "java(blockedMac.getWlc().getId())"),
        @Mapping(target = "reason", source = "blockedMac.reason"),
        @Mapping(target = "originalBlockTime", source = "blockedMac.blockTime")
    })
    DeletedMacTo toToFromBlockedMac(BlockedMac blockedMac, Integer deletedByUserId);
}