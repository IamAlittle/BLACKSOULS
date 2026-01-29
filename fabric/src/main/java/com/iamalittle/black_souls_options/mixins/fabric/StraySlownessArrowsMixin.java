package com.iamalittle.black_souls_options.mixins.fabric;

import com.iamalittle.black_souls_options.contracts.effects.mobs.StrayContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.SkeletonContract;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
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
 * 流浪者契约迟缓之箭Mixin（Fabric版）
 * 实现箭矢优先级系统：
 * 背包里其他类型的箭 > 流浪者契约的迟缓之箭 > 骷髅契约的箭 > 背包里普通的箭
 */
@Mixin(BowItem.class)
public class StraySlownessArrowsMixin {

    /**
     * 修改弓的use方法，允许拥有流浪者或骷髅契约的玩家在没有箭矢时也能开始使用弓
     */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void allowContractArrowUse(Level level, Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack bowStack = player.getItemInHand(interactionHand);
        
        // 检查玩家是否有流浪者或骷髅契约
        if (StrayContract.hasStrayContract(player) || SkeletonContract.hasSkeletonContract(player)) {
            // 允许玩家开始使用弓，就像有箭矢一样
            player.startUsingItem(interactionHand);
            cir.setReturnValue(InteractionResultHolder.consume(bowStack));
        }
    }

    /**
     * 修改弓的releaseUsing方法，实现箭矢优先级系统
     */
    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    public void onReleaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged, CallbackInfo ci) {
        if (livingEntity instanceof Player player && 
            (StrayContract.hasStrayContract(player) || SkeletonContract.hasSkeletonContract(player))) {
            
            // 获取玩家当前的箭矢
            ItemStack arrowStack = player.getProjectile(stack);
            
            // 优先级系统：
            // 1. 如果玩家背包里有特殊箭矢（药水箭、光灵箭等），优先使用
            // 2. 如果有流浪者契约，使用迟缓之箭
            // 3. 如果有骷髅契约，使用普通箭矢
            // 4. 否则使用背包里的普通箭矢
            
            boolean shouldUseContractArrow = false;
            ItemStack contractArrowStack = ItemStack.EMPTY;
            
            // 检查优先级
            if (!StrayContract.hasSpecialArrowsInInventory(player)) {
                // 没有特殊箭矢时，使用契约箭矢
                if (StrayContract.hasStrayContract(player)) {
                    // 流浪者契约：使用迟缓之箭
                    contractArrowStack = new ItemStack(Items.TIPPED_ARROW);
                    PotionUtils.setPotion(contractArrowStack, Potions.SLOWNESS);
                    shouldUseContractArrow = true;
                } else if (SkeletonContract.hasSkeletonContract(player) && arrowStack.isEmpty()) {
                    // 骷髅契约：使用普通箭矢（仅在背包没有箭矢时）
                    contractArrowStack = new ItemStack(Items.ARROW);
                    shouldUseContractArrow = true;
                }
            }
            
            // 如果应该使用契约箭矢，并且当前没有箭矢或需要替换
            if (shouldUseContractArrow && (arrowStack.isEmpty() || !StrayContract.isSpecialArrow(arrowStack))) {
                // 继续执行原版射箭逻辑，但使用契约箭矢
                int j = ((BowItem)(Object)this).getUseDuration(stack) - timeCharged;
                float f = BowItem.getPowerForTime(j);
                
                if (!((double)f < 0.1)) {
                    if (!level.isClientSide) {
                        ArrowItem arrowItem = (ArrowItem)(Items.ARROW);
                        AbstractArrow abstractArrow = arrowItem.createArrow(level, contractArrowStack, player);
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
                        
                        // 如果使用契约箭矢，设置为创造模式拾取（避免消耗箭矢）
                        if (!StrayContract.hasSpecialArrowsInInventory(player)) {
                            abstractArrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                        }
                        
                        level.addFreshEntity(abstractArrow);
                    }

                    // 播放射箭音效
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + f * 0.5F);
                    
                    // 不消耗箭矢（如果使用契约箭矢）
                    if (!StrayContract.hasSpecialArrowsInInventory(player)) {
                        player.awardStat(Stats.ITEM_USED.get((BowItem)(Object)this));
                    }
                    
                    // 取消原版方法的执行
                    ci.cancel();
                }
            }
        }
    }
}