package com.iamalittle.black_souls_options.mixins.fabric;

import com.iamalittle.black_souls_options.contracts.effects.mobs.WitchContract;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric平台的女巫契约魔法伤害减免Mixin
 * 这个Mixin拦截Player类的hurt方法，在伤害计算时检查并减免魔法伤害
 */
@Mixin(Player.class)
public class WitchDamageMixin {
    
    /**
     * 拦截hurt方法以检查女巫契约的魔法伤害减免效果
     * 这个方法在伤害计算时被调用，可以修改伤害值
     */
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float onPlayerHurt(float amount, DamageSource damageSource) {
        Player player = (Player) (Object) this;
        
        // 检查玩家是否拥有女巫契约效果，并且是否应该减免魔法伤害
        if (WitchContract.shouldReduceMagicDamage(player, damageSource)) {
            // 计算减免后的伤害值
            float reducedAmount = WitchContract.onPlayerHurt(player, damageSource, amount);
            return reducedAmount;
        }
        
        return amount;
    }
}