package com.iamalittle.black_souls_options.render;

import com.iamalittle.black_souls_options.contracts.effects.mobs.WardenContract;
import com.iamalittle.black_souls_options.common.events.RenderWorldLastEvent;
import com.iamalittle.black_souls_options.common.Events;
import com.iamalittle.black_souls_options.render.LineRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;

/**
 * 监守者方块高亮渲染器
 * 负责渲染玩家能看到的方块边框，实现声纳扫描效果
 */
public class WardenBlockHighlighter {
    private static final double MIN_DISTANCE = 3.0; // 最小检测距离（方块）
    private static final double MAX_DISTANCE = 25.0; // 最大检测距离（方块）
    private static final double FADE_START_DISTANCE = 15.0; // 透明度渐变开始距离（方块）
    private static final float DARK_GREEN_RED = 0.0f;   // 深绿色边框的红色分量
    private static final float DARK_GREEN_GREEN = 0.5f; // 深绿色边框的绿色分量
    private static final float DARK_GREEN_BLUE = 0.0f;  // 深绿色边框的蓝色分量
    private static final float MAX_ALPHA = 1.0f;        // 最大透明度
    private static final int SCAN_DURATION_TICKS = 100;  // 扫描持续时间（游戏刻，约4秒）- 放慢扫描速度
    private static final int HOLD_DURATION_TICKS = 1000; // 保持常亮时间（游戏刻，约5秒）
    private static final int FADE_DURATION_TICKS = 120;  // 暗淡时间
    private static final int TOTAL_CYCLE_TICKS = SCAN_DURATION_TICKS + HOLD_DURATION_TICKS + FADE_DURATION_TICKS;
    
    private boolean enabled = false;
    private Player currentPlayer;
    private int scanTick = 0; // 声纳扫描计时器
    
    public WardenBlockHighlighter() {
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
        
        // 检查玩家是否拥有监守者契约
        if (!WardenContract.hasWardenContract(currentPlayer)) {
            return;
        }
        
        // 更新声纳扫描计时器
        scanTick++;
        if (scanTick >= TOTAL_CYCLE_TICKS) {
            scanTick = 0;
        }
        
        // 获取玩家位置和视线方向
        Vec3 playerPos = currentPlayer.getEyePosition(event.getTickDelta());
        Vec3 lookVec = currentPlayer.getLookAngle();
        
        // 查找范围内的方块
        List<BlockPos> visibleBlocks = findVisibleBlocksInRange(mc, playerPos, lookVec);
        
        // 渲染可见方块的边框（声纳扫描效果）
        renderSonarBlockOutlines(event, visibleBlocks, playerPos);
    }
    
