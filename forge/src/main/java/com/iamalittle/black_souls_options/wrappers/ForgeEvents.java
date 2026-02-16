package com.iamalittle.black_souls_options.wrappers;

import com.iamalittle.black_souls_options.common.Events;
import com.iamalittle.black_souls_options.contracts.effects.mobs.AxolotlContract;
import com.iamalittle.black_souls_options.controllers.ContractsScreen;
import com.iamalittle.black_souls_options.controllers.TargetEntityScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;


@Mod.EventBusSubscriber(modid = "black_souls_options", bus = Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class ForgeEvents {
    
    // 存储实体位置跟踪信息
    private static final Map<UUID, Vec3> entityPositions = new HashMap<>();
    
    // 用于跟踪是否在暂停屏幕中
    private static boolean inPauseScreen = false;

    public static void setup() {
        // Forge事件通过注解自动注册，无需额外代码
    }

    @SubscribeEvent
    public static void onChunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            Events.ChunkLoaded.trigger();
        }
    }

    @SubscribeEvent
    public static void onChunkUnloaded(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            Events.ChunkUnloaded.trigger();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        // 跟踪实体位置变化
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            UUID entityId = entity.getUUID();
            Vec3 currentPos = entity.position();
            Vec3 oldPos = entityPositions.get(entityId);

            if (oldPos == null || !oldPos.equals(currentPos)) {
                // 实体移动了，触发事件
                if (oldPos != null) {
                    Events.EntityMoved.trigger(new Events.EntityMoveEvent(entity, oldPos, currentPos));
                }
                // 更新位置记录
                entityPositions.put(entityId, currentPos);
            }
        }

        // 清理不存在的实体位置记录（通过遍历当前实体列表来实现）
        var currentEntities = new HashSet<UUID>();
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            currentEntities.add(entity.getUUID());
        }
        entityPositions.keySet().removeIf(uuid -> !currentEntities.contains(uuid));
    }
    @SubscribeEvent
    public static void screenOpen(ScreenEvent.Opening event) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            // 检查玩家是否佩戴铁头盔且当前屏幕是TargetEntityScreen（对话界面）
            if (player.getItemBySlot(EquipmentSlot.HEAD).getItem() == Items.IRON_HELMET && 
                Minecraft.getInstance().screen instanceof TargetEntityScreen) {
                // 如果玩家佩戴铁头盔且正在显示对话界面，阻止其他GUI界面
                if (!(event.getNewScreen() instanceof TargetEntityScreen) && 
                    !(event.getNewScreen() instanceof PauseScreen) && 
                    !(event.getNewScreen() instanceof DeathScreen)) {
                    // 阻止所有非对话界面、非暂停屏幕、非死亡界面的GUI
                    event.setCanceled(true);
                }
            }
        }
        
        // 当屏幕关闭时重置暂停屏幕状态
        if (event.getCurrentScreen() == null) {
            inPauseScreen = false;
        }
    }
    


}