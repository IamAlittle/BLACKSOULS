package com.iamalittle.black_souls_options.mixins.fabric;

import com.iamalittle.black_souls_options.contracts.effects.mobs.SkeletonContract;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 骷髅契约无限箭矢Mixin（Fabric版）
 * 让拥有骷髅契约的玩家在生存模式下也能无限拉弓射箭
 */
@Mixin(BowItem.class)
public class SkeletonInfiniteArrowsMixin {

    /**
     * 修改弓的use方法，允许拥有骷髅契约的玩家在没有箭矢时也能开始使用弓
     */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void allowInfiniteArrowUse(Level level, Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack bowStack = player.getItemInHand(interactionHand);
        
        // 检查玩家是否有骷髅契约
        if (SkeletonContract.hasSkeletonContract(player)) {
            // 允许玩家开始使用弓，就像有箭矢一样
            player.startUsingItem(interactionHand);
            cir.setReturnValue(InteractionResultHolder.consume(bowStack));
        }
    }

    /**
     * 修改弓的releaseUsing方法，确保骷髅契约玩家可以射箭
     * 模拟无限附魔的行为：当箭矢为空时创建虚拟箭矢，但不消耗
     */
    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    public void onReleaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged, CallbackInfo ci) {
        if (livingEntity instanceof Player player && SkeletonContract.hasSkeletonContract(player)) {
            // 检查玩家是否有箭矢或无限附魔
            ItemStack arrowStack = player.getProjectile(stack);
            boolean hasInfiniteArrows = SkeletonContract.canUseInfiniteArrows(player);
            
            // 如果没有箭矢但拥有骷髅契约，模拟无限附魔行为
            if (arrowStack.isEmpty() && hasInfiniteArrows) {
                // 创建虚拟箭矢用于射箭，但不实际消耗
                arrowStack = new ItemStack(Items.ARROW);
                
                // 继续执行原版射箭逻辑，但跳过箭矢消耗部分
                int j = ((BowItem)(Object)this).getUseDuration(stack) - timeCharged;
                float f = BowItem.getPowerForTime(j);
                
                if (!((double)f < 0.1)) {
                    if (!level.isClientSide) {
                        ArrowItem arrowItem = (ArrowItem)(Items.ARROW);
                        AbstractArrow abstractArrow = arrowItem.createArrow(level, arrowStack, player);
                        abstractArrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, f * 3.0F, 1.0F);
                        
                        if (f == 1.0F) {
                            abstractArrow.setCritArrow(true);
                        }

                        // 应用弓的附魔效果
                        int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, stack);
                        if (powerLevel > 0) {
                            abstractArrow.setBaseDamage(abstractArrow.getBaseDamage() + (double)powerLevel * 0.5D + 0.5D);
                        }

                        int punchLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, stack);
                        if (punchLevel > 0) {
                            abstractArrow.setKnockback(punchLevel);
                        }

                        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, stack) > 0) {
                            abstractArrow.setSecondsOnFire(100);
                        }

                        // 消耗弓的耐久度
                        stack.hurtAndBreak(1, player, (player2) -> player2.broadcastBreakEvent(player.getUsedItemHand()));
                        
                        // 设置为创造模式拾取，避免消耗箭矢
                        abstractArrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                        
                        level.addFreshEntity(abstractArrow);
                    }

                    // 播放射箭音效
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + f * 0.5F);
                    
                    // 不消耗箭矢（模拟无限附魔）
                    player.awardStat(Stats.ITEM_USED.get((BowItem)(Object)this));
                    
                    // 取消原版方法的执行
                    ci.cancel();
                }
            }
        }
    }
}