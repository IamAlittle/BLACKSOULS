package com.iamalittle.black_souls_options.mixins.forge;

import com.iamalittle.black_souls_options.ai.goal.AvoidPlayerWithCatContractGoal;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 苦力怕AI Mixin - 为苦力怕添加躲避拥有猫契约玩家的AI目标
 */
@Mixin(Creeper.class)
public class CreeperAIMixin {
    
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void onRegisterGoals(CallbackInfo ci) {
        Creeper creeper = (Creeper) (Object) this;
        
        // 添加躲避拥有猫契约玩家的AI目标
        // 优先级为3，与躲避猫和豹猫的优先级相同
        creeper.goalSelector.addGoal(3, new AvoidPlayerWithCatContractGoal(creeper));
    }
}