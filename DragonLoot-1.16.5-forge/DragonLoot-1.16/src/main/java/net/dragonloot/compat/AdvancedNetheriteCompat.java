package net.dragonloot.compat;

import java.util.List;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
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

        boolean hasEnderman = true;
        boolean hasPiglin = true;
        boolean hasPhantom = true;

        if (!(hasEnderman || hasPiglin || hasPhantom)) {
            return;
        }

        if (shouldShowDetailedTooltips()) {
            if (hasEnderman) {
                tooltip.add(pacifyEndermenLine());
            }
            if (hasPiglin) {
                tooltip.add(pacifyPiglinsLine());
            }
            if (hasPhantom) {
                tooltip.add(pacifyPhantomsLine());
            }
        } else {
            tooltip.add(pressShiftLine());
        }
    }

    public static void appendPickaxePerkTooltips(ItemStack stack, List<ITextComponent> tooltip) {
        if (!ConfigInit.CONFIG.advanced_netherite_gear_perks_enabled || !isDragonPickaxe(stack.getItem())) {
            return;
        }

        if (shouldShowDetailedTooltips()) {
            tooltip.add(pickaxeIronLine());
            tooltip.add(pickaxeGoldLine());
            tooltip.add(pickaxeEmeraldLine());
            tooltip.add(pickaxeDiamondLine());
        } else {
            tooltip.add(pressShiftLine());
        }
    }

    public static void appendHoePerkTooltips(ItemStack stack, List<ITextComponent> tooltip) {
        if (!ConfigInit.CONFIG.advanced_netherite_gear_perks_enabled || !isDragonHoe(stack.getItem())) {
            return;
        }

        if (shouldShowDetailedTooltips()) {
            tooltip.add(hoeAdditionalCropsLine());
        } else {
            tooltip.add(pressShiftLine());
        }
    }

    public static void appendSwordPerkTooltips(ItemStack stack, List<ITextComponent> tooltip) {
        if (!ConfigInit.CONFIG.advanced_netherite_gear_perks_enabled || !isDragonSword(stack.getItem())) {
            return;
        }

        if (shouldShowDetailedTooltips()) {
            tooltip.add(swordEndermanLine());
            tooltip.add(swordPiglinLine());
            tooltip.add(swordZombifiedPiglinLine());
            tooltip.add(swordPhantomLine());
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

    private static boolean isDragonPickaxe(Item item) {
        return item == ItemInit.DRAGON_PICKAXE_ITEM.get();
    }

    private static boolean isDragonHoe(Item item) {
        return item == ItemInit.DRAGON_HOE_ITEM.get();
    }

    private static boolean isDragonSword(Item item) {
        return item == ItemInit.DRAGON_SWORD_ITEM.get();
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

    private static ITextComponent plusComponent() {
        return new TranslationTextComponent("tooltip.advancednetherite.misc.plus").withStyle(TextFormatting.GREEN, TextFormatting.BOLD);
    }

    private static ITextComponent pressShiftLine() {
        ITextComponent shiftKey = new TranslationTextComponent("tooltip.advancednetherite.misc.shift").withStyle(TextFormatting.YELLOW);
        return new TranslationTextComponent("tooltip.advancednetherite.misc.press_shift_key", shiftKey);
    }

    private static ITextComponent pacifyEndermenLine() {
        return new TranslationTextComponent("tooltip.advancednetherite.armor.enderman_passive", plusComponent()).withStyle(TextFormatting.DARK_GREEN);
    }

    private static ITextComponent pacifyPiglinsLine() {
        return new TranslationTextComponent("tooltip.advancednetherite.armor.piglin_passive", plusComponent()).withStyle(TextFormatting.GOLD);
    }

    private static ITextComponent pacifyPhantomsLine() {
        return new TranslationTextComponent("tooltip.advancednetherite.armor.phantom_passive", plusComponent()).withStyle(TextFormatting.GRAY);
    }

    private static ITextComponent hoeAdditionalCropsLine() {
        return new TranslationTextComponent("tooltip.advancednetherite.hoe.additional_crop_drops", plusComponent());
    }

    private static ITextComponent pickaxeIronLine() {
        return new TranslationTextComponent("tooltip.advancednetherite.pickaxe.additional_iron_drop", plusComponent()).withStyle(TextFormatting.GRAY);
    }

    private static ITextComponent pickaxeGoldLine() {
        return new TranslationTextComponent("tooltip.advancednetherite.pickaxe.additional_gold_drop", plusComponent()).withStyle(TextFormatting.GOLD);
    }

    private static ITextComponent pickaxeEmeraldLine() {
        return new TranslationTextComponent("tooltip.advancednetherite.pickaxe.additional_emerald_drop", plusComponent()).withStyle(TextFormatting.DARK_GREEN);
    }

    private static ITextComponent pickaxeDiamondLine() {
        return new TranslationTextComponent("tooltip.advancednetherite.pickaxe.additional_diamond_drop", plusComponent()).withStyle(TextFormatting.AQUA);
    }

    private static ITextComponent swordEndermanLine() {
        return new TranslationTextComponent("tooltip.advancednetherite.sword.additional_enderman_mob_drop", plusComponent()).withStyle(TextFormatting.DARK_GREEN);
    }

    private static ITextComponent swordPiglinLine() {
        return new TranslationTextComponent("tooltip.advancednetherite.sword.additional_piglin_mob_drop", plusComponent()).withStyle(TextFormatting.GOLD);
    }

    private static ITextComponent swordZombifiedPiglinLine() {
        return new TranslationTextComponent("tooltip.advancednetherite.sword.additional_zombified_piglin_mob_drop", plusComponent()).withStyle(TextFormatting.GOLD);
    }

    private static ITextComponent swordPhantomLine() {
        return new TranslationTextComponent("tooltip.advancednetherite.sword.additional_phantom_mob_drop", plusComponent()).withStyle(TextFormatting.GRAY);
    }
}
