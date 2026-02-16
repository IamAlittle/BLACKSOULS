package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.util.*;

/**
 * 绵羊契约效果 - 彩虹变色
 * 玩家契约绵羊后获得的能力：
 * 1. 玩家模型随时间变换颜色，类似被命名为"jeb_"的绵羊
 * 2. 实现彩虹渐变效果
 */
public class SheepContract extends ContractEffect {
    private static final String EFFECT_ID = "sheep_rainbow_color";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.sheep.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.sheep.description";
    
    // 绵羊契约玩家集合
    private static final Set<UUID> sheepContractPlayers = new HashSet<>();
    
    // 彩虹循环周期（毫秒）- 控制整个彩虹循环的时间
    private static final long RAINBOW_CYCLE_DURATION = 3000L;
    
    public SheepContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            sheepContractPlayers.add(player.getUUID());
            
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
            sheepContractPlayers.remove(player.getUUID());
            
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
        // 彩虹变色效果在渲染器中处理，这里不需要每tick更新
    }
    
    /**
     * 获取当前彩虹颜色 - 使用平滑的HSV颜色渐变
     * @param player 玩家
     * @return RGB颜色数组，如果玩家没有契约则返回null
     */
    public static float[] getRainbowColor(Player player) {
        if (player == null || !hasSheepContract(player)) {
            return null;
        }
        
        // 基于游戏时间计算彩虹相位（0.0到1.0之间循环）
        long gameTime = player.level().getGameTime();
        double phase = (gameTime % (RAINBOW_CYCLE_DURATION / 50)) / (double)(RAINBOW_CYCLE_DURATION / 50);
        
        // 使用HSV颜色模型实现平滑的彩虹渐变
        return getRainbowColorFromPhase(phase);
    }
    
    /**
     * 根据相位值获取彩虹颜色（HSV颜色模型）
     * @param phase 相位值（0.0到1.0）
     * @return RGB颜色数组
     */
    private static float[] getRainbowColorFromPhase(double phase) {
        // HSV颜色模型：Hue从0到360度循环，Saturation=1.0，Value=1.0
        double hue = phase * 360.0;
        return hsvToRgb(hue, 1.0, 1.0);
    }
    
    /**
     * HSV转RGB颜色
     * @param h 色相（0-360）
     * @param s 饱和度（0-1）
     * @param v 亮度（0-1）
     * @return RGB颜色数组
     */
    private static float[] hsvToRgb(double h, double s, double v) {
        h = h % 360.0;
        if (h < 0) h += 360.0;
        
        double c = v * s;
        double x = c * (1 - Math.abs((h / 60.0) % 2 - 1));
        double m = v - c;
        
        double r = 0, g = 0, b = 0;
        
        if (h < 60) {
            r = c; g = x; b = 0;
        } else if (h < 120) {
            r = x; g = c; b = 0;
        } else if (h < 180) {
            r = 0; g = c; b = x;
        } else if (h < 240) {
            r = 0; g = x; b = c;
        } else if (h < 300) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }
        
        return new float[]{
            (float)(r + m),
            (float)(g + m), 
            (float)(b + m)
        };
    }
    
    /**
     * 检查玩家是否拥有绵羊契约效果
     */
    public static boolean hasSheepContract(Player player) {
        return player != null && sheepContractPlayers.contains(player.getUUID());
    }
    
    @Override
    protected long getTickInterval() {
        return 100; // 每1秒检查一次
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.sheep.effect_title")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.sheep.effect1")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.sheep.effect2")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.sheep.effect3")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        return details;
    }
}