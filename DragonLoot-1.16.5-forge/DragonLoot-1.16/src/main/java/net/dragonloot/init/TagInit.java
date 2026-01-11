package net.dragonloot.init;

import net.minecraft.item.Item;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;

public class TagInit {
    public static final ITag.INamedTag<Item> NOT_DESTROYED_BY_EXPLOSION = ItemTags.bind("dragonloot:explosion_resistant");

    public static void init() {

    }

}
