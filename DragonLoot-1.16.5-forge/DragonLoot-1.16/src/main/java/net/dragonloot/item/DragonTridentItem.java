package net.dragonloot.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.Map;
import net.dragonloot.entity.DragonTridentEntity;
import net.dragonloot.init.ConfigInit;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class DragonTridentItem extends TridentItem {

    private final Multimap<Attribute, AttributeModifier> attributeModifiers;
    public DragonTridentItem(Item.Properties properties) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        for (Map.Entry<Attribute, AttributeModifier> entry : super.getDefaultAttributeModifiers(EquipmentSlotType.MAINHAND).entries()) {
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
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlotType slot, ItemStack stack) {
        return slot == EquipmentSlotType.MAINHAND ? this.attributeModifiers : super.getAttributeModifiers(slot, stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, World level, LivingEntity living, int timeLeft) {
        if (living instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) living;
            int elapsed = this.getUseDuration(stack) - timeLeft;
            if (elapsed >= 10) {
                int riptideLevel = EnchantmentHelper.getRiptide(stack);
                if (riptideLevel <= 0 || player.isInWaterRainOrBubble() || player.isInLava()) {
                    if (!level.isClientSide) {
                        stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(living.getUsedItemHand()));
                        if (riptideLevel == 0) {
                            DragonTridentEntity tridentEntity = new DragonTridentEntity(level, player, stack);
                            tridentEntity.shootFromRotation(player, player.xRot, player.yRot, 0.0F, 2.5F + riptideLevel * 0.5F, 1.0F);
                            if (player.abilities.instabuild) {
                                tridentEntity.pickup = AbstractArrowEntity.PickupStatus.CREATIVE_ONLY;
                            }

                            level.addFreshEntity(tridentEntity);
                            level.playSound(null, tridentEntity, SoundEvents.TRIDENT_THROW, SoundCategory.PLAYERS, 1.0F, 1.0F);
                            if (!player.abilities.instabuild) {
                                player.inventory.removeItem(stack);
                            }
                        }
                    }

                    player.awardStat(Stats.ITEM_USED.get(this));
                    if (riptideLevel > 0) {
                        float yaw = player.yRot;
                        float pitch = player.xRot;
                        float x = -MathHelper.sin(yaw * (float) (Math.PI / 180.0F)) * MathHelper.cos(pitch * (float) (Math.PI / 180.0F));
                        float y = -MathHelper.sin(pitch * (float) (Math.PI / 180.0F));
                        float z = MathHelper.cos(yaw * (float) (Math.PI / 180.0F)) * MathHelper.cos(pitch * (float) (Math.PI / 180.0F));
                        float magnitude = MathHelper.sqrt(x * x + y * y + z * z);
                        float velocity = 3.0F * ((1.0F + riptideLevel) / 4.0F);
                        x *= velocity / magnitude;
                        y *= velocity / magnitude;
                        z *= velocity / magnitude;
                        player.push(x, y, z);
                        player.startAutoSpinAttack(20);
                        if (player.isOnGround()) {
                            float bounce = 1.1999999F;
                            player.move(MoverType.SELF, new Vector3d(0.0D, bounce, 0.0D));
                        }

                        SoundEvent soundEvent;
                        if (riptideLevel >= 3) {
                            soundEvent = SoundEvents.TRIDENT_RIPTIDE_3;
                        } else if (riptideLevel == 2) {
                            soundEvent = SoundEvents.TRIDENT_RIPTIDE_2;
                        } else {
                            soundEvent = SoundEvents.TRIDENT_RIPTIDE_1;
                        }

                        level.playSound(null, player, soundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);

                    }
                }
            }
        }
    }

    @Override
    public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            return ActionResult.fail(stack);
        } else if (EnchantmentHelper.getRiptide(stack) > 0 && !(player.isInWaterRainOrBubble() || player.isInLava())) {
            return ActionResult.fail(stack);
        } else {
            player.startUsingItem(hand);
            return ActionResult.consume(stack);
        }
    }

}
