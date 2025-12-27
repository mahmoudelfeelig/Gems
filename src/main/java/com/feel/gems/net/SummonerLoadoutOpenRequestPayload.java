package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;




/**
 * C2S: request to open the Summoner loadout editor.
 */
public record SummonerLoadoutOpenRequestPayload() implements CustomPayload {
    public static final Id<SummonerLoadoutOpenRequestPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "summoner_loadout_open_request"));
    public static final SummonerLoadoutOpenRequestPayload INSTANCE = new SummonerLoadoutOpenRequestPayload();
    public static final PacketCodec<RegistryByteBuf, SummonerLoadoutOpenRequestPayload> CODEC = PacketCodec.unit(INSTANCE);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
