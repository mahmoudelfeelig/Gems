package com.feel.gems.entity;

import com.feel.gems.GemsMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * Registry for custom Gems mod entities.
 */
public final class ModEntities {
    
    private static final Identifier SHADOW_CLONE_ID = Identifier.of(GemsMod.MOD_ID, "shadow_clone");
    private static final Identifier HUNTER_PACK_ID = Identifier.of(GemsMod.MOD_ID, "hunter_pack");
    private static final Identifier TEST_DUMMY_ID = Identifier.of(GemsMod.MOD_ID, "test_dummy");
    
    public static final EntityType<ShadowCloneEntity> SHADOW_CLONE = Registry.register(
            Registries.ENTITY_TYPE,
            SHADOW_CLONE_ID,
            EntityType.Builder.create(ShadowCloneEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6F, 1.8F) // Same as player
                    .eyeHeight(1.62F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, SHADOW_CLONE_ID))
    );
    
    public static final EntityType<HunterPackEntity> HUNTER_PACK = Registry.register(
            Registries.ENTITY_TYPE,
            HUNTER_PACK_ID,
            EntityType.Builder.create(HunterPackEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6F, 1.8F) // Same as player
                    .eyeHeight(1.62F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, HUNTER_PACK_ID))
    );

    public static final EntityType<TestDummyEntity> TEST_DUMMY = Registry.register(
            Registries.ENTITY_TYPE,
            TEST_DUMMY_ID,
            EntityType.Builder.create(TestDummyEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6F, 1.8F)
                    .eyeHeight(1.62F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, TEST_DUMMY_ID))
    );
    
    private ModEntities() {}
    
    public static void init() {
        // Register entity attributes
        FabricDefaultAttributeRegistry.register(SHADOW_CLONE, ShadowCloneEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(HUNTER_PACK, HunterPackEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(TEST_DUMMY, TestDummyEntity.createAttributes());
        GemsMod.LOGGER.info("Registered Gems entities");
    }
}
