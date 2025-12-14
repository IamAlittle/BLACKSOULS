package com.iamalittle.black_souls_options.contracts.effects;

import net.minecraft.world.entity.player.Player;

public abstract class ContractEffect {
    
    protected String id;
    protected String name;
    protected String description;
    
    public ContractEffect(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
    
    public abstract void onActivate(Player player);
    
    public abstract void onDeactivate(Player player);
    
    public abstract void onTick(Player player);
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return "ContractEffect{id='" + id + "', name='" + name + "', description='" + description + "'}";
    }
}