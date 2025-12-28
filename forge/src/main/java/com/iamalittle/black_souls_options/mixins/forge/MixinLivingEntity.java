package com.iamalittle.black_souls_options.mixins.forge;

import com.iamalittle.black_souls_options.common.Events;
import com.iamalittle.black_souls_options.contracts.effects.mobs.AxolotlContract;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for LivingEntity to listen for entity death events and prevent item usage when feigning death
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    /**
     * Inject into the die method to trigger our custom death event
     */
    @Inject(method = "die", at = @At("HEAD"))
    private void onDie(DamageSource damageSource, CallbackInfo ci) {
        // Trigger our custom entity death event with damage source
        Events.EntityDeath.trigger(new Events.EntityDeathEvent((LivingEntity)(Object)this, damageSource));
    }
    
    /**
     * Prevent starting to use items when feigning death
     * This method intercepts the startUsingItem method to prevent item usage
     */
    @Inject(method = "startUsingItem", at = @At("HEAD"), cancellable = true)
    private void preventStartUsingItem(InteractionHand hand, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity instanceof net.minecraft.world.entity.player.Player) {
            net.minecraft.world.entity.player.Player player = (net.minecraft.world.entity.player.Player) entity;
            
            // 检查玩家是否正在装死
            if (AxolotlContract.isPlayerFeigningDeath(player)) {
                // 取消开始使用物品的动作
                ci.cancel();
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c装死时无法使用物品！"), true);
            }
        }
    }
}