package net.dragonloot.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class DragonLootConfig {

        public int scale_minimum_drop_amount = 3;
        public int additional_scales_per_player = 2;
        public float additional_scale_drop_chance = 0.8F;
        public int dragon_armor_protection_helmet = 7;
        public int dragon_armor_protection_chest = 10;
        public int dragon_armor_protection_leggings = 9;
        public int dragon_armor_protection_boots = 7;
        public int dragon_armor_protection_horse = 18;
        public float dragon_armor_toughness = 3.0F;
        public float dragon_armor_knockback_resistance = 1.0F;
        public int dragon_armor_enchantability = 15;
        public int dragon_armor_durability_multiplier = 37;
        public int dragon_item_durability_multiplier = 37;
        public float dragon_item_base_damage = 5.0F;
        public float dragon_tool_mining_speed = 12.0F;
        public int dragon_tool_enchantability = 20;
        public boolean dragon_anvil_no_cap = true;
        public boolean advanced_netherite_gear_perks_enabled = false;

        private final ModConfigSpec.IntValue scaleMinimumDropAmountValue;
        private final ModConfigSpec.IntValue additionalScalesPerPlayerValue;
        private final ModConfigSpec.DoubleValue additionalScaleDropChanceValue;
        private final ModConfigSpec.IntValue helmetProtectionValue;
        private final ModConfigSpec.IntValue chestplateProtectionValue;
        private final ModConfigSpec.IntValue leggingsProtectionValue;
        private final ModConfigSpec.IntValue bootsProtectionValue;
        private final ModConfigSpec.IntValue horseProtectionValue;
        private final ModConfigSpec.DoubleValue armorToughnessValue;
        private final ModConfigSpec.DoubleValue armorKnockbackValue;
        private final ModConfigSpec.IntValue armorEnchantabilityValue;
        private final ModConfigSpec.IntValue armorDurabilityMultiplierValue;
        private final ModConfigSpec.IntValue toolDurabilityMultiplierValue;
        private final ModConfigSpec.DoubleValue toolBaseDamageValue;
        private final ModConfigSpec.DoubleValue toolMiningSpeedValue;
        private final ModConfigSpec.IntValue toolEnchantabilityValue;
        private final ModConfigSpec.BooleanValue dragonAnvilNoCapValue;
        private final ModConfigSpec.BooleanValue advancedNetheriteGearPerksEnabledValue;

        public DragonLootConfig(ModConfigSpec.Builder builder) {
                builder.comment("Dragon Loot balance settings. Gear stats and compatibility perks require a full game/server restart.",
                        "This file is not automatically synchronized: use matching settings on the server and all clients.")
                        .push("dragonloot");
                scaleMinimumDropAmountValue = builder.comment("Minimum guaranteed dragon scale drops from the Ender Dragon.")
                        .defineInRange("scale_minimum_drop_amount", 3, 0, 64);
                additionalScalesPerPlayerValue = builder.comment("Additional drop rolls per nearby player.")
                        .defineInRange("additional_scales_per_player", 2, 0, 32);
                additionalScaleDropChanceValue = builder.comment("Chance per additional roll to drop a scale (0.0 - 1.0).")
                        .defineInRange("additional_scale_drop_chance", 0.8D, 0.0D, 1.0D);

                builder.comment("Protection values contribute to the total armor attribute, capped at 30 in vanilla Minecraft.").push("armor");
                helmetProtectionValue = builder.gameRestart().defineInRange("dragon_armor_protection_helmet", 7, 0, 30);
                chestplateProtectionValue = builder.gameRestart().defineInRange("dragon_armor_protection_chest", 10, 0, 30);
                leggingsProtectionValue = builder.gameRestart().defineInRange("dragon_armor_protection_leggings", 9, 0, 30);
                bootsProtectionValue = builder.gameRestart().defineInRange("dragon_armor_protection_boots", 7, 0, 30);
                horseProtectionValue = builder.comment("Protection supplied by Dragon horse armor.")
                        .gameRestart().defineInRange("dragon_armor_protection_horse", 18, 0, 30);
                armorToughnessValue = builder.comment("Toughness per armor piece. Vanilla Minecraft caps the total toughness attribute at 20.")
                        .gameRestart().defineInRange("dragon_armor_toughness", 3.0D, 0.0D, 20.0D);
                armorKnockbackValue = builder.gameRestart().defineInRange("dragon_armor_knockback_resistance", 1.0D, 0.0D, 5.0D);
                armorEnchantabilityValue = builder.gameRestart().defineInRange("dragon_armor_enchantability", 15, 1, 255);
                armorDurabilityMultiplierValue = builder.comment("Multiplied by each armor slot's base durability. Upper bound prevents integer overflow.")
                        .gameRestart().defineInRange("dragon_armor_durability_multiplier", 37, 1, Integer.MAX_VALUE / 16);
                builder.pop();

                builder.push("tools");
                toolDurabilityMultiplierValue = builder.comment("Multiplied by 67 for Dragon tools and weapons. Upper bound prevents integer overflow.")
                        .gameRestart().defineInRange("dragon_item_durability_multiplier", 37, 1, Integer.MAX_VALUE / 67);
                toolBaseDamageValue = builder.comment("Material attack damage bonus; each weapon adds its own modifier. Vanilla Minecraft caps total attack damage at 2048.")
                        .gameRestart().defineInRange("dragon_item_base_damage", 5.0D, 0.0D, 2048.0D);
                toolMiningSpeedValue = builder.comment("Mining speed for Dragon tools on their effective blocks. Set this explicitly to suit your pack's progression.",
                        "Advanced Netherite's mining-speed config is not automatically inherited.")
                        .gameRestart().defineInRange("dragon_tool_mining_speed", 12.0D, 0.0D, Float.MAX_VALUE);
                toolEnchantabilityValue = builder.gameRestart().defineInRange("dragon_tool_enchantability", 20, 1, 255);
                builder.pop();

                builder.push("compat");
                advancedNetheriteGearPerksEnabledValue = builder.comment("Enables Dragon armor mob pacification and armor perk tooltips.",
                        "Preserves the existing fixed minimum durability multiplier of 47 and +3 sword/axe attack damage.",
                        "Does not inherit Advanced Netherite config values or implement its additional tool drops. Configure Dragon stats separately.")
                        .gameRestart().define("advanced_netherite_gear_perks_enabled", false);
                builder.pop();

                dragonAnvilNoCapValue = builder.comment("If true, Dragon Anvils ignore the vanilla 40 level cap.")
                        .define("dragon_anvil_no_cap", true);

                builder.pop();
        }

        public void bake() {
                scale_minimum_drop_amount = scaleMinimumDropAmountValue.get();
                additional_scales_per_player = additionalScalesPerPlayerValue.get();
                additional_scale_drop_chance = additionalScaleDropChanceValue.get().floatValue();
                dragon_armor_protection_helmet = helmetProtectionValue.get();
                dragon_armor_protection_chest = chestplateProtectionValue.get();
                dragon_armor_protection_leggings = leggingsProtectionValue.get();
                dragon_armor_protection_boots = bootsProtectionValue.get();
                dragon_armor_protection_horse = horseProtectionValue.get();
                dragon_armor_toughness = armorToughnessValue.get().floatValue();
                dragon_armor_knockback_resistance = armorKnockbackValue.get().floatValue();
                dragon_armor_enchantability = armorEnchantabilityValue.get();
                dragon_armor_durability_multiplier = armorDurabilityMultiplierValue.get();
                dragon_item_durability_multiplier = toolDurabilityMultiplierValue.get();
                dragon_item_base_damage = toolBaseDamageValue.get().floatValue();
                dragon_tool_mining_speed = toolMiningSpeedValue.get().floatValue();
                dragon_tool_enchantability = toolEnchantabilityValue.get();
                dragon_anvil_no_cap = dragonAnvilNoCapValue.get();
                advanced_netherite_gear_perks_enabled = advancedNetheriteGearPerksEnabledValue.get();
        }
}
