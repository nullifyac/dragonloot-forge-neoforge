package net.dragonloot.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.Map;
import java.util.function.Consumer;
import net.dragonloot.entity.DragonTridentEntity;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.item.render.DragonTridentBewlr;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class DragonTridentItem extends TridentItem {

    private final Multimap<Attribute, AttributeModifier> attributeModifiers;

    public DragonTridentItem(Item.Properties properties) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        for (Map.Entry<Attribute, AttributeModifier> entry : super.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).entries()) {
            Attribute attribute = entry.getKey();
            AttributeModifier modifier = entry.getValue();
            if (attribute.equals(Attributes.ATTACK_DAMAGE)) {
                double bonus = ConfigInit.CONFIG.dragon_item_base_damage / 5F;
                builder.put(attribute, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", modifier.getAmount() + bonus, Operation.ADDITION));
            } else {
                builder.put(attribute, modifier);
            }
        }
        this.attributeModifiers = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.attributeModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (living instanceof Player player) {
            int elapsed = this.getUseDuration(stack) - timeLeft;
            if (elapsed >= 10) {
                int riptideLevel = EnchantmentHelper.getRiptide(stack);
                if (riptideLevel <= 0 || player.isInWaterRainOrBubble() || player.isInLava()) {
                    if (!level.isClientSide) {
                        stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(living.getUsedItemHand()));
                        if (riptideLevel == 0) {
                            DragonTridentEntity tridentEntity = new DragonTridentEntity(level, player, stack);
                            tridentEntity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F + riptideLevel * 0.5F, 1.0F);
                            if (player.getAbilities().instabuild) {
                                tridentEntity.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                            }

                            level.addFreshEntity(tridentEntity);
                            level.playSound(null, tridentEntity, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
                            if (!player.getAbilities().instabuild) {
                                player.getInventory().removeItem(stack);
                            }
                        }
                    }

                    player.awardStat(Stats.ITEM_USED.get(this));
                    if (riptideLevel > 0) {
                        float yaw = player.getYRot();
                        float pitch = player.getXRot();
                        float x = -Mth.sin(yaw * (float) (Math.PI / 180.0F)) * Mth.cos(pitch * (float) (Math.PI / 180.0F));
                        float y = -Mth.sin(pitch * (float) (Math.PI / 180.0F));
                        float z = Mth.cos(yaw * (float) (Math.PI / 180.0F)) * Mth.cos(pitch * (float) (Math.PI / 180.0F));
                        float magnitude = Mth.sqrt(x * x + y * y + z * z);
                        float velocity = 3.0F * ((1.0F + riptideLevel) / 4.0F);
                        x *= velocity / magnitude;
                        y *= velocity / magnitude;
                        z *= velocity / magnitude;
                        player.push(x, y, z);
                        player.startAutoSpinAttack(20);
                        if (player.isOnGround()) {
                            float bounce = 1.1999999F;
                            player.move(MoverType.SELF, new Vec3(0.0D, bounce, 0.0D));
                        }

                        SoundEvent soundEvent;
                        if (riptideLevel >= 3) {
                            soundEvent = SoundEvents.TRIDENT_RIPTIDE_3;
                        } else if (riptideLevel == 2) {
                            soundEvent = SoundEvents.TRIDENT_RIPTIDE_2;
                        } else {
                            soundEvent = SoundEvents.TRIDENT_RIPTIDE_1;
                        }

                        level.playSound(null, player, soundEvent, SoundSource.PLAYERS, 1.0F, 1.0F);

                    }

                }
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            return InteractionResultHolder.fail(stack);
        } else if (EnchantmentHelper.getRiptide(stack) > 0 && !(player.isInWaterRainOrBubble() || player.isInLava())) {
            return InteractionResultHolder.fail(stack);
        } else {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    // Renderer creates its models lazily once the client finished baking layers.
                    this.renderer = new DragonTridentBewlr();
                }
                return this.renderer;
            }
        });
    }
}
