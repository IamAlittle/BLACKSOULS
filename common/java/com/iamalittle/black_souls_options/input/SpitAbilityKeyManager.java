package com.iamalittle.black_souls_options.input;

import com.iamalittle.black_souls_options.contracts.effects.mobs.LlamaContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.ParrotContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.SnowGolemContract;
import com.iamalittle.black_souls_options.network.ContractNetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

/**
 * 吐口水能力按键管理器
 * 管理吐口水能力的按键触发
 */
public class SpitAbilityKeyManager {
    
    // 吐口水能力按键绑定（默认R键）
    public static final KeyMapping SPIT_ABILITY_KEY = new KeyMapping(
        "key.black_souls_options.spit_ability",
        GLFW.GLFW_KEY_R,
        "key.categories.black_souls_options"
    );
    
    // 按键状态跟踪
    private static boolean wasKeyPressed = false;
    
    /**
     * 更新按键状态
     * 应该在每帧调用
     */
    public static void updateKeyState() {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        
        boolean isKeyPressed = SPIT_ABILITY_KEY.isDown();
        
        // 检测按键按下事件（从释放到按下）
        if (isKeyPressed && !wasKeyPressed) {
            // 按键刚刚按下，触发吐口水能力
            triggerSpitAbility();
        }
        
        wasKeyPressed = isKeyPressed;
    }
    
    /**
     * 触发吐口水能力
     */
    private static void triggerSpitAbility() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        // 检查玩家是否拥有羊驼契约
        if (LlamaContract.hasLlamaContract(player)) {
            // 使用ContractNetworkHandler发送网络包到服务端执行吐口水攻击
            ContractNetworkHandler.sendSpitAttackRequest();
        }
        
        // 检查玩家是否拥有鹦鹉契约
        if (ParrotContract.hasParrotContract(player)) {
            // 使用ContractNetworkHandler发送网络包到服务端执行随机音效播放
            ContractNetworkHandler.sendRandomSoundRequest();
        }
        
        // 检查玩家是否拥有雪傀儡契约
        if (SnowGolemContract.hasSnowGolemContract(player)) {
            // 使用ContractNetworkHandler发送网络包到服务端执行雪球攻击
            ContractNetworkHandler.sendSnowballAttackRequest();
        }
    }
    
    /**
     * 发送吐口水攻击网络包（已废弃，使用ContractNetworkHandler.sendSpitAttackRequest()替代）
     */
    private static void sendSpitAttackPacket() {
        // 已废弃，直接调用ContractNetworkHandler.sendSpitAttackRequest()
        ContractNetworkHandler.sendSpitAttackRequest();
    }
    
    /**
     * 检查吐口水能力按键是否被按下
     * @return 按键是否被按下
     */
    public static boolean isSpitKeyPressed() {
        return SPIT_ABILITY_KEY.isDown();
    }
    
    /**
     * 获取按键描述
     * @return 按键描述文本
     */
    public static String getKeyDescription() {
        return "R键 - 特殊能力（吐口水/雪球攻击）";
    }
}