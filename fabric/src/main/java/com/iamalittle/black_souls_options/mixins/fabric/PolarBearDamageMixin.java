package com.iamalittle.black_souls_options.mixins.fabric;

import com.iamalittle.black_souls_options.contracts.effects.mobs.PolarBearContract;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for Player to handle Polar Bear contract freeze damage immunity
 * This mixin intercepts damage events to check if the player should be immune to powder snow freeze damage
 */
@Mixin(Player.class)
public class PolarBearDamageMixin {
    
    /**
     * Intercept the hurt method to check for Polar Bear contract freeze damage immunity
     * This method is called when a player takes damage
     */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onPlayerHurt(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        
        // 检查玩家是否拥有北极熊契约效果，免疫细雪冻伤伤害
        if (PolarBearContract.hasPolarBearContract(player)) {
            if (PolarBearContract.onPlayerHurt(player, damageSource)) {
                cir.setReturnValue(false); // 取消伤害
                return;
            }
        }
    }
}