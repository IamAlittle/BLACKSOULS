package com.iamalittle.black_souls_options.input;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

/**
 * 契约能力按键管理器
 * 提供通用的按键状态检查，契约只需调用isAbilityKeyPressed()即可
 */
public class ContractAbilityKeyManager {
    
    // 通用能力触发按键（默认按键：R）
    public static final KeyMapping CONTRACT_ABILITY_KEY = new KeyMapping(
        "key.black_souls_options.contract_ability",
        GLFW.GLFW_KEY_R,
        "key.categories.gameplay"
    );
    
    private static boolean wasKeyPressed = false;
    
    /**
     * 更新按键状态（需要在客户端tick事件中调用）
     */
    public static void updateKeyState() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        
        if (player == null) {
            wasKeyPressed = false;
            return;
        }
        
        boolean isKeyPressed = CONTRACT_ABILITY_KEY.isDown();
        wasKeyPressed = isKeyPressed;
    }
    
    /**
     * 检查按键状态（供契约调用）
     * @return 如果按键按下返回true，否则返回false
     */
    public static boolean isAbilityKeyPressed() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        
        if (player == null) {
            return false;
        }
        
        // 直接返回按键按下状态，实现按住连续触发
        return CONTRACT_ABILITY_KEY.isDown();
    }
    
    /**
     * 获取按键描述
     */
    public static String getKeyDescription() {
        return "触发契约特殊能力";
    }
}