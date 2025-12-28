package com.iamalittle.black_souls_options.render;

import com.iamalittle.black_souls_options.contracts.effects.mobs.DolphinContract;
import com.iamalittle.black_souls_options.common.events.RenderWorldLastEvent;
import com.iamalittle.black_souls_options.common.Events;
import com.iamalittle.black_souls_options.render.LineRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/**
 * 箱子高亮渲染器
 * 负责渲染附近容器的红色边框轮廓
 */
public class ChestHighlighter {
    private static final double MAX_DISTANCE = 128.0; // 最大检测距离（方块）
    private static final float RED_COLOR = 1.0f;     // 红色分量
    private static final float GREEN_COLOR = 0.0f;   // 绿色分量
    private static final float BLUE_COLOR = 0.0f;    // 蓝色分量
    private static final float ALPHA = 1.0f;         // 透明度
    private static final float LINE_WIDTH = 2.0f;    // 线宽
    private static final int CACHE_TICKS = 20;       // 缓存更新间隔（tick）
    
    private boolean enabled = false;
    private Player currentPlayer;
    private List<BlockPos> cachedContainers = new ArrayList<>();
    private int lastCacheUpdateTick = 0;
    private Vec3 lastPlayerPos = Vec3.ZERO;
    
    public ChestHighlighter() {
        // 注册世界渲染事件
        Events.RenderWorldLast.add(this::onRenderWorldLast);
    }
    
    /**
     * 设置高亮是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * 更新高亮渲染器状态
     */
    public void update(Player player) {
        this.currentPlayer = player;
    }
    
