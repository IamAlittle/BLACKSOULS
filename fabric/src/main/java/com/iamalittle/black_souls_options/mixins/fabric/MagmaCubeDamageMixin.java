package com.iamalittle.black_souls_options.mixins.fabric;

import com.iamalittle.black_souls_options.contracts.effects.mobs.MagmaCubeContract;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric平台的岩浆怪契约伤害免疫Mixin
 * 这个Mixin直接拦截Player类的hurt方法，在伤害发生前检查免疫条件
 */
@Mixin(Player.class)
public class MagmaCubeDamageMixin {
    
    /**
     * 拦截hurt方法以检查岩浆怪契约的火焰伤害免疫效果
     * 这个方法在伤害发生前被调用，可以完全阻止伤害
     */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onPlayerHurt(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        
        // 检查玩家是否拥有岩浆怪契约效果，并且是否应该免疫火焰伤害
        if (MagmaCubeContract.onPlayerHurt(player, damageSource)) {
            // 免疫伤害，取消伤害事件
            cir.setReturnValue(false);
        }
    }
}