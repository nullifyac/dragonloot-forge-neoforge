package net.dragonloot.init;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class TagInit {
    public static final TagKey<Item> NOT_DESTROYED_BY_EXPLOSION = TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation("dragonloot", "explosion_resistant"));

    public static void init() {

    }

}
