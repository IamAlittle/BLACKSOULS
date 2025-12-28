package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 猫契约效果 - 苦力怕逃离
 * 玩家契约猫后获得的能力：
 * 1. 苦力怕会像原版一样逃离拥有猫契约的玩家
 */
public class CatContract extends ContractEffect {
    private static final String EFFECT_ID = "cat_scare_creeper";
    private static final String DISPLAY_NAME = "哈气";
    private static final String DESCRIPTION = "使幻翼/苦力怕远离玩家";
    
    // 逃离距离（方块）
    private static final double FLEE_DISTANCE = 6.0;
    
    // 猫契约玩家集合
    private static final Set<UUID> catContractPlayers = new HashSet<>();
    
    public CatContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            catContractPlayers.add(player.getUUID());
            
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
            catContractPlayers.remove(player.getUUID());
            
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
        // 猫契约不需要每tick更新，逃离逻辑在Creeper的AI中处理
        
        // 检查并驱散附近的幻翼
        if (player != null && player.isAlive() && hasCatContract(player)) {
            scareAwayPhantoms(player);
        }
    }
    
    /**
     * 检查玩家是否拥有猫契约效果
     */
    public static boolean hasCatContract(Player player) {
        return player != null && catContractPlayers.contains(player.getUUID());
    }
    
    /**
     * 检查玩家是否应该被苦力怕躲避
     * 如果玩家拥有猫契约，苦力怕会像躲避猫一样躲避玩家
     */
    public static boolean shouldAvoidPlayer(Player player) {
        return player != null && player.isAlive() && hasCatContract(player);
    }
    
    /**
     * 驱散玩家附近的幻翼
     * 当玩家拥有猫契约时，附近的幻翼会被推开并失去对玩家的仇恨
     */
    private static void scareAwayPhantoms(Player player) {
        if (player.level().isClientSide()) {
            return; // 只在服务器端执行
        }
        
        // 检测玩家周围20格范围内的幻翼
        AABB searchArea = new AABB(player.getX() - 20, player.getY() - 10, player.getZ() - 20,
                                   player.getX() + 20, player.getY() + 10, player.getZ() + 20);
        
        List<Phantom> nearbyPhantoms = player.level().getEntitiesOfClass(Phantom.class, searchArea);
        
        for (Phantom phantom : nearbyPhantoms) {
            if (phantom.isAlive() && phantom.distanceTo(player) <= 20.0) {
                // 消除幻翼对玩家的仇恨
                phantom.setTarget(null);
                
                // 计算推开方向（远离玩家）
                Vec3 pushDirection = phantom.position().subtract(player.position()).normalize();
                
                // 施加推开力（没有冷却，每次检测都会推开，力度增强到3.0倍）
                phantom.setDeltaMovement(pushDirection.x * 5.0,
                                        Math.max(0.8, pushDirection.y * 1.2 + 0.5), 
                                        pushDirection.z * 5.0);
                
                // 播放幻翼被驱散的声音（音量增强到1.2F）
                phantom.level().playSound(null, phantom.getX(), phantom.getY(), phantom.getZ(),
                    SoundEvents.PHANTOM_HURT, SoundSource.HOSTILE, 1.2F, 1.0F);
            }
        }
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6猫契约效果："));
        details.add(Component.literal("§7哈！"));
        details.add(Component.literal("§7- 苦力怕会在" + FLEE_DISTANCE + "格范围内逃离玩家"));
        details.add(Component.literal("§7幻翼驱散"));
        details.add(Component.literal("§7- 20格范围内的幻翼会被强力推开并失去仇恨"));
        return details;
    }
    
    @Override
    public CompoundTag saveToNBT() {
        return super.saveToNBT();
    }
    
    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
    }
}