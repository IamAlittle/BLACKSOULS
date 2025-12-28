package com.iamalittle.black_souls_options.common;

import com.iamalittle.black_souls_options.common.events.RenderWorldLastEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class Events {
    public static final SimpleEventHandler ChunkLoaded = new SimpleEventHandler();
    public static final SimpleEventHandler ChunkUnloaded = new SimpleEventHandler();
    public static final ParameterizedEventHandler<RenderWorldLastEvent> RenderWorldLast = new ParameterizedEventHandler<>();
    public static final ParameterizedEventHandler<EntityMoveEvent> EntityMoved = new ParameterizedEventHandler<>();
    public static final ParameterizedEventHandler<EntityDeathEvent> EntityDeath = new ParameterizedEventHandler<>();
    
    public static class EntityMoveEvent {
        private final Entity entity;
        private final Vec3 oldPos;
        private final Vec3 newPos;
        
        public EntityMoveEvent(Entity entity, Vec3 oldPos, Vec3 newPos) {
            this.entity = entity;
            this.oldPos = oldPos;
            this.newPos = newPos;
        }
        
        public Entity getEntity() {
            return entity;
        }
        
        public Vec3 getOldPos() {
            return oldPos;
        }
        
        public Vec3 getNewPos() {
            return newPos;
        }
    }
    
    public static class EntityDeathEvent {
        private final Entity entity;
        private final net.minecraft.world.damagesource.DamageSource damageSource;
        
        public EntityDeathEvent(Entity entity) {
            this.entity = entity;
            this.damageSource = null;
        }
        
        public EntityDeathEvent(Entity entity, net.minecraft.world.damagesource.DamageSource damageSource) {
            this.entity = entity;
            this.damageSource = damageSource;
        }
        
        public Entity getEntity() {
            return entity;
        }
        
        public net.minecraft.world.damagesource.DamageSource getDamageSource() {
            return damageSource;
        }
    }
}