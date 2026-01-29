package com.iamalittle.black_souls_options.client;

import com.iamalittle.black_souls_options.input.SpitAbilityKeyManager;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端事件处理器
 * 处理客户端相关的事件，如按键注册和更新
 */
@Mod.EventBusSubscriber(modid = "black_souls_options", value = Dist.CLIENT)
public class ClientEventHandler {
    
    // 存储所有需要注册的按键映射
    private static final List<KeyMapping> KEY_MAPPINGS = new ArrayList<>();
    
    static {
        // 添加吐口水能力按键
        KEY_MAPPINGS.add(SpitAbilityKeyManager.SPIT_ABILITY_KEY);
    }
    
    /**
     * 注册按键映射
     */
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyMapping keyMapping : KEY_MAPPINGS) {
            event.register(keyMapping);
        }
    }
    
    /**
     * 客户端tick事件，用于更新按键状态
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // 更新吐口水能力按键状态
            SpitAbilityKeyManager.updateKeyState();
        }
    }
}