    /**
     * 查找玩家能看到的范围内的方块
     */
    private List<BlockPos> findVisibleBlocksInRange(Minecraft mc, Vec3 playerPos, Vec3 lookVec) {
        List<BlockPos> visibleBlocks = new ArrayList<>();
        
        if (mc.level == null) {
            return visibleBlocks;
        }
        
        // 计算搜索范围
        int minX = (int) (playerPos.x - MAX_DISTANCE);
        int maxX = (int) (playerPos.x + MAX_DISTANCE);
        int minY = (int) (playerPos.y - MAX_DISTANCE);
        int maxY = (int) (playerPos.y + MAX_DISTANCE);
        int minZ = (int) (playerPos.z - MAX_DISTANCE);
        int maxZ = (int) (playerPos.z + MAX_DISTANCE);
        
        // 遍历范围内的所有方块
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    
                    // 检查距离
                    double distance = playerPos.distanceTo(Vec3.atCenterOf(pos));
                    if (distance < MIN_DISTANCE || distance > MAX_DISTANCE) {
                        continue;
                    }
                    
                    // 检查方块是否可见
                    if (isBlockVisibleToPlayer(mc, pos, playerPos, lookVec)) {
                        visibleBlocks.add(pos);
                    }
                }
            }
        }
        
        return visibleBlocks;
    }
    
    /**
     * 检查方块是否对玩家可见
     */
    private boolean isBlockVisibleToPlayer(Minecraft mc, BlockPos pos, Vec3 playerPos, Vec3 lookVec) {
        if (mc.level == null) {
            return false;
        }
        
        BlockState blockState = mc.level.getBlockState(pos);
        
        // 跳过空气方块
        if (blockState.isAir()) {
            return false;
        }
        
        // 跳过可替换方块（如草、花、蕨等）
        if (blockState.canBeReplaced()) {
            return false;
        }
        
        // 检查方块是否在玩家视线范围内
        Vec3 blockCenter = Vec3.atCenterOf(pos);
        Vec3 toBlock = blockCenter.subtract(playerPos);
        
        // 计算视线方向与方块方向的夹角
        double dotProduct = lookVec.dot(toBlock.normalize());
        
        // 放宽条件：如果夹角大于75度（cos(75°) = 0.2588），则认为方块在视野内
        if (dotProduct < 0.25) {
            return false;
        }
        
        // 放宽条件：简化可见面检测，只要方块在玩家视野内且不被其他方块完全遮挡就显示
        return isBlockNotFullyOccluded(mc, pos, playerPos);
    }
    
    /**
     * 检查方块是否有面向玩家的可见面
     */
    private boolean hasVisibleFaceToPlayer(Minecraft mc, BlockPos pos, Vec3 playerPos) {
        if (mc.level == null) {
            return false;
        }
        
        BlockState blockState = mc.level.getBlockState(pos);
        VoxelShape shape = blockState.getShape(mc.level, pos);
        
        // 如果方块没有碰撞箱，则认为不可见
        if (shape.isEmpty()) {
            return false;
        }
        
        // 检查六个方向的面是否可见
        for (Direction direction : Direction.values()) {
            if (isFaceVisible(mc, pos, direction, playerPos)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查方块是否未被完全遮挡（简化版）
     */
    private boolean isBlockNotFullyOccluded(Minecraft mc, BlockPos pos, Vec3 playerPos) {
        if (mc.level == null) {
            return false;
        }
        
        BlockState blockState = mc.level.getBlockState(pos);
        VoxelShape shape = blockState.getShape(mc.level, pos);
        
        // 如果方块没有碰撞箱，则认为不可见
        if (shape.isEmpty()) {
            return false;
        }
        
        // 简化检测：只要方块至少有一个面没有被固体方块完全遮挡就显示
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockState adjacentState = mc.level.getBlockState(adjacentPos);
            
            // 如果相邻方块是透明或空气，则这个面可见
            if (adjacentState.isAir() || !adjacentState.canOcclude()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查特定方向的面是否可见
     */
    private boolean isFaceVisible(Minecraft mc, BlockPos pos, Direction direction, Vec3 playerPos) {
        if (mc.level == null) {
            return false;
        }
        
        // 检查相邻方块是否遮挡
        BlockPos adjacentPos = pos.relative(direction);
        BlockState adjacentState = mc.level.getBlockState(adjacentPos);
        
        // 如果相邻方块是透明或空气，则这个面可见
        if (adjacentState.isAir() || !adjacentState.canOcclude()) {
            // 进一步检查玩家是否能"看到"这个面
            Vec3 faceCenter = getFaceCenter(pos, direction);
            Vec3 toFace = faceCenter.subtract(playerPos);
            
            // 放宽条件：简化视线检测，减少采样次数
            if (!isLineOfSightBlockedSimple(mc, playerPos, faceCenter)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取方块特定面的中心点
     */
    private Vec3 getFaceCenter(BlockPos pos, Direction direction) {
        double x = pos.getX() + 0.5 + direction.getStepX() * 0.5;
        double y = pos.getY() + 0.5 + direction.getStepY() * 0.5;
        double z = pos.getZ() + 0.5 + direction.getStepZ() * 0.5;
        return new Vec3(x, y, z);
    }
    
    /**
     * 简化版视线检测
     */
    private boolean isLineOfSightBlocked(Minecraft mc, Vec3 start, Vec3 end) {
        if (mc.level == null) {
            return false;
        }
        
        // 使用简单的射线检测（简化版）
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        direction = direction.normalize();
        
        // 沿着射线采样几个点
        int samples = (int) (distance * 2); // 每0.5格采样一次
        for (int i = 1; i < samples; i++) {
            double t = (double) i / samples;
            Vec3 samplePoint = start.add(direction.scale(distance * t));
            BlockPos samplePos = new BlockPos((int) samplePoint.x, (int) samplePoint.y, (int) samplePoint.z);
            
            // 跳过起点和终点的方块
            if (samplePoint.distanceTo(start) < 0.5 || samplePoint.distanceTo(end) < 0.5) {
                continue;
            }
            
            BlockState sampleState = mc.level.getBlockState(samplePos);
            if (!sampleState.isAir() && sampleState.canOcclude()) {
                return true; // 视线被阻挡
            }
        }
        
        return false;
    }
    
    /**
     * 更简化的视线检测（放宽条件）
     */
    private boolean isLineOfSightBlockedSimple(Minecraft mc, Vec3 start, Vec3 end) {
        if (mc.level == null) {
            return false;
        }
        
        // 使用更简化的射线检测
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        direction = direction.normalize();
        
        // 减少采样次数：每1格采样一次
        int samples = (int) distance;
        for (int i = 1; i < samples; i++) {
            double t = (double) i / samples;
            Vec3 samplePoint = start.add(direction.scale(distance * t));
            BlockPos samplePos = new BlockPos((int) samplePoint.x, (int) samplePoint.y, (int) samplePoint.z);
            
            // 跳过起点和终点的方块
            if (samplePoint.distanceTo(start) < 1.0 || samplePoint.distanceTo(end) < 1.0) {
                continue;
            }
            
            BlockState sampleState = mc.level.getBlockState(samplePos);
            // 放宽条件：只检测完全阻挡视线的固体方块
            if (!sampleState.isAir() && sampleState.canOcclude() && !sampleState.canBeReplaced()) {
                return true; // 视线被阻挡
            }
        }
        
        return false;
    }
    
    /**
     * 渲染可见方块的边框（声纳扫描效果）
     */
    private void renderSonarBlockOutlines(RenderWorldLastEvent event, List<BlockPos> blockPositions, Vec3 playerPos) {
        if (blockPositions.isEmpty()) {
            return;
        }
        
        // 开始渲染线条，启用深度测试（只渲染可见面）
        LineRenderer.instance.begin(event, true);
        
        // 计算当前阶段的透明度
        float baseAlpha = calculateThreeStageAlpha();
        
        // 如果透明度足够高，则渲染可见方块的边框
        if (baseAlpha > 0.05f) {
            for (BlockPos pos : blockPositions) {
                // 计算方块到玩家的距离
                double distance = Math.sqrt(pos.distSqr(new BlockPos((int)playerPos.x, (int)playerPos.y, (int)playerPos.z)));
                
                // 在扫描阶段，根据距离和扫描进度计算透明度
                float alpha = calculateScanAlpha(distance, baseAlpha);
                
                // 如果透明度足够高，则渲染这个方块的边框
                if (alpha > 0.05f) {
                    renderSonarFacesOutline(event, pos, playerPos, alpha);
                }
            }
        }
        
        // 结束渲染
        LineRenderer.instance.end();
    }
    
    /**
     * 计算三阶段效果的透明度
     * 阶段1：扫描阶段（4秒）- 从内到外显示边框
     * 阶段2：保持阶段（5秒）- 所有边框保持常亮
     * 阶段3：暗淡阶段（2秒）- 边框稍微暗淡，但仍保持可见
     */
    private float calculateThreeStageAlpha() {
        if (scanTick < SCAN_DURATION_TICKS) {
            // 阶段1：扫描阶段 - 从内到外显示边框
            float scanProgress = (float) scanTick / SCAN_DURATION_TICKS;
            // 使用正弦函数创建平滑的淡入效果
            return Mth.sin(scanProgress * (float)Math.PI * 0.5f) * MAX_ALPHA;
        } else if (scanTick < SCAN_DURATION_TICKS + HOLD_DURATION_TICKS) {
            // 阶段2：保持阶段 - 所有边框保持常亮
            return MAX_ALPHA;
        } else {
            // 阶段3：暗淡阶段 - 边框稍微暗淡，但仍保持可见
            float fadeProgress = (float)(scanTick - SCAN_DURATION_TICKS - HOLD_DURATION_TICKS) / FADE_DURATION_TICKS;
            // 使用线性插值，从MAX_ALPHA到0.3f（保持30%的可见度）
            return MAX_ALPHA * (1.0f - fadeProgress * 0.7f);
        }
    }
    
    /**
     * 计算扫描阶段的透明度（从玩家位置向外扩散）
     */
    private float calculateScanAlpha(double distance, float baseAlpha) {
        // 扫描阶段：根据距离和扫描进度计算透明度
        float scanProgress = (float) scanTick / SCAN_DURATION_TICKS;
        
        // 计算扫描波当前到达的距离（从MIN_DISTANCE到MAX_DISTANCE）
        float currentScanDistance = (float)MIN_DISTANCE + scanProgress * (float)(MAX_DISTANCE - MIN_DISTANCE);
        
        // 检查方块是否应该显示
        boolean shouldShowBlock = false;
        
        if (scanTick < SCAN_DURATION_TICKS) {
            // 扫描阶段：只有当方块在扫描波前方时才显示
            shouldShowBlock = (distance <= currentScanDistance);
        } else {
            // 非扫描阶段（保持和暗淡阶段）：所有在扫描范围内的方块都显示
            shouldShowBlock = (distance <= MAX_DISTANCE);
        }
        
        if (shouldShowBlock) {
            // 对于15-25格的方块，根据距离逐渐降低透明度（在所有阶段都生效）
            if (distance > FADE_START_DISTANCE) {
                // 计算距离渐变因子（15格时透明度为1，25格时透明度为0）
                float fadeFactor = 1.0f - (float)(distance - FADE_START_DISTANCE) / (float)(MAX_DISTANCE - FADE_START_DISTANCE);
                fadeFactor = Mth.clamp(fadeFactor, 0.0f, 1.0f);
                return baseAlpha * fadeFactor;
            }
            return baseAlpha; // 15格以内的方块显示完整透明度
        }
        
        return 0.0f; // 不应该显示的方块不显示
    }
    
    /**
     * 渲染方块的可见面边框（声纳扫描版本）
     */
    private void renderSonarFacesOutline(RenderWorldLastEvent event, BlockPos pos, Vec3 playerPos, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        
        BlockState blockState = mc.level.getBlockState(pos);
        AABB boundingBox = blockState.getShape(mc.level, pos).bounds().move(pos);
        
        // 放宽条件：渲染所有未被完全遮挡的面
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockState adjacentState = mc.level.getBlockState(adjacentPos);
            
            // 如果相邻方块是透明或空气，则渲染这个面
            if (adjacentState.isAir() || !adjacentState.canOcclude()) {
                // 渲染这个面的边框（使用动态透明度）
                renderSonarFaceOutline(boundingBox, direction, alpha);
            }
        }
    }
    
    /**
     * 渲染单个面的边框（声纳扫描版本）
     */
    private void renderSonarFaceOutline(AABB box, Direction direction, float alpha) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;
        
        switch (direction) {
            case DOWN:
                // 渲染底面
                LineRenderer.instance.line(minX, minY, minZ, maxX, minY, minZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                LineRenderer.instance.line(maxX, minY, minZ, maxX, minY, maxZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                LineRenderer.instance.line(maxX, minY, maxZ, minX, minY, maxZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                LineRenderer.instance.line(minX, minY, maxZ, minX, minY, minZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                break;
                
            case UP:
                // 渲染顶面
                LineRenderer.instance.line(minX, maxY, minZ, maxX, maxY, minZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                LineRenderer.instance.line(maxX, maxY, minZ, maxX, maxY, maxZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                LineRenderer.instance.line(maxX, maxY, maxZ, minX, maxY, maxZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                LineRenderer.instance.line(minX, maxY, maxZ, minX, maxY, minZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                break;
                
            case NORTH:
                // 渲染北面（-Z方向）
                LineRenderer.instance.line(minX, minY, minZ, maxX, minY, minZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                LineRenderer.instance.line(maxX, minY, minZ, maxX, maxY, minZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                LineRenderer.instance.line(maxX, maxY, minZ, minX, maxY, minZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                LineRenderer.instance.line(minX, maxY, minZ, minX, minY, minZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                break;
                
            case SOUTH:
                // 渲染南面（+Z方向）
                LineRenderer.instance.line(minX, minY, maxZ, maxX, minY, maxZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                LineRenderer.instance.line(maxX, minY, maxZ, maxX, maxY, maxZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                LineRenderer.instance.line(maxX, maxY, maxZ, minX, maxY, maxZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                LineRenderer.instance.line(minX, maxY, maxZ, minX, minY, maxZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                break;
                
            case WEST:
                // 渲染西面（-X方向）
                LineRenderer.instance.line(minX, minY, minZ, minX, minY, maxZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                LineRenderer.instance.line(minX, minY, maxZ, minX, maxY, maxZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                LineRenderer.instance.line(minX, maxY, maxZ, minX, maxY, minZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                LineRenderer.instance.line(minX, maxY, minZ, minX, minY, minZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                break;
                
            case EAST:
                // 渲染东面（+X方向）
                LineRenderer.instance.line(maxX, minY, minZ, maxX, minY, maxZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                LineRenderer.instance.line(maxX, minY, maxZ, maxX, maxY, maxZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                LineRenderer.instance.line(maxX, maxY, maxZ, maxX, maxY, minZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                LineRenderer.instance.line(maxX, maxY, minZ, maxX, minY, minZ, DARK_GREEN_RED, DARK_GREEN_GREEN, DARK_GREEN_BLUE, alpha);
                break;
        }
    }
    

}