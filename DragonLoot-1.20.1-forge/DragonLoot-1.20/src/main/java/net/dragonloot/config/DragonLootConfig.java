package net.dragonloot.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class DragonLootConfig {

        public int scale_minimum_drop_amount = 3;
        public int additional_scales_per_player = 2;
        public float additional_scale_drop_chance = 0.8F;
        public int dragon_armor_protection_helmet = 7;
        public int dragon_armor_protection_chest = 10;
        public int dragon_armor_protection_leggings = 9;
        public int dragon_armor_protection_boots = 7;
        public float dragon_armor_toughness = 3.0F;
        public float dragon_armor_knockback_resistance = 1.0F;
        public int dragon_armor_enchantability = 15;
        public int dragon_armor_durability_multiplier = 37;
        public int dragon_item_durability_multiplier = 37;
        public float dragon_item_base_damage = 5.0F;
        public int dragon_tool_enchantability = 20;
        public boolean dragon_anvil_no_cap = true;
        public boolean advanced_netherite_gear_perks_enabled = false;

        private final ForgeConfigSpec.IntValue scaleMinimumDropAmountValue;
        private final ForgeConfigSpec.IntValue additionalScalesPerPlayerValue;
        private final ForgeConfigSpec.DoubleValue additionalScaleDropChanceValue;
        private final ForgeConfigSpec.IntValue helmetProtectionValue;
        private final ForgeConfigSpec.IntValue chestplateProtectionValue;
        private final ForgeConfigSpec.IntValue leggingsProtectionValue;
        private final ForgeConfigSpec.IntValue bootsProtectionValue;
        private final ForgeConfigSpec.DoubleValue armorToughnessValue;
        private final ForgeConfigSpec.DoubleValue armorKnockbackValue;
        private final ForgeConfigSpec.IntValue armorEnchantabilityValue;
        private final ForgeConfigSpec.IntValue armorDurabilityMultiplierValue;
        private final ForgeConfigSpec.IntValue toolDurabilityMultiplierValue;
        private final ForgeConfigSpec.DoubleValue toolBaseDamageValue;
        private final ForgeConfigSpec.IntValue toolEnchantabilityValue;
        private final ForgeConfigSpec.BooleanValue dragonAnvilNoCapValue;
        private final ForgeConfigSpec.BooleanValue advancedNetheriteGearPerksEnabledValue;

        public DragonLootConfig(ForgeConfigSpec.Builder builder) {
                builder.push("dragonloot");
                scaleMinimumDropAmountValue = builder.comment("Minimum guaranteed dragon scale drops from the Ender Dragon.")
                        .defineInRange("scale_minimum_drop_amount", 3, 0, 64);
                additionalScalesPerPlayerValue = builder.comment("Additional drop rolls per nearby player.")
                        .defineInRange("additional_scales_per_player", 2, 0, 32);
                additionalScaleDropChanceValue = builder.comment("Chance per additional roll to drop a scale (0.0 - 1.0).")
                        .defineInRange("additional_scale_drop_chance", 0.8D, 0.0D, 1.0D);

                builder.push("armor");
                helmetProtectionValue = builder.defineInRange("dragon_armor_protection_helmet", 7, 0, 20);
                chestplateProtectionValue = builder.defineInRange("dragon_armor_protection_chest", 10, 0, 20);
                leggingsProtectionValue = builder.defineInRange("dragon_armor_protection_leggings", 9, 0, 20);
                bootsProtectionValue = builder.defineInRange("dragon_armor_protection_boots", 7, 0, 20);
                armorToughnessValue = builder.defineInRange("dragon_armor_toughness", 3.0D, 0.0D, 10.0D);
                armorKnockbackValue = builder.defineInRange("dragon_armor_knockback_resistance", 1.0D, 0.0D, 5.0D);
                armorEnchantabilityValue = builder.defineInRange("dragon_armor_enchantability", 15, 1, 50);
                armorDurabilityMultiplierValue = builder.defineInRange("dragon_armor_durability_multiplier", 37, 1, 128);
                builder.pop();

                builder.push("tools");
                toolDurabilityMultiplierValue = builder.defineInRange("dragon_item_durability_multiplier", 37, 1, 256);
                toolBaseDamageValue = builder.defineInRange("dragon_item_base_damage", 5.0D, 0.0D, 20.0D);
                toolEnchantabilityValue = builder.defineInRange("dragon_tool_enchantability", 20, 1, 50);
                builder.pop();

                builder.push("compat");
                advancedNetheriteGearPerksEnabledValue = builder.comment("If true, Dragon Scale gear perks are enabled (pacifies mobs and shows Advanced Netherite-style tooltips).")
                        .define("advanced_netherite_gear_perks_enabled", false);
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
                dragon_armor_toughness = armorToughnessValue.get().floatValue();
                dragon_armor_knockback_resistance = armorKnockbackValue.get().floatValue();
                dragon_armor_enchantability = armorEnchantabilityValue.get();
                dragon_armor_durability_multiplier = armorDurabilityMultiplierValue.get();
                dragon_item_durability_multiplier = toolDurabilityMultiplierValue.get();
                dragon_item_base_damage = toolBaseDamageValue.get().floatValue();
                dragon_tool_enchantability = toolEnchantabilityValue.get();
                dragon_anvil_no_cap = dragonAnvilNoCapValue.get();
                advanced_netherite_gear_perks_enabled = advancedNetheriteGearPerksEnabledValue.get();
        }
}
