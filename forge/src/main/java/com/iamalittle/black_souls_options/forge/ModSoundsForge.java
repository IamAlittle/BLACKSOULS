package com.iamalittle.black_souls_options.forge;

import com.iamalittle.black_souls_options.sound.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = "black_souls_options", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModSoundsForge {
    
    @SubscribeEvent
    public static void onRegisterSounds(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.SOUND_EVENTS, helper -> {
            // 注册自定义音效
            helper.register(new ResourceLocation("black_souls_options", "cursor1"), 
                SoundEvent.createVariableRangeEvent(new ResourceLocation("black_souls_options", "cursor1")));
            helper.register(new ResourceLocation("black_souls_options", "sword1"), 
                SoundEvent.createVariableRangeEvent(new ResourceLocation("black_souls_options", "sword1")));
            helper.register(new ResourceLocation("black_souls_options", "sword3"), 
                SoundEvent.createVariableRangeEvent(new ResourceLocation("black_souls_options", "sword3")));
            helper.register(new ResourceLocation("black_souls_options", "title_screen_bgm1"), 
                SoundEvent.createVariableRangeEvent(new ResourceLocation("black_souls_options", "title_screen_bgm1")));
            helper.register(new ResourceLocation("black_souls_options", "title_screen_bgm2"), 
                SoundEvent.createVariableRangeEvent(new ResourceLocation("black_souls_options", "title_screen_bgm2")));
        });
    }
    
    @SubscribeEvent
    public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        // 在客户端设置完成后初始化音效引用
        event.enqueueWork(() -> {
            ModSounds.CURSOR1 = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("black_souls_options", "cursor1"));
            ModSounds.SWORD1 = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("black_souls_options", "sword1"));
            ModSounds.SWORD3 = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("black_souls_options", "sword3"));
            ModSounds.TITLE_SCREEN_BGM1 = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("black_souls_options", "title_screen_bgm1"));
            ModSounds.TITLE_SCREEN_BGM2 = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("black_souls_options", "title_screen_bgm2"));
        });
    }
}