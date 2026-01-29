package com.iamalittle.black_souls_options.render;

import com.iamalittle.black_souls_options.contracts.effects.mobs.PiglinContract;
import com.iamalittle.black_souls_options.common.events.RenderWorldLastEvent;
import com.iamalittle.black_souls_options.common.Events;
import com.iamalittle.black_souls_options.render.LineRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import java.util.ArrayList;
import java.util.List;

/**
 * PiglinLoved物品高亮渲染器
 * 负责渲染附近带有piglin_loved标签的方块和物品的金色边框轮廓
 */
public class PiglinLovedHighlighter {
    private static final double MAX_DISTANCE = 24.0; // 最大检测距离（方块）- 减少距离以降低镶金黑石显示数量
    private static final float RED_COLOR = 1.0f;     // 金色边框的红色分量
    private static final float GREEN_COLOR = 0.84f;  // 金色边框的绿色分量
    private static final float BLUE_COLOR = 0.0f;    // 金色边框的蓝色分量
    private static final float ALPHA = 0.8f;         // 透明度
    private static final int CACHE_TICKS = 40;       // 缓存更新间隔（tick）- 比箱子检测稍长以优化性能
    
    private boolean enabled = false;
    private Player currentPlayer;
    private List<BlockPos> cachedPiglinLovedBlocks = new ArrayList<>();
    private int lastCacheUpdateTick = 0;
    private Vec3 lastPlayerPos = Vec3.ZERO;
    
    public PiglinLovedHighlighter() {
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
        
        // 检查玩家是否拥有猪灵契约
        if (!PiglinContract.hasPiglinContract(currentPlayer)) {
            return;
        }
        
        // 获取玩家位置
        Vec3 playerPos = currentPlayer.getEyePosition(event.getTickDelta());
        
        // 检查是否需要更新缓存
        if (shouldUpdateCache(mc, playerPos)) {
            cachedPiglinLovedBlocks = findNearbyPiglinLovedBlocks(mc, playerPos);
            lastCacheUpdateTick = mc.player.tickCount;
            lastPlayerPos = playerPos;
        }
        
        // 查找附近的piglin_loved掉落物（不缓存，实时检测）
        List<net.minecraft.world.entity.item.ItemEntity> piglinLovedItems = findNearbyPiglinLovedItems(mc, playerPos);
        
        // 渲染piglin_loved方块边框
        renderPiglinLovedOutlines(event, cachedPiglinLovedBlocks);
        
        // 渲染piglin_loved掉落物边框
        renderPiglinLovedItemOutlines(event, piglinLovedItems);
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
        if (moveDistance > 3.0) { // 如果移动超过3格，更新缓存
            return true;
        }
        
        return false;
    }
    
    /**
     * 查找附近的piglin_loved方块（优化版本）
     */
    private List<BlockPos> findNearbyPiglinLovedBlocks(Minecraft mc, Vec3 playerPos) {
        List<BlockPos> piglinLovedBlocks = new ArrayList<>();
        
        if (mc.level == null) {
            return piglinLovedBlocks;
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
                    
                    // 获取区块中的方块
                    var chunk = mc.level.getChunk(chunkX, chunkZ);
                    
                    // 遍历区块中的所有方块位置（优化性能，只检查可能包含piglin_loved方块的位置）
                    for (int x = chunkX << 4; x < (chunkX << 4) + 16; x++) {
                        for (int z = chunkZ << 4; z < (chunkZ << 4) + 16; z++) {
                            for (int y = mc.level.getMinBuildHeight(); y < mc.level.getMaxBuildHeight(); y++) {
                                BlockPos pos = new BlockPos(x, y, z);
                                
                                // 检查距离
                                double distance = playerPos.distanceTo(Vec3.atCenterOf(pos));
                                if (distance <= MAX_DISTANCE) {
                                    
                                    // 检查方块是否为piglin_loved
                                    if (isPiglinLovedBlock(mc, pos)) {
                                        piglinLovedBlocks.add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return piglinLovedBlocks;
    }
    
    /**
     * 检查方块是否为piglin_loved（优化版本）
     */
    private boolean isPiglinLovedBlock(Minecraft mc, BlockPos pos) {
        if (mc.level == null) {
            return false;
        }
        
        BlockState blockState = mc.level.getBlockState(pos);
        
        // 排除镶金黑石（gilded_blackstone），因为它太常见了
        if (blockState.is(Blocks.GILDED_BLACKSTONE)) {
            return false;
        }
        
        // 检查方块是否带有GUARDED_BY_PIGLINS标签（被猪灵守护的方块）
        if (blockState.is(BlockTags.GUARDED_BY_PIGLINS)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 查找附近的piglin_loved掉落物
     */
    private List<net.minecraft.world.entity.item.ItemEntity> findNearbyPiglinLovedItems(Minecraft mc, Vec3 playerPos) {
        List<net.minecraft.world.entity.item.ItemEntity> piglinLovedItems = new ArrayList<>();
        
        if (mc.level == null) {
            return piglinLovedItems;
        }
        
        // 获取玩家周围的掉落物实体
        List<net.minecraft.world.entity.item.ItemEntity> itemEntities = mc.level.getEntitiesOfClass(
            net.minecraft.world.entity.item.ItemEntity.class,
            new AABB(
                playerPos.x - MAX_DISTANCE, playerPos.y - MAX_DISTANCE, playerPos.z - MAX_DISTANCE,
                playerPos.x + MAX_DISTANCE, playerPos.y + MAX_DISTANCE, playerPos.z + MAX_DISTANCE
            )
        );
        
        // 检查每个掉落物是否带有PIGLIN_LOVED标签
        for (net.minecraft.world.entity.item.ItemEntity itemEntity : itemEntities) {
            net.minecraft.world.item.ItemStack itemStack = itemEntity.getItem();
            if (!itemStack.isEmpty() && itemStack.is(ItemTags.PIGLIN_LOVED)) {
                piglinLovedItems.add(itemEntity);
            }
        }
        
        return piglinLovedItems;
    }
    
    /**
     * 渲染piglin_loved方块边框轮廓
     */
    private void renderPiglinLovedOutlines(RenderWorldLastEvent event, List<BlockPos> blockPositions) {
        if (blockPositions.isEmpty()) {
            return;
        }
        
        // 开始渲染线条
        LineRenderer.instance.begin(event, false); // 禁用深度测试，实现穿墙可见
        
        for (BlockPos pos : blockPositions) {
            // 计算方块的边界框
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
    
    /**
     * 渲染piglin_loved掉落物边框轮廓
     */
    private void renderPiglinLovedItemOutlines(RenderWorldLastEvent event, List<net.minecraft.world.entity.item.ItemEntity> itemEntities) {
        if (itemEntities.isEmpty()) {
            return;
        }
        
        // 开始渲染线条
        LineRenderer.instance.begin(event, false); // 禁用深度测试，实现穿墙可见
        
        for (net.minecraft.world.entity.item.ItemEntity itemEntity : itemEntities) {
            // 使用掉落物实体的精确位置，创建一个小型边框
            double x = itemEntity.getX();
            double y = itemEntity.getY();
            double z = itemEntity.getZ();
            
            // 掉落物通常很小，使用0.2x0.2x0.2的边框
            double size = 0.2;
            AABB boundingBox = new AABB(
                x - size, y - size, z - size,
                x + size, y + size, z + size
            );
            
            // 渲染边框
            renderBoundingBox(boundingBox);
        }
        
        // 结束渲染
        LineRenderer.instance.end();
    }
}