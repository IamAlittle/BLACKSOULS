package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import com.iamalittle.black_souls_options.input.ContractAbilityKeyManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * 羊驼契约效果 - 吐口水能力
 * 玩家契约羊驼后获得的能力：
 * 1. 可以像羊驼一样吐口水攻击敌人
 * 2. 口水攻击会造成伤害和击退效果
 */
public class LlamaContract extends ContractEffect {
    private static final String EFFECT_ID = "llama_spit_attack";
    private static final String DISPLAY_NAME = "羊驼";
    private static final String DESCRIPTION = "可以吐口水攻击敌人";
    
    // 羊驼契约玩家集合
    private static final Set<UUID> llamaContractPlayers = new HashSet<>();
    
    // 吐口水伤害
    private static final float SPIT_DAMAGE = 1.0f;
    
    public LlamaContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            llamaContractPlayers.add(player.getUUID());
            
            // 使用契约目标名称发送消息（仅在需要时发送）
            if (sendMessage) {
                String entityName = effectData.getString("contractEntityName");
                if (entityName.isEmpty()) {
                    entityName = displayName; // 回退到效果名称
                }
                sendActivationMessage(player, entityName);
            }
        }
    }
    
    @Override
    protected void onDeactivate(Player player) {
        if (player != null) {
            llamaContractPlayers.remove(player.getUUID());
            
            // 使用契约目标名称发送消息
            String entityName = effectData.getString("contractEntityName");
            if (entityName.isEmpty()) {
                entityName = displayName; // 回退到效果名称
            }
            sendDeactivationMessage(player, entityName);
        }
    }
    
    @Override
    protected void onTick(Player player) {
        // 检查按键状态，如果按下则触发吐口水能力
        if (ContractAbilityKeyManager.isAbilityKeyPressed()) {
            performSpitAttack(player);
        }
    }
    
    /**
     * 执行吐口水攻击
     * @param player 玩家
     * @return 是否成功执行吐口水
     */
    public static boolean performSpitAttack(Player player) {
        if (player == null || !llamaContractPlayers.contains(player.getUUID())) {
            return false;
        }
        
        // 检查玩家是否在生存模式
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }
        
        // 执行吐口水攻击
        if (spitAtTarget(player)) {
            // 播放吐口水音效
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                SoundEvents.LLAMA_SPIT, SoundSource.PLAYERS, 1.0f, 1.0f);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * 向目标方向吐口水
     * @param player 玩家
     * @return 是否成功吐口水
     */
    private static boolean spitAtTarget(Player player) {
        if (player.level().isClientSide()) {
            return false; // 只在服务端执行
        }
        
        // 获取玩家视线方向
        Vec3 lookVec = player.getLookAngle();
        
        // 计算吐口水的起始位置（玩家嘴巴位置，稍微向前偏移）
        double startX = player.getX() + lookVec.x * 0.3;
        double startY = player.getY() + player.getEyeHeight() - 0.1; // 稍微降低高度
        double startZ = player.getZ() + lookVec.z * 0.3;
        
        // 创建羊驼口水实体
        LlamaSpit spit = new LlamaSpit(EntityType.LLAMA_SPIT, player.level());
        spit.setOwner(player);
        spit.setPos(startX, startY, startZ);
        
        // 设置口水速度和方向（增加速度）
        double speed = 2.0; // 增加口水速度
        spit.shoot(lookVec.x, lookVec.y, lookVec.z, (float) speed, 1.0f);
        
        // 添加到世界
        player.level().addFreshEntity(spit);
        
        return true;
    }
    
    /**
     * 检查玩家是否拥有羊驼契约效果
     */
    public static boolean hasLlamaContract(Player player) {
        return player != null && llamaContractPlayers.contains(player.getUUID());
    }
    

    
    @Override
    protected long getTickInterval() {
        return 100; // 每100毫秒检测一次，确保能捕获按键事件
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6羊驼契约效果："));
        details.add(Component.literal("§7- 可以像羊驼一样吐口水攻击敌人"));
        details.add(Component.literal("§7- 1点伤害：）"));
        return details;
    }
}