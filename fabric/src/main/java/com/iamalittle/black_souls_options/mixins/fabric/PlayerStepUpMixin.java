package com.iamalittle.black_souls_options.mixins.fabric;

import com.iamalittle.black_souls_options.contracts.effects.mobs.HorseContract;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric版本玩家爬坡能力增强Mixin
 * 让玩家可以跨越1整格高的方块
 */
@Mixin(Entity.class)
public class PlayerStepUpMixin {
    
    /**
     * 修改实体的最大爬坡高度
     * 对于拥有马契约的玩家实体，将最大爬坡高度设置为1.0F（1整格）
     */
    @Inject(method = "maxUpStep", at = @At("HEAD"), cancellable = true)
    private void increasePlayerStepHeight(CallbackInfoReturnable<Float> cir) {
        Entity entity = (Entity) (Object) this;
        
        // 只对玩家实体生效
        if (entity instanceof Player) {
            Player player = (Player) entity;
            
            // 检查玩家是否拥有马契约效果
            if (HorseContract.hasHorseContract(player)) {
                // 设置最大爬坡高度为1.0F（1整格）
                cir.setReturnValue(1.0F);
            }
        }
    }
}