package com.iamalittle.black_souls_options.ai.goal;

import com.iamalittle.black_souls_options.contracts.effects.mobs.CatContract;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * 躲避拥有猫契约的玩家的AI目标
 * 使用原版AvoidEntityGoal系统，与躲避猫的行为一致
 */
public class AvoidPlayerWithCatContractGoal extends AvoidEntityGoal<Player> {
    
    private final Creeper creeper;
    private boolean hasPlayedSound = false;
    
    public AvoidPlayerWithCatContractGoal(Creeper creeper) {
        super(creeper, Player.class, 6.0F, 1.0D, 1.2D);
        this.creeper = creeper;
    }
    
    /**
     * 检查是否可以开始执行此AI目标
     * 只有当附近有拥有猫契约的玩家时才执行
     */
    @Override
    public boolean canUse() {
        // 首先调用父类方法检查基本条件
        if (!super.canUse()) {
            this.hasPlayedSound = false; // 重置音效播放状态
            return false;
        }
        
        // 检查目标玩家是否拥有猫契约
        if (this.toAvoid != null) {
            boolean shouldAvoid = CatContract.shouldAvoidPlayer((Player) this.toAvoid);
            
            // 如果可以开始执行且还没有播放音效，则播放猫哈气音效
            if (shouldAvoid && !this.hasPlayedSound) {
                // 播放猫哈气音效，没有冷却时间，每次触发都会播放
                this.creeper.level().playSound(null, this.creeper.getX(), this.creeper.getY(), this.creeper.getZ(), 
                    SoundEvents.CAT_HISS, SoundSource.HOSTILE, 1.0F, 1.0F);
                this.hasPlayedSound = true;
            }
            
            return shouldAvoid;
        }
        
        this.hasPlayedSound = false; // 重置音效播放状态
        return false;
    }
    
    /**
     * 检查是否可以继续执行此AI目标
     * 只有当目标玩家仍然拥有猫契约时才继续
     */
    @Override
    public boolean canContinueToUse() {
        if (!super.canContinueToUse()) {
            this.hasPlayedSound = false; // 重置音效播放状态
            return false;
        }
        
        // 检查目标玩家是否仍然拥有猫契约
        if (this.toAvoid != null) {
            boolean shouldContinue = CatContract.shouldAvoidPlayer((Player) this.toAvoid);
            
            // 如果不再继续执行，重置音效状态以便下次触发时重新播放
            if (!shouldContinue) {
                this.hasPlayedSound = false;
            }
            
            return shouldContinue;
        }
        
        this.hasPlayedSound = false; // 重置音效播放状态
        return false;
    }
    
    /**
     * 当AI目标停止时重置音效状态
     */
    @Override
    public void stop() {
        super.stop();
        this.hasPlayedSound = false; // 重置音效播放状态，确保下次触发时重新播放
    }
}