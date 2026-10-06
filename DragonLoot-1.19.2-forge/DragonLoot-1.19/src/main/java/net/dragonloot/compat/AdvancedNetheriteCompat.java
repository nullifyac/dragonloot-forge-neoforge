package net.dragonloot.compat;

import java.util.List;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

public final class AdvancedNetheriteCompat {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    private AdvancedNetheriteCompat() {
    }

    public static boolean isWearingPacifyingArmor(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (isPacifyingArmor(stack)) {
                return true;
            }
        }
        return false;
    }

    public static void appendArmorPerkTooltips(ItemStack stack, List<Component> tooltip) {
        if (!ConfigInit.CONFIG.advanced_netherite_gear_perks_enabled || !isDragonArmor(stack.getItem())) {
            return;
        }

        if (shouldShowDetailedTooltips()) {
            tooltip.add(Component.translatable("tooltip.dragonloot.armor.enderman_passive").withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(Component.translatable("tooltip.dragonloot.armor.piglin_passive").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.dragonloot.armor.phantom_passive").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(pressShiftLine());
        }
    }

    private static boolean isPacifyingArmor(ItemStack stack) {
        return isDragonArmorAndConfigEnabled(stack.getItem());
    }

    private static boolean isDragonArmorAndConfigEnabled(Item item) {
        return ConfigInit.CONFIG.advanced_netherite_gear_perks_enabled && isDragonArmor(item);
    }

    private static boolean isDragonArmor(Item item) {
        return item == ItemInit.DRAGON_HELMET.get()
            || item == ItemInit.DRAGON_CHESTPLATE.get()
            || item == ItemInit.DRAGON_LEGGINGS.get()
            || item == ItemInit.DRAGON_BOOTS.get()
            || item == ItemInit.UPGRADED_DRAGON_CHESTPLATE.get();
    }

    private static boolean shouldShowDetailedTooltips() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return false;
        }
        return isShiftDown();
    }

    @OnlyIn(Dist.CLIENT)
    private static boolean isShiftDown() {
        return Screen.hasShiftDown();
    }

    private static Component pressShiftLine() {
        Component shiftKey = Component.translatable("key.keyboard.left.shift").withStyle(ChatFormatting.YELLOW);
        return Component.translatable("tooltip.dragonloot.armor.press_shift_key", shiftKey);
    }
}

