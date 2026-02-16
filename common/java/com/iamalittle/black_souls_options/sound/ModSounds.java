package com.iamalittle.black_souls_options.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static SoundEvent CURSOR1;
    public static SoundEvent SWORD1;
    public static SoundEvent SWORD3;
    public static SoundEvent TITLE_SCREEN_BGM1;
    public static SoundEvent TITLE_SCREEN_BGM2;
    
    public static void initialize() {
        // 音效注册由Fabric和Forge各自的初始化代码处理
    }
    
    // Fabric版本的注册方法
    public static void registerFabricSounds() {
        CURSOR1 = registerSoundEventFabric("cursor1");
        SWORD1 = registerSoundEventFabric("sword1");
        SWORD3 = registerSoundEventFabric("sword3");
        TITLE_SCREEN_BGM1 = registerSoundEventFabric("title_screen_bgm1");
        TITLE_SCREEN_BGM2 = registerSoundEventFabric("title_screen_bgm2");
    }
    
    // Forge版本的注册方法
    public static void registerForgeSounds() {
        CURSOR1 = registerSoundEventForge("cursor1");
        SWORD1 = registerSoundEventForge("sword1");
        SWORD3 = registerSoundEventForge("sword3");
        TITLE_SCREEN_BGM1 = registerSoundEventForge("title_screen_bgm1");
        TITLE_SCREEN_BGM2 = registerSoundEventForge("title_screen_bgm2");
    }
    
    private static SoundEvent registerSoundEventFabric(String name) {
        ResourceLocation location = new ResourceLocation("black_souls_options", name);
        return SoundEvent.createVariableRangeEvent(location);
    }
    
    private static SoundEvent registerSoundEventForge(String name) {
        ResourceLocation location = new ResourceLocation("black_souls_options", name);
        return SoundEvent.createVariableRangeEvent(location);
    }
}