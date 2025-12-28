package com.iamalittle.black_souls_options.wrappers;

import com.iamalittle.black_souls_options.common.Events;
import com.iamalittle.black_souls_options.common.events.RenderWorldLastEvent;
import com.iamalittle.black_souls_options.contracts.effects.mobs.AxolotlContract;
import com.iamalittle.black_souls_options.controllers.TargetEntityScreen;
import com.iamalittle.black_souls_options.events.FabricPlayerDeathEventHandler;
import com.iamalittle.black_souls_options.fabric.ContractEventsFabric;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FabricEvents {
    
    // 存储实体位置跟踪信息
    private static final Map<UUID, Vec3> entityPositions = new HashMap<>();

    // 用于跟踪是否在暂停屏幕中
    private static boolean inPauseScreen = false;

    public static void setup() {
        // 注册屏幕初始化后事件，用于阻止玩家倒地时打开GUI界面
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            Player player = client.player;
            if (player != null && screen != null) {
                if (player.getItemBySlot(EquipmentSlot.HEAD).getItem() == Items.IRON_HELMET &&
                        client.screen instanceof TargetEntityScreen) {
                    // 如果玩家佩戴铁头盔且正在显示对话界面，阻止其他GUI界面
                    if (!(screen instanceof TargetEntityScreen) &&
                            !(screen instanceof PauseScreen) &&
                            !(screen instanceof DeathScreen)) {
                        // 阻止所有非对话界面、非暂停屏幕、非死亡界面的GUI
                        client.setScreen(null);
                    }
                }
        }});

        WorldRenderEvents.LAST.register(context -> {
            Events.RenderWorldLast.trigger(new RenderWorldLastEvent(context.matrixStack(), context.tickDelta(), context.projectionMatrix()));
        });
        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
            Events.ChunkLoaded.trigger();
        });
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            Events.ChunkUnloaded.trigger();
        });
        
        // 实体加载时开始跟踪位置
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity != null && level != null && level.isClientSide()) {
                entityPositions.put(entity.getUUID(), entity.position());
            }
        });
        
        // 实体卸载时停止跟踪位置
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity != null && level != null && level.isClientSide()) {
                entityPositions.remove(entity.getUUID());
            }
        });
        
        // 每tick检查实体位置变化
        WorldRenderEvents.START.register(context -> {
            if (context.world() != null && context.world().isClientSide()) {
                for (Entity entity : context.world().entitiesForRendering()) {
                    UUID entityId = entity.getUUID();
                    Vec3 currentPos = entity.position();
                    Vec3 lastPos = entityPositions.get(entityId);
                    
                    if (lastPos != null && !lastPos.equals(currentPos)) {
                        // 位置发生变化，触发移动事件
                        Events.EntityMoved.trigger(new Events.EntityMoveEvent(entity, lastPos, currentPos));
                        entityPositions.put(entityId, currentPos);
                    } else if (lastPos == null) {
                        // 新实体，记录初始位置
                        entityPositions.put(entityId, currentPos);
                    }
                }
            }
        });
        
        // 初始化契约事件处理器
        ContractEventsFabric.initialize();
        
        // 初始化死亡图腾事件处理器
        FabricPlayerDeathEventHandler.initialize();
        
        // 初始化死亡图腾数据事件处理器
        com.iamalittle.black_souls_options.fabric.DeathTotemEventsFabric.initialize();
    }
}