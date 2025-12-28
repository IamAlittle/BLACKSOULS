package com.iamalittle.black_souls_options.render;

import com.iamalittle.black_souls_options.common.Events;
import com.iamalittle.black_souls_options.common.events.RenderWorldLastEvent;
import com.iamalittle.black_souls_options.contracts.Contract;
import com.iamalittle.black_souls_options.contracts.GlobalContractManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 契约追踪器渲染器，负责绘制从玩家到追踪目标的红线
 */
public class ContractTrackerRenderer {
    private static final float RED_LINE_RED = 1.0f;
    private static final float RED_LINE_GREEN = 0.0f;
    private static final float RED_LINE_BLUE = 0.0f;
    private static final float RED_LINE_ALPHA = 0.8f; // 稍微降低透明度，减少视觉干扰
    private static final double MAX_LINE_LENGTH = 100.0; // 最大显示100米
    
    // 平滑参数
    private static final float SMOOTHING_FACTOR = 0.1f; // 平滑系数，值越小越平滑
    
    // 用于平滑插值的缓存位置
    private static Vec3 smoothedPlayerPos = null;
    private static final Map<UUID, Vec3> smoothedTargetPositions = new HashMap<>(); // 每个目标的平滑位置缓存
    
    public static void setup() {
        // 注册世界渲染事件监听器
        Events.RenderWorldLast.add(ContractTrackerRenderer::onRenderWorldLast);
    }
    
    /**
     * 世界渲染事件处理方法
     */
    private static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player == null || mc.level == null) {
            return;
        }
        
        // 获取玩家契约管理器
        var contractManager = GlobalContractManager.getInstance().getContractManager(player);
        if (contractManager == null) {
            return;
        }
        
        // 获取当前玩家位置
        Vec3 currentPlayerPos = player.position();
        
        // 平滑玩家位置
        if (smoothedPlayerPos == null) {
            // 第一次初始化
            smoothedPlayerPos = currentPlayerPos;
        } else {
            // 使用线性插值进行平滑处理
            smoothedPlayerPos = new Vec3(
                lerp(smoothedPlayerPos.x, currentPlayerPos.x, SMOOTHING_FACTOR),
                lerp(smoothedPlayerPos.y, currentPlayerPos.y, SMOOTHING_FACTOR),
                lerp(smoothedPlayerPos.z, currentPlayerPos.z, SMOOTHING_FACTOR)
            );
        }
        
        // 开始渲染线条
        LineRenderer.instance.begin(event, true);
        
        // 清理不再追踪的契约的平滑位置缓存
        Set<UUID> activeTrackingTargets = new HashSet<>();
        
        try {
            // 遍历所有契约
            for (Contract contract : contractManager.getAllContracts()) {
                // 只渲染正在追踪的契约
                if (!contract.isTracking()) {
                    continue;
                }
                
                activeTrackingTargets.add(contract.getEntityId());
                
                // 检查维度是否匹配
                String currentDimension = mc.level.dimension().location().toString();
                if (!currentDimension.equals(contract.getDimension())) {
                    continue;
                }
                
                // 获取目标位置
                var targetPos = contract.getEntityPosition();
                Vec3 targetVec = new Vec3(targetPos.x + 0.5, targetPos.y + 0.5, targetPos.z + 0.5);
                
                // 平滑目标位置
                UUID targetId = contract.getEntityId();
                Vec3 smoothedTargetVec = smoothedTargetPositions.get(targetId);
                if (smoothedTargetVec == null) {
                    // 第一次初始化
                    smoothedTargetVec = targetVec;
                    smoothedTargetPositions.put(targetId, smoothedTargetVec);
                } else {
                    // 使用线性插值进行平滑处理
                    smoothedTargetVec = new Vec3(
                        lerp(smoothedTargetVec.x, targetVec.x, SMOOTHING_FACTOR),
                        lerp(smoothedTargetVec.y, targetVec.y, SMOOTHING_FACTOR),
                        lerp(smoothedTargetVec.z, targetVec.z, SMOOTHING_FACTOR)
                    );
                    smoothedTargetPositions.put(targetId, smoothedTargetVec);
                }
                
                // 计算距离
                double distance = smoothedPlayerPos.distanceTo(smoothedTargetVec);
                
                // 如果距离为0，不绘制线条
                if (distance < 0.1) {
                    continue;
                }
                
                // 计算线条的终点（如果超过100米，只显示100米部分）
                Vec3 endPos;
                if (distance <= MAX_LINE_LENGTH) {
                    endPos = smoothedTargetVec;
                } else {
                    // 计算方向向量
                    Vec3 direction = smoothedTargetVec.subtract(smoothedPlayerPos).normalize();
                    // 计算终点位置（100米处）
                    endPos = smoothedPlayerPos.add(direction.scale(MAX_LINE_LENGTH));
                }
                
                // 绘制红线
                LineRenderer.instance.line(
                    smoothedPlayerPos.x, smoothedPlayerPos.y, smoothedPlayerPos.z,
                    endPos.x -0.5, endPos.y, endPos.z -0.5,
                    RED_LINE_RED, RED_LINE_GREEN, RED_LINE_BLUE, RED_LINE_ALPHA
                );
            }
        } finally {
            // 结束渲染
            LineRenderer.instance.end();
            
            // 清理不再追踪的契约的平滑位置缓存
            smoothedTargetPositions.keySet().removeIf(targetId -> !activeTrackingTargets.contains(targetId));
        }
    }
    
    /**
     * 线性插值函数
     */
    private static double lerp(double start, double end, float factor) {
        return start + (end - start) * factor;
    }
}