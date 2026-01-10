package com.feel.gems.client.entity;

import com.feel.gems.client.ClientShadowCloneState;
import com.feel.gems.entity.HunterPackEntity;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;

/**
 * Renders HunterPackEntity (Six-Pack Pain clones) with a humanoid (biped) model.
 * Uses the owner's skin texture if available, otherwise falls back to Steve.
 * Reuses ClientShadowCloneState for owner UUID tracking.
 */
public class HunterPackEntityRenderer extends MobEntityRenderer<HunterPackEntity, BipedEntityRenderState, BipedEntityModel<BipedEntityRenderState>> {
    
    private static final Identifier STEVE_SKIN = Identifier.ofVanilla("textures/entity/player/wide/steve.png");
    
    // Store current entity being rendered for texture lookup
    private HunterPackEntity currentEntity;
    
    public HunterPackEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER)), 0.5F);
    }
    
    @Override
    public BipedEntityRenderState createRenderState() {
        return new BipedEntityRenderState();
    }
    
    @Override
    public void updateRenderState(HunterPackEntity entity, BipedEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        this.currentEntity = entity;
    }
    
    @Override
    public Identifier getTexture(BipedEntityRenderState state) {
        if (currentEntity == null) {
            return STEVE_SKIN;
        }
        
        // Try to get owner's skin from client state (reuses ShadowClone's state tracker)
        UUID ownerUuid = ClientShadowCloneState.getOwner(currentEntity.getId());
        if (ownerUuid == null) {
            return STEVE_SKIN;
        }
        
        // Look up skin from player list
        Identifier skinId = getSkinIdForUuid(ownerUuid);
        if (skinId != null) {
            return skinId;
        }
        
        return STEVE_SKIN;
    }
    
    /**
     * Get skin texture Identifier for a player UUID from the player list.
     */
    private Identifier getSkinIdForUuid(UUID uuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) {
            return null;
        }
        
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(uuid);
        if (entry == null) {
            return null;
        }
        
        SkinTextures skinTextures = entry.getSkinTextures();
        if (skinTextures == null) {
            return null;
        }
        
        // SkinTextures record has body() which returns AssetInfo.TextureAsset
        // TextureAsset has texturePath() which returns Identifier
        return skinTextures.body().texturePath();
    }
}
