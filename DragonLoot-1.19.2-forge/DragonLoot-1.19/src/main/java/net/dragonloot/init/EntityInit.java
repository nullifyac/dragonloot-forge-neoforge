package net.dragonloot.init;

import net.dragonloot.DragonLootMain;
import net.dragonloot.entity.DragonTridentEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EntityInit {

    private EntityInit() {
    }

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DragonLootMain.MOD_ID);

    public static final RegistryObject<EntityType<DragonTridentEntity>> DRAGONTRIDENT_ENTITY = ENTITY_TYPES.register("dragon_trident", () -> EntityType.Builder.<DragonTridentEntity>of(DragonTridentEntity::new, MobCategory.MISC)
        .sized(0.5F, 0.5F)
        .clientTrackingRange(4)
        .updateInterval(20)
        .build(DragonLootMain.MOD_ID + ":dragon_trident"));
}
