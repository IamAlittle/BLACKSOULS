package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 蠹虫契约效果 - 秒破坏石头类方块
 * 玩家契约蠹虫后获得的能力：
 * 1. 能够快速破坏石头类方块（类似蠹虫的破坏能力）
 * 2. 破坏时产生蠹虫特有的粒子效果
 */
public class SilverfishContract extends ContractEffect {
    private static final String EFFECT_ID = "silverfish_stone_breaker";
    private static final String DISPLAY_NAME = "石头破坏者";
    private static final String DESCRIPTION = "秒破坏石头类方块";
    
    // 蠹虫契约玩家集合
    private static final Set<UUID> silverfishContractPlayers = new HashSet<>();
    
    // 可破坏的石头类方块列表（使用base_stone_overworld标签）
    private static final TagKey<Block> STONE_BLOCK_TAG = BlockTags.BASE_STONE_OVERWORLD;
    
    public SilverfishContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }

    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            silverfishContractPlayers.add(player.getUUID());
            
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
            silverfishContractPlayers.remove(player.getUUID());
            
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
        // 蠹虫契约不需要每tick处理，破坏逻辑在挖掘事件中处理
    }
    
    /**
     * 处理玩家挖掘方块事件（方块破坏完成后调用）
     * 这个方法需要在挖掘事件处理器中调用
     */
    public static boolean onPlayerBreakBlock(Player player, BlockPos pos, BlockState blockState) {
        if (player == null || pos == null || blockState == null || !hasSilverfishContract(player)) {
            return false; // 不处理
        }
        
        // 检查是否为石头类方块
        if (isStoneBlock(blockState.getBlock())) {
            // 快速破坏石头类方块
            return fastBreakStoneBlock(player, pos, blockState);
        }
        
        return false; // 不处理非石头类方块
    }
    
    /**
     * 处理玩家开始挖掘方块事件（玩家按左键时立即调用）
     * 这个方法需要在开始挖掘事件处理器中调用
     */
    public static boolean onPlayerStartBreakBlock(Player player, BlockPos pos, BlockState blockState) {
        if (player == null || pos == null || blockState == null || !hasSilverfishContract(player)) {
            return false; // 不处理
        }
        
        // 检查是否为石头类方块
        if (isStoneBlock(blockState.getBlock())) {
            // 立即破坏石头类方块
            return instantBreakStoneBlock(player, pos, blockState);
        }
        
        return false; // 不处理非石头类方块
    }
    
    /**
     * 快速破坏石头类方块
     */
    private static boolean fastBreakStoneBlock(Player player, BlockPos pos, BlockState blockState) {
        if (player.level() instanceof ServerLevel serverLevel) {
            // 检查玩家是否有权限破坏这个方块
            if (!player.mayInteract(serverLevel, pos)) {
                return false; // 玩家没有权限，跳过破坏
            }
            
            // 检查方块是否可以被破坏（非不可破坏方块）
            if (blockState.getDestroySpeed(serverLevel, pos) < 0) {
                return false; // 不可破坏的方块（如基岩），跳过破坏
            }
            
            // 立即破坏方块并产生掉落物
            boolean destroyed = serverLevel.destroyBlock(pos, true, player);
            
            if (destroyed) {
                // 显示蠹虫破坏粒子效果
                spawnSilverfishParticles(serverLevel, pos);
                
                // 播放蠹虫破坏音效
                serverLevel.playSound(null, pos, 
                    net.minecraft.sounds.SoundEvents.SILVERFISH_AMBIENT, 
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.0f);
                
                return true; // 成功破坏
            }
        }
        
        return false; // 破坏失败
    }
    
    /**
     * 立即破坏石头类方块（玩家按左键时立即调用）
     */
    private static boolean instantBreakStoneBlock(Player player, BlockPos pos, BlockState blockState) {
        if (player.level() instanceof ServerLevel serverLevel) {
            // 检查玩家是否有权限破坏这个方块
            if (!player.mayInteract(serverLevel, pos)) {
                return false; // 玩家没有权限，跳过破坏
            }
            
            // 检查方块是否可以被破坏（非不可破坏方块）
            if (blockState.getDestroySpeed(serverLevel, pos) < 0) {
                return false; // 不可破坏的方块（如基岩），跳过破坏
            }
            
            // 立即破坏方块并产生掉落物
            boolean destroyed = serverLevel.destroyBlock(pos, true, player);
            
            if (destroyed) {
                // 显示蠹虫破坏粒子效果
                spawnSilverfishParticles(serverLevel, pos);
                
                // 播放蠹虫破坏音效
                serverLevel.playSound(null, pos, 
                    net.minecraft.sounds.SoundEvents.SILVERFISH_AMBIENT, 
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.0f);
                
                return true; // 成功破坏，阻止正常的挖掘过程
            }
        }
        
        return false; // 破坏失败
    }
    
    /**
     * 检查方块是否为石头类方块
     */
    private static boolean isStoneBlock(Block block) {
        return block.defaultBlockState().is(STONE_BLOCK_TAG);
    }
    
    /**
     * 显示蠹虫破坏粒子效果
     */
    private static void spawnSilverfishParticles(ServerLevel serverLevel, BlockPos pos) {
        // 在破坏位置生成蠹虫粒子效果
        serverLevel.sendParticles(ParticleTypes.CRIT,
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            10, 0.3, 0.3, 0.3, 0.1);
        
        // 添加一些烟雾粒子效果
        serverLevel.sendParticles(ParticleTypes.SMOKE,
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            5, 0.2, 0.2, 0.2, 0.05);
    }
    
    /**
     * 检查玩家是否拥有蠹虫契约效果
     */
    public static boolean hasSilverfishContract(Player player) {
        return player != null && silverfishContractPlayers.contains(player.getUUID());
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6蠹虫契约效果："));
        details.add(Component.literal("§7石头破坏者"));
        details.add(Component.literal("§7- 能够秒破坏石头类方块"));
        details.add(Component.literal("§7- 包括石头、圆石、石砖、安山岩等"));
        details.add(Component.literal("§7- 破坏时产生蠹虫粒子效果"));
        details.add(Component.literal("§7- 获得蠹虫的快速挖掘能力"));
        return details;
    }
    
    @Override
    public CompoundTag saveToNBT() {
        return super.saveToNBT(); // 蠹虫契约不需要额外数据
    }
    
    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt); // 蠹虫契约不需要额外数据
    }
}