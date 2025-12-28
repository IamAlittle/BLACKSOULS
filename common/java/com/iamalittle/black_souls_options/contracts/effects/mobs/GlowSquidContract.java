package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.*;

/**
 * 发光鱿鱼契约效果 - 优化版本
 * 智能跟随光源系统，玩家移动时立即更新光源位置
 * 玩家离开服务器时自动清理光源方块
 */
public class GlowSquidContract extends ContractEffect {
    private static final String EFFECT_ID = "glow_squid_glowing";
    private static final String DISPLAY_NAME = "我是发光鱿鱼";
    private static final String DESCRIPTION = "获得发光效果并照亮周围";
    
    // 发光鱿鱼契约玩家集合
    private static final Set<UUID> glowSquidContractPlayers = new HashSet<>();
    
    // 光源检测范围（格）
    private static final int LIGHT_RANGE = 2;
    
    // 存储每个玩家的光源位置，用于清理
    private static final Map<UUID, BlockPos> playerLightPositions = new HashMap<>();
    
    // 存储每个玩家的最后位置，用于检测移动
    private static final Map<UUID, BlockPos> playerLastPositions = new HashMap<>();
    
    public GlowSquidContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            glowSquidContractPlayers.add(player.getUUID());
            
            // 记录玩家当前位置
            playerLastPositions.put(player.getUUID(), player.blockPosition());
            
            // 清理玩家周围的旧光源方块
            cleanupLightBlocks(player);

            // 立即应用发光效果
            applyGlowingEffect(player);
            
            // 立即放置光源方块
            updateSurroundingLight(player);
            
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
            // 清理玩家周围的旧光源方块
            cleanupLightBlocks(player);

            glowSquidContractPlayers.remove(player.getUUID());

            // 移除发光效果
            removeGlowingEffect(player);

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
        if (player == null || player.level().isClientSide()) {
            return;
        }
        
        // 每2秒检查一次发光效果，确保效果持续存在
        if (player.level().getGameTime() % 40 == 0) {
            applyGlowingEffect(player);
        }
        
