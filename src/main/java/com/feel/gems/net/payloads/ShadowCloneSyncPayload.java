package com.feel.gems.net.payloads;

import com.feel.gems.GemsMod;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

/**
 * S2C payload to sync shadow clone owner information for skin rendering.
 */
public record ShadowCloneSyncPayload(int entityId, UUID ownerUuid, String ownerName) implements CustomPayload {
    
    public static final Id<ShadowCloneSyncPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "shadow_clone_sync"));
    
    public static final PacketCodec<RegistryByteBuf, ShadowCloneSyncPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, ShadowCloneSyncPayload::entityId,
            Uuids.PACKET_CODEC, ShadowCloneSyncPayload::ownerUuid,
            PacketCodecs.STRING, ShadowCloneSyncPayload::ownerName,
            ShadowCloneSyncPayload::new
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
