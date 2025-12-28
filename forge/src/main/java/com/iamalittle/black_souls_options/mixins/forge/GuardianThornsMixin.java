package com.iamalittle.black_souls_options.mixins.forge;

import com.iamalittle.black_souls_options.contracts.effects.mobs.GuardianThornsContract;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for Player to handle Guardian contract thorns damage reflection
 * This mixin intercepts damage events to check if the player should reflect damage
 */
@Mixin(Player.class)
public class GuardianThornsMixin {
    
    /**
     * Intercept the hurt method to check for Guardian contract thorns reflection
     * This method is called when a player takes damage
     */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onPlayerHurt(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        
        // 检查玩家是否拥有守卫者契约效果，触发反伤效果
        if (GuardianThornsContract.hasGuardianThornsContract(player)) {
            GuardianThornsContract.onPlayerHurt(player, damageSource, amount);
        }
    }
}