package com.iamalittle.black_souls_options.contracts.effects;

import com.iamalittle.black_souls_options.contracts.effects.mobs.BlazeContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.CaveSpiderContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.ZombieContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.HuskContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.DrownedContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.GuardianContract;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;

/**
 * 攻击事件处理器，用于处理玩家攻击时触发的契约效果
 * 包括烈焰人契约的点燃效果和洞穴蜘蛛契约的中毒效果
 */
public class AttackEventHandler {
    
    /**
     * 处理玩家攻击事件（Forge版本，包含伤害来源）
     * @param attacker 攻击者（玩家）
     * @param target 被攻击的目标
     * @param damageSource 伤害来源
     */
    public static void onPlayerAttack(Player attacker, Entity target, DamageSource damageSource) {
        if (attacker == null || target == null || !attacker.isAlive()) {
            return;
        }
        
        // 检查并触发烈焰人契约的点燃效果
        if (BlazeContract.hasBlazeContract(attacker)) {
            BlazeContract.igniteTarget(target, attacker);
        }
        
        // 检查并触发洞穴蜘蛛契约的中毒效果
        if (CaveSpiderContract.hasCaveSpiderContract(attacker)) {
            CaveSpiderContract.poisonTarget(target, attacker);
        }
        
        // 检查并触发僵尸契约的感染效果
        if (ZombieContract.hasZombieContract(attacker)) {
            ZombieContract.tryConvertVillager(target, attacker);
        }
        
        // 检查并触发尸壳契约的饥饿效果和感染效果
        if (HuskContract.hasHuskContract(attacker)) {
            HuskContract.applyHungerToTarget(target, attacker);
            HuskContract.tryConvertVillager(target, attacker);
        }
        
        // 检查并触发溺尸契约的缓慢效果和感染效果
        if (DrownedContract.hasDrownedContract(attacker)) {
            DrownedContract.applySlownessToTarget(target, attacker, damageSource);
            DrownedContract.tryConvertVillager(target, attacker);
        }
    }
    
    /**
     * 处理玩家攻击事件（Fabric版本，不包含伤害来源）
     * @param attacker 攻击者（玩家）
     * @param target 被攻击的目标
     */
    public static void onPlayerAttack(Player attacker, Entity target) {
        if (attacker == null || target == null || !attacker.isAlive()) {
            return;
        }
        
        // 检查并触发烈焰人契约的点燃效果
        if (BlazeContract.hasBlazeContract(attacker)) {
            BlazeContract.igniteTarget(target, attacker);
        }
        
        // 检查并触发洞穴蜘蛛契约的中毒效果
        if (CaveSpiderContract.hasCaveSpiderContract(attacker)) {
            CaveSpiderContract.poisonTarget(target, attacker);
        }
        
        // 检查并触发僵尸契约的感染效果
        if (ZombieContract.hasZombieContract(attacker)) {
            ZombieContract.tryConvertVillager(target, attacker);
        }
        
        // 检查并触发尸壳契约的饥饿效果和感染效果
        if (HuskContract.hasHuskContract(attacker)) {
            HuskContract.applyHungerToTarget(target, attacker);
            HuskContract.tryConvertVillager(target, attacker);
        }
        
        // 检查并触发溺尸契约的缓慢效果和感染效果
        if (DrownedContract.hasDrownedContract(attacker)) {
            DrownedContract.applySlownessToTarget(target, attacker);
            DrownedContract.tryConvertVillager(target, attacker);
        }
    }
}