    /**
     * 世界渲染事件处理方法
     */
    private void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!enabled) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        
        currentPlayer = mc.player;
        
        // 检查玩家是否拥有海豚契约
        if (!DolphinContract.hasDolphinContract(currentPlayer)) {
            return;
        }
        
        // 获取玩家位置
        Vec3 playerPos = currentPlayer.getEyePosition(event.getTickDelta());
        
        // 检查是否需要更新缓存
        if (shouldUpdateCache(mc, playerPos)) {
            cachedContainers = findNearbyContainers(mc, playerPos);
            lastCacheUpdateTick = mc.player.tickCount;
            lastPlayerPos = playerPos;
        }
        
        // 渲染容器边框
        renderContainerOutlines(event, cachedContainers);
    }
    
    /**
     * 检查是否需要更新缓存
     */
    private boolean shouldUpdateCache(Minecraft mc, Vec3 currentPlayerPos) {
        // 检查tick间隔
        if (mc.player.tickCount - lastCacheUpdateTick >= CACHE_TICKS) {
            return true;
        }
        
        // 检查玩家移动距离
        double moveDistance = currentPlayerPos.distanceTo(lastPlayerPos);
        if (moveDistance > 2.0) { // 如果移动超过2格，更新缓存
            return true;
        }
        
        return false;
    }
    
    /**
     * 查找附近的容器方块（优化版本）
     */
    private List<BlockPos> findNearbyContainers(Minecraft mc, Vec3 playerPos) {
        List<BlockPos> containers = new ArrayList<>();
        
        if (mc.level == null) {
            return containers;
        }
        
        // 使用区块加载器获取已加载的区块
        int chunkRadius = (int) Math.ceil(MAX_DISTANCE / 16.0);
        BlockPos playerBlockPos = new BlockPos(
            (int) playerPos.x,
            (int) playerPos.y,
            (int) playerPos.z
        );
        
        // 获取玩家所在区块坐标
        int playerChunkX = playerBlockPos.getX() >> 4;
        int playerChunkZ = playerBlockPos.getZ() >> 4;
        
        // 遍历附近的已加载区块
        for (int chunkX = playerChunkX - chunkRadius; chunkX <= playerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = playerChunkZ - chunkRadius; chunkZ <= playerChunkZ + chunkRadius; chunkZ++) {
                
                // 检查区块是否已加载
                if (mc.level.hasChunk(chunkX, chunkZ)) {
                    
                    // 获取区块中的方块实体（容器通常是方块实体）
                    mc.level.getChunk(chunkX, chunkZ).getBlockEntities().forEach((pos, blockEntity) -> {
                        
                        // 检查距离
                        double distance = playerPos.distanceTo(Vec3.atCenterOf(pos));
                        if (distance <= MAX_DISTANCE) {
                            
                            // 检查方块是否为容器
                            if (isContainerBlock(mc, pos)) {
                                containers.add(pos);
                            }
                        }
                    });
                }
            }
        }
        
        return containers;
    }
    
    /**
     * 检查方块是否为容器（优化版本）
     */
    private boolean isContainerBlock(Minecraft mc, BlockPos pos) {
        // 首先检查方块实体类型，这是最快速的方法
        BlockEntity blockEntity = mc.level.getBlockEntity(pos);
        if (blockEntity != null) {
            // 常见的容器方块实体类型
            return blockEntity instanceof ChestBlockEntity ||
                   blockEntity instanceof BarrelBlockEntity ||
                   blockEntity instanceof ShulkerBoxBlockEntity ||
                   blockEntity instanceof EnderChestBlockEntity ||
                   blockEntity instanceof DispenserBlockEntity ||
                   blockEntity instanceof DropperBlockEntity ||
                   blockEntity instanceof HopperBlockEntity ||
                   blockEntity instanceof AbstractFurnaceBlockEntity;
        }
        
        // 如果方块实体不存在，再检查方块类型（作为备用）
        Block block = mc.level.getBlockState(pos).getBlock();
        
        return block == Blocks.CHEST ||
               block == Blocks.TRAPPED_CHEST ||
               block == Blocks.BARREL ||
               block == Blocks.SHULKER_BOX ||
               block == Blocks.WHITE_SHULKER_BOX ||
               block == Blocks.ORANGE_SHULKER_BOX ||
               block == Blocks.MAGENTA_SHULKER_BOX ||
               block == Blocks.LIGHT_BLUE_SHULKER_BOX ||
               block == Blocks.YELLOW_SHULKER_BOX ||
               block == Blocks.LIME_SHULKER_BOX ||
               block == Blocks.PINK_SHULKER_BOX ||
               block == Blocks.GRAY_SHULKER_BOX ||
               block == Blocks.LIGHT_GRAY_SHULKER_BOX ||
               block == Blocks.CYAN_SHULKER_BOX ||
               block == Blocks.PURPLE_SHULKER_BOX ||
               block == Blocks.BLUE_SHULKER_BOX ||
               block == Blocks.BROWN_SHULKER_BOX ||
               block == Blocks.GREEN_SHULKER_BOX ||
               block == Blocks.RED_SHULKER_BOX ||
               block == Blocks.BLACK_SHULKER_BOX ||
               block == Blocks.ENDER_CHEST ||
               block == Blocks.DISPENSER ||
               block == Blocks.DROPPER ||
               block == Blocks.HOPPER;
    }
    
    /**
     * 渲染容器边框轮廓
     */
    private void renderContainerOutlines(RenderWorldLastEvent event, List<BlockPos> containerPositions) {
        if (containerPositions.isEmpty()) {
            return;
        }
        
        // 开始渲染线条
        LineRenderer.instance.begin(event, false); // 禁用深度测试，实现穿墙可见
        
        for (BlockPos pos : containerPositions) {
            // 计算容器的边界框
            AABB boundingBox = new AABB(pos);
            
            // 渲染边框
            renderBoundingBox(boundingBox);
        }
        
        // 结束渲染
        LineRenderer.instance.end();
    }
    
    /**
     * 渲染边界框
     */
    private void renderBoundingBox(AABB box) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;
        
        // 渲染底部四边形
        LineRenderer.instance.line(minX, minY, minZ, maxX, minY, minZ, RED_COLOR, GREEN_COLOR, BLUE_COLOR, ALPHA);
        LineRenderer.instance.line(maxX, minY, minZ, maxX, minY, maxZ, RED_COLOR, GREEN_COLOR, BLUE_COLOR, ALPHA);
        LineRenderer.instance.line(maxX, minY, maxZ, minX, minY, maxZ, RED_COLOR, GREEN_COLOR, BLUE_COLOR, ALPHA);
        LineRenderer.instance.line(minX, minY, maxZ, minX, minY, minZ, RED_COLOR, GREEN_COLOR, BLUE_COLOR, ALPHA);
        
        // 渲染顶部四边形
        LineRenderer.instance.line(minX, maxY, minZ, maxX, maxY, minZ, RED_COLOR, GREEN_COLOR, BLUE_COLOR, ALPHA);
        LineRenderer.instance.line(maxX, maxY, minZ, maxX, maxY, maxZ, RED_COLOR, GREEN_COLOR, BLUE_COLOR, ALPHA);
        LineRenderer.instance.line(maxX, maxY, maxZ, minX, maxY, maxZ, RED_COLOR, GREEN_COLOR, BLUE_COLOR, ALPHA);
        LineRenderer.instance.line(minX, maxY, maxZ, minX, maxY, minZ, RED_COLOR, GREEN_COLOR, BLUE_COLOR, ALPHA);
        
        // 渲染垂直边
        LineRenderer.instance.line(minX, minY, minZ, minX, maxY, minZ, RED_COLOR, GREEN_COLOR, BLUE_COLOR, ALPHA);
        LineRenderer.instance.line(maxX, minY, minZ, maxX, maxY, minZ, RED_COLOR, GREEN_COLOR, BLUE_COLOR, ALPHA);
        LineRenderer.instance.line(maxX, minY, maxZ, maxX, maxY, maxZ, RED_COLOR, GREEN_COLOR, BLUE_COLOR, ALPHA);
        LineRenderer.instance.line(minX, minY, maxZ, minX, maxY, maxZ, RED_COLOR, GREEN_COLOR, BLUE_COLOR, ALPHA);
    }
}