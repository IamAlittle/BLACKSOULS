package com.iamalittle.black_souls_options.mixins.fabric;

import com.iamalittle.black_souls_options.ai.goal.AvoidPlayerWithCatContractGoal;
import com.iamalittle.black_souls_options.config.BlackSoulsConfig;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * 苦力怕AI Mixin - 为苦力怕添加躲避拥有猫契约玩家的AI目标
 */
@Mixin(Creeper.class)
public class CreeperAIMixin {
    
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void onRegisterGoals(CallbackInfo ci) {
        Creeper creeper = (Creeper) (Object) this;
        
        try {
            // 使用反射安全地访问protected字段
            Field goalSelectorField = Mob.class.getDeclaredField("goalSelector");
            goalSelectorField.setAccessible(true);
            GoalSelector goalSelector = (GoalSelector) goalSelectorField.get(creeper);
            
            // 添加躲避拥有猫契约玩家的AI目标
            // 优先级为2，与躲避猫和豹猫的优先级相同
            goalSelector.addGoal(2, new AvoidPlayerWithCatContractGoal(creeper));
        } catch (Exception e) {
            BlackSoulsConfig.error("[BLACKSOULS] Failed to add cat contract avoidance AI to creeper: " + e.getMessage());
        }
    }
}