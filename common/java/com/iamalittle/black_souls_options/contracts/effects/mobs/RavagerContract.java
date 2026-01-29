package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import java.util.*;

/**
 * 劫掠兽契约效果 - 破坏沿途的树叶和农作物
 * 玩家契约劫掠兽后获得的能力：
 * 1. 破坏玩家周围一格范围内的树叶和农作物方块
 * 2. 产生破坏粒子效果
 */
public class RavagerContract extends ContractEffect {
    private static final String EFFECT_ID = "ravager_destroy_plants";
    private static final String DISPLAY_NAME = "破坏者";
    private static final String DESCRIPTION = "破坏沿途的树叶和农作物";
    
    // 破坏范围（以玩家为中心的正方体边长）
    private static final int DESTROY_RANGE = 3; // 3格范围（玩家为中心，左右各1格）
    
    // 劫掠兽契约玩家集合
    private static final Set<UUID> ravagerContractPlayers = new HashSet<>();
    
    // 记录上次破坏的方块位置，避免重复破坏
    private static final Map<UUID, Set<BlockPos>> lastDestroyedPositions = new HashMap<>();
    
    // 可破坏的树叶方块列表
    private static final Set<Block> LEAVES_BLOCKS = Set.of(
        Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES,
        Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.CHERRY_LEAVES,
        Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES
    );
    
    // 可破坏的农作物方块列表
    private static final Set<Block> CROP_BLOCKS = Set.of(
        Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS,
        Blocks.MELON_STEM, Blocks.PUMPKIN_STEM, Blocks.NETHER_WART,
        Blocks.SWEET_BERRY_BUSH, Blocks.COCOA
    );
    
    public RavagerContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            ravagerContractPlayers.add(player.getUUID());
            lastDestroyedPositions.put(player.getUUID(), new HashSet<>());
            
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
            ravagerContractPlayers.remove(player.getUUID());
            lastDestroyedPositions.remove(player.getUUID());
            
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
        if (player == null || !player.isAlive() || player.level() == null) {
            return;
        }
        
        // 破坏玩家周围的树叶和农作物
        destroyPlantsAroundPlayer(player);
    }
    
    /**
     * 破坏玩家周围的树叶和农作物
     */
    private void destroyPlantsAroundPlayer(Player player) {
        BlockPos playerPos = player.blockPosition();
        UUID playerUUID = player.getUUID();
        Set<BlockPos> currentDestroyedPositions = new HashSet<>();
        
        // 获取玩家周围的范围
        int range = DESTROY_RANGE / 2;
        
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    
                    // 检查是否为可破坏的树叶或农作物方块
                    BlockState blockState = player.level().getBlockState(checkPos);
                    if (isDestructiblePlant(blockState)) {
                        // 破坏方块并产生掉落物
                        destroyBlockWithDrops(player, checkPos, blockState);
                        currentDestroyedPositions.add(checkPos);
                        
                        // 显示破坏粒子效果
                        spawnDestructionParticles(player, checkPos);
                    }
                }
            }
        }
        
        // 更新破坏记录
        lastDestroyedPositions.put(playerUUID, currentDestroyedPositions);
    }
    
    /**
     * 破坏方块并产生掉落物
     * 使用玩家的权限进行破坏，确保被视为玩家操作
     */
    private void destroyBlockWithDrops(Player player, BlockPos pos, BlockState blockState) {
        if (player.level() instanceof ServerLevel serverLevel) {
            // 检查玩家是否有权限破坏这个方块
            if (!player.mayInteract(serverLevel, pos)) {
                return; // 玩家没有权限，跳过破坏
            }
            
            // 检查方块是否可以被破坏（非不可破坏方块）
            if (blockState.getDestroySpeed(serverLevel, pos) < 0) {
                return; // 不可破坏的方块（如基岩），跳过破坏
            }
            
            // 使用玩家的权限破坏方块并产生掉落物
            // 第三个参数为true表示产生掉落物，第四个参数为player表示使用玩家权限
            boolean destroyed = serverLevel.destroyBlock(pos, true, player);
            
            // 如果方块没有被完全破坏（比如有掉落物保护），强制设置为空气
            if (!destroyed && !serverLevel.getBlockState(pos).isAir()) {
                serverLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
    }
    
    /**
     * 检查方块是否为可破坏的树叶或农作物
     */
    private boolean isDestructiblePlant(BlockState blockState) {
        Block block = blockState.getBlock();
        return LEAVES_BLOCKS.contains(block) || CROP_BLOCKS.contains(block);
    }
    
    /**
     * 在破坏位置生成粒子效果
     */
    private void spawnDestructionParticles(Player player, BlockPos pos) {
        if (player.level() instanceof ServerLevel serverLevel) {
            // 生成破坏粒子效果
            serverLevel.sendParticles(ParticleTypes.POOF,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                5, 0.3, 0.3, 0.3, 0.1);
        }
    }
    
    /**
     * 检查玩家是否拥有劫掠兽契约效果
     */
    public static boolean hasRavagerContract(Player player) {
        return player != null && ravagerContractPlayers.contains(player.getUUID());
    }
    
    @Override
    protected long getTickInterval() {
        return 100; // 每1秒检查一次
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6劫掠兽契约效果："));
        details.add(Component.literal("§7- 破坏玩家周围" + DESTROY_RANGE + "格范围内的树叶和农作物"));
        return details;
    }
}