        // 每tick都检查是否需要更新光源位置
        updateSurroundingLight(player);
    }
    
    /**
     * 应用发光效果给玩家
     */
    private void applyGlowingEffect(Player player) {
        if (player != null && player.isAlive()) {
            // 检查玩家是否已经有发光效果
            MobEffectInstance currentGlow = player.getEffect(MobEffects.GLOWING);
            
            // 如果效果不存在或即将过期，重新应用
            if (currentGlow == null || currentGlow.getDuration() < 100) {
                // 应用发光效果，持续时间为30秒，但会定期刷新
                MobEffectInstance glowEffect = new MobEffectInstance(
                    MobEffects.GLOWING, 
                    45,
                    0, // 等级0
                    false, // 不显示粒子
                    true // 显示图标
                );
                
                player.addEffect(glowEffect);

            }
        }
    }
    
    /**
     * 更新玩家周围的光照
     * 智能跟随光源系统：玩家移动时立即更新光源位置
     */
    private void updateSurroundingLight(Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        
        UUID playerId = player.getUUID();
        BlockPos currentPos = player.blockPosition();
        
        // 获取玩家最后记录的位置
        BlockPos lastPos = playerLastPositions.get(playerId);
        
        // 检查玩家是否移动了（即使只有0.1格）
        boolean hasMoved = false;
        if (lastPos != null) {
            double distance = Math.sqrt(currentPos.distSqr(lastPos));
            if (distance > 0.1) {
                hasMoved = true;
                // 更新最后位置
                playerLastPositions.put(playerId, currentPos);
            }
        } else {
            // 没有记录位置，记录当前位置
            playerLastPositions.put(playerId, currentPos);
            hasMoved = true;
        }
        
        // 获取玩家当前的光源位置
        BlockPos currentLightPos = playerLightPositions.get(playerId);
        
        // 检查是否需要更新光源
        boolean shouldUpdateLight = false;
        
        if (currentLightPos == null) {
            // 没有光源位置，需要放置新光源
            shouldUpdateLight = true;
        } else if (hasMoved) {
            // 玩家移动了，需要更新光源位置
            shouldUpdateLight = true;
        } else if (player.level().getBlockState(currentLightPos).getBlock() != Blocks.LIGHT) {
            // 光源方块被破坏或替换了，需要重新放置
            shouldUpdateLight = true;
        }
        
        // 如果需要更新光源
        if (shouldUpdateLight) {
            // 清理之前的光源位置（如果存在且还是光源方块）
            if (currentLightPos != null && 
                player.level().getBlockState(currentLightPos).getBlock() == Blocks.LIGHT) {
                player.level().setBlock(currentLightPos, Blocks.AIR.defaultBlockState(), 3);
            }
            
            // 寻找新的合适位置放置光源
            BlockPos newLightPos = findSuitableLightPosition(player, currentPos);
            
            if (newLightPos != null) {
                // 放置新的光源方块（最大亮度15）
                BlockState lightState = Blocks.LIGHT.defaultBlockState()
                    .setValue(BlockStateProperties.LEVEL, 15);
                player.level().setBlock(newLightPos, lightState, 3);
                
                // 更新玩家光源位置
                playerLightPositions.put(playerId, newLightPos);

            }
        }
    }
    
    /**
     * 寻找适合放置光源的位置
     * 在玩家2格范围内寻找空气方块位置
     */
    private BlockPos findSuitableLightPosition(Player player, BlockPos playerPos) {
        // 优先检查玩家头顶、脚下、周围的空气位置
        BlockPos[] candidatePositions = {
            playerPos.above(),      // 玩家头顶
            playerPos.below(),      // 玩家脚下
            playerPos.north(),      // 玩家北边
            playerPos.south(),      // 玩家南边
            playerPos.east(),       // 玩家东边
            playerPos.west(),       // 玩家西边
            playerPos.above().north(),
            playerPos.above().south(),
            playerPos.above().east(),
            playerPos.above().west()
        };
        
        // 检查候选位置
        for (BlockPos candidate : candidatePositions) {
            if (canPlaceLight(player.level(), candidate)) {
                return candidate;
            }
        }
        
        // 如果直接候选位置都不合适，搜索2格范围内的所有位置
        for (int x = -LIGHT_RANGE; x <= LIGHT_RANGE; x++) {
            for (int y = -LIGHT_RANGE; y <= LIGHT_RANGE; y++) {
                for (int z = -LIGHT_RANGE; z <= LIGHT_RANGE; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    if (canPlaceLight(player.level(), checkPos)) {
                        return checkPos;
                    }
                }
            }
        }
        
        return null; // 没有找到合适位置
    }
    
    /**
     * 检查是否可以放置光源方块
     */
    private boolean canPlaceLight(net.minecraft.world.level.Level level, BlockPos pos) {
        // 检查位置是否可替换
        BlockState currentState = level.getBlockState(pos);
        return currentState.isAir();

    }
    
    /**
     * 清理玩家周围的光源方块
     * 只清理该玩家放置的光源方块
     */
    private void cleanupLightBlocks(Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        
        UUID playerId = player.getUUID();
        
        // 获取该玩家的光源位置
        BlockPos lightPos = playerLightPositions.get(playerId);
        
        if (lightPos != null) {
            // 检查该位置是否还是光源方块
            if (player.level().getBlockState(lightPos).getBlock() == Blocks.LIGHT) {
                // 移除光源方块
                player.level().setBlock(lightPos, Blocks.AIR.defaultBlockState(), 3);
            }
            
            // 从映射中移除玩家记录
            playerLightPositions.remove(playerId);
        }
    }
    
    /**
     * 移除玩家的发光效果
     */
    private void removeGlowingEffect(Player player) {
        if (player != null) {
            player.removeEffect(MobEffects.GLOWING);
        }
    }
    
    /**
     * 检查玩家是否拥有发光鱿鱼契约效果
     */
    public static boolean hasGlowSquidContract(Player player) {
        return player != null && glowSquidContractPlayers.contains(player.getUUID());
    }
    
    /**
     * 清理玩家发光鱿鱼契约的光源方块（静态方法）
     * 供事件处理器调用，防止玩家离开服务器时遗留光源方块
     */
    public static void cleanupPlayerLightBlocks(Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        
        UUID playerId = player.getUUID();
        
        // 获取该玩家的光源位置
        BlockPos lightPos = playerLightPositions.get(playerId);
        
        if (lightPos != null) {
            // 检查该位置是否还是光源方块
            if (player.level().getBlockState(lightPos).getBlock() == Blocks.LIGHT) {
                // 移除光源方块
                player.level().setBlock(lightPos, Blocks.AIR.defaultBlockState(), 3);
            }
            
            // 从映射中移除玩家记录
            playerLightPositions.remove(playerId);
            playerLastPositions.remove(playerId);
            glowSquidContractPlayers.remove(playerId);
        }
    }

    @Override
    protected long getTickInterval() {
        return 5;
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6发光鱿鱼契约效果："));
        details.add(Component.literal("§7- 持续获得发光效果"));
        details.add(Component.literal("§7- 玩家周围产生光源，照亮附近区域"));
        return details;
    }
}