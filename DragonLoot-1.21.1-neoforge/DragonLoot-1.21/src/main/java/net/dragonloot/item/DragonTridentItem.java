package net.dragonloot.item;

import net.dragonloot.entity.DragonTridentEntity;
import net.dragonloot.init.ConfigInit;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class DragonTridentItem extends TridentItem {

    public DragonTridentItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        ItemAttributeModifiers baseModifiers = TridentItem.createAttributes();
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        double bonus = ConfigInit.CONFIG.dragon_item_base_damage / 5F;

        for (ItemAttributeModifiers.Entry entry : baseModifiers.modifiers()) {
            if (entry.attribute().equals(Attributes.ATTACK_DAMAGE) && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
                builder.add(
                    entry.attribute(),
                    new AttributeModifier(entry.modifier().id(), entry.modifier().amount() + bonus, entry.modifier().operation()),
                    entry.slot());
            } else {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        return builder.build();
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (living instanceof Player player) {
            int elapsed = this.getUseDuration(stack, living) - timeLeft;
            if (elapsed >= 10) {
                float spinStrength = EnchantmentHelper.getTridentSpinAttackStrength(stack, player);
                if (isTooDamagedToUse(stack)) {
                    return;
                }
                if (spinStrength <= 0 || player.isInWaterRainOrBubble() || player.isInLava()) {
                    Holder<SoundEvent> sound = EnchantmentHelper.pickHighestLevel(stack, EnchantmentEffectComponents.TRIDENT_SOUND)
                        .orElse(SoundEvents.TRIDENT_THROW);
                    if (!level.isClientSide) {
                        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(living.getUsedItemHand()));
                        if (spinStrength == 0) {
                            DragonTridentEntity tridentEntity = new DragonTridentEntity(level, player, stack);
                            tridentEntity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 1.0F);
                            if (player.getAbilities().instabuild) {
                                tridentEntity.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                            }

                            level.addFreshEntity(tridentEntity);
                            level.playSound(null, tridentEntity, sound.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                            if (!player.getAbilities().instabuild) {
                                player.getInventory().removeItem(stack);
                            }
                        }
                    }

                    player.awardStat(Stats.ITEM_USED.get(this));
                    if (spinStrength > 0) {
                        float yaw = player.getYRot();
                        float pitch = player.getXRot();
                        float x = -Mth.sin(yaw * (float) (Math.PI / 180.0F)) * Mth.cos(pitch * (float) (Math.PI / 180.0F));
                        float y = -Mth.sin(pitch * (float) (Math.PI / 180.0F));
                        float z = Mth.cos(yaw * (float) (Math.PI / 180.0F)) * Mth.cos(pitch * (float) (Math.PI / 180.0F));
                        float magnitude = Mth.sqrt(x * x + y * y + z * z);
                        float velocity = spinStrength;
                        x *= velocity / magnitude;
                        y *= velocity / magnitude;
                        z *= velocity / magnitude;
                        player.push(x, y, z);
                        player.startAutoSpinAttack(20, 8.0F, stack);
                        if (player.onGround()) {
                            float bounce = 1.1999999F;
                            player.move(MoverType.SELF, new Vec3(0.0D, bounce, 0.0D));
                        }

                        level.playSound(null, player, sound.value(), SoundSource.PLAYERS, 1.0F, 1.0F);

                    }

                }
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isTooDamagedToUse(stack)) {
            return InteractionResultHolder.fail(stack);
        } else {
            if (EnchantmentHelper.getTridentSpinAttackStrength(stack, player) > 0 && !(player.isInWaterRainOrBubble() || player.isInLava())) {
                return InteractionResultHolder.fail(stack);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
    }

    private static boolean isTooDamagedToUse(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        DragonTridentEntity trident = new DragonTridentEntity(level, position.x(), position.y(), position.z(), stack.copyWithCount(1));
        trident.pickup = AbstractArrow.Pickup.ALLOWED;
        return trident;
    }
}
