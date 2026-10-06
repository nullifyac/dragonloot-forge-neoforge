package net.dragonloot.compat;

import java.util.List;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

public final class AdvancedNetheriteCompat {

    private static final EquipmentSlotType[] ARMOR_SLOTS = {
        EquipmentSlotType.HEAD,
        EquipmentSlotType.CHEST,
        EquipmentSlotType.LEGS,
        EquipmentSlotType.FEET
    };

    private AdvancedNetheriteCompat() {
    }

    public static boolean isWearingPacifyingArmor(PlayerEntity player) {
        for (EquipmentSlotType slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (isPacifyingArmor(stack)) {
                return true;
            }
        }
        return false;
    }

    public static void appendArmorPerkTooltips(ItemStack stack, List<ITextComponent> tooltip) {
        if (!ConfigInit.CONFIG.advanced_netherite_gear_perks_enabled || !isDragonArmor(stack.getItem())) {
            return;
        }

        if (shouldShowDetailedTooltips()) {
            tooltip.add(new TranslationTextComponent("tooltip.dragonloot.armor.enderman_passive").withStyle(TextFormatting.DARK_GREEN));
            tooltip.add(new TranslationTextComponent("tooltip.dragonloot.armor.piglin_passive").withStyle(TextFormatting.GOLD));
            tooltip.add(new TranslationTextComponent("tooltip.dragonloot.armor.phantom_passive").withStyle(TextFormatting.GRAY));
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

    private static ITextComponent pressShiftLine() {
        ITextComponent shiftKey = new TranslationTextComponent("key.keyboard.left.shift").withStyle(TextFormatting.YELLOW);
        return new TranslationTextComponent("tooltip.dragonloot.armor.press_shift_key", shiftKey);
    }
}
