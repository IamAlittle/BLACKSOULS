package com.iamalittle.black_souls_options.mixins.forge;

import com.iamalittle.black_souls_options.contracts.effects.mobs.SquidContract;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for Player to handle Squid contract blindness defense
 * This mixin intercepts damage events to check if the player should apply blindness to attacker
 */
@Mixin(Player.class)
public class SquidBlindnessMixin {
    
    /**
     * Intercept the hurt method to check for Squid contract blindness effect
     * This method is called when a player takes damage
     */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onPlayerHurt(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        
        // 检查玩家是否拥有鱿鱼契约效果，触发失明防御效果
        if (SquidContract.hasSquidContract(player)) {
            // 获取攻击者
            Entity attacker = damageSource.getEntity();
            if (attacker != null && attacker != player) {
                SquidContract.onPlayerHurt(player, attacker);
            }
        }
    }
}