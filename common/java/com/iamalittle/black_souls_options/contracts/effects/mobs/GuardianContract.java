package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import io.netty.util.concurrent.BlockingOperationException;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import java.util.*;
import java.util.Collection;

/**
 * 古守卫者契约效果 - 让周围一格的任何流体消失
 * 玩家契约古守卫者后获得的能力：
 * 1. 让玩家周围一格范围内的任何流体方块消失
 */
public class GuardianContract extends ContractEffect {
    private static final String EFFECT_ID = "guardian_fluid_clear";
    private static final String DISPLAY_NAME = "我是个海绵";
    private static final String DESCRIPTION = "让周围一格的任何流体消失";
    
    // 流体方块消失范围（以玩家为中心的正方体边长）
    private static final int FLUID_CLEAR_RANGE = 3; // 3格范围（玩家为中心，左右各1格）
    
    // 古守卫者契约玩家集合
    private static final Set<UUID> guardianContractPlayers = new HashSet<>();
    
    // 记录上次清理水方块的位置，避免重复清理
    private static final Map<UUID, Set<BlockPos>> lastClearedPositions = new HashMap<>();
    
    // 记录契约给予的挖掘疲劳效果实例（改为实例变量，避免静态共享问题）
    private MobEffectInstance contractMiningFatigueEffect = null;
    
    public GuardianContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            guardianContractPlayers.add(player.getUUID());
            lastClearedPositions.put(player.getUUID(), new HashSet<>());
            applyMiningFatigue(player);
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
            guardianContractPlayers.remove(player.getUUID());
            lastClearedPositions.remove(player.getUUID());
            
            // 清除挖掘疲劳效果
            clearMiningFatigue(player);
            
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
        if (player != null && player.isAlive() && !player.level().isClientSide()) {
            // 清理玩家周围的流体方块
            clearFluidAroundPlayer(player);

        }
    }
    
    /**
     * 清理玩家周围的流体方块
     */
    private void clearFluidAroundPlayer(Player player) {
        BlockPos playerPos = player.blockPosition();
        UUID playerUUID = player.getUUID();
        Set<BlockPos> currentClearedPositions = new HashSet<>();
        
        // 获取玩家周围的范围
        int range = FLUID_CLEAR_RANGE / 2;
        
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    
                    // 检查是否为流体方块
                    BlockState blockState = player.level().getBlockState(checkPos);
                    if (isFluidBlock(blockState)) {
                        // 将流体方块替换为空气
                        player.level().setBlockAndUpdate(checkPos, Blocks.AIR.defaultBlockState());
                        currentClearedPositions.add(checkPos);
                    }
                }
            }
        }
        
        // 更新清理记录
        lastClearedPositions.put(playerUUID, currentClearedPositions);
    }
    
    /**
     * 检查方块是否为流体方块
     */
    private boolean isFluidBlock(BlockState blockState) {
        // 检查方块是否为流体方块
        FluidState fluidState = blockState.getFluidState();
        return !fluidState.isEmpty(); // 如果流体状态不为空，说明是流体方块
    }
    
    /**
     * 给玩家施加挖掘疲劳效果（副作用）
     */
    private void applyMiningFatigue(Player player) {
        // 检查是否已经给予过效果
        if (contractMiningFatigueEffect == null) {
            // 施加挖掘疲劳效果，持续时间为无限（-1），等级为1
            contractMiningFatigueEffect = new MobEffectInstance(MobEffects.DIG_SLOWDOWN, -1, 0);
            player.addEffect(contractMiningFatigueEffect);
        }
    }
    
    /**
     * 清除契约给予的挖掘疲劳效果
     */
    private void clearMiningFatigue(Player player) {
        // 检查是否有契约给予的挖掘疲劳效果
        if (contractMiningFatigueEffect != null) {
            // 安全地移除特定效果实例
            removeSpecificEffect(player, contractMiningFatigueEffect);
            
            // 从记录中移除
            contractMiningFatigueEffect = null;
        }
    }
    
    /**
     * 安全地移除特定的效果实例（模仿美西螈契约的实现）
     */
    private void removeSpecificEffect(Player player, MobEffectInstance effectToRemove) {
        // 获取玩家当前的所有效果
        Collection<MobEffectInstance> activeEffects = player.getActiveEffects();
        
        // 遍历效果列表，只移除与指定效果匹配的实例
        for (MobEffectInstance effect : activeEffects) {
            if (effect.getEffect() == effectToRemove.getEffect() && 
                effect.getAmplifier() == effectToRemove.getAmplifier()) {
                // 找到匹配的效果，移除它
                player.removeEffect(effect.getEffect());
                break;
            }
        }
    }
    
    /**
     * 检查玩家是否拥有古守卫者契约效果
     */
    public static boolean hasGuardianContract(Player player) {
        return player != null && guardianContractPlayers.contains(player.getUUID());
    }
    @Override
    protected long getTickInterval() {
        return 500;
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6古守卫者契约效果："));
        details.add(Component.literal("§7- 让玩家周围" + FLUID_CLEAR_RANGE + "格范围内的任何流体方块消失"));
        details.add(Component.literal("§c- 持续获得挖掘疲劳效果"));
        return details;
    }
}