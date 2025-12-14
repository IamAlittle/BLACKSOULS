package com.iamalittle.black_souls_options.contracts;

import net.minecraft.world.entity.player.Player;

import java.util.*;

public class GlobalContractManager {
    
    private static GlobalContractManager instance;
    
    public static GlobalContractManager getInstance() {
        if (instance == null) {
            instance = new GlobalContractManager();
        }
        return instance;
    }
    
    public void onPlayerJoin(Player player) {
        // 玩家加入游戏时的逻辑
        System.out.println("Player joined: " + player.getName().getString());
        // 可以在这里加载玩家保存的契约数据
    }
    
    public void onPlayerLeave(Player player) {
        // 玩家离开游戏时的逻辑
        System.out.println("Player left: " + player.getName().getString());
        // 可以在这里保存玩家的契约数据
    }
    
    public void update() {
        // 全局更新逻辑
        // 可以在这里处理所有玩家的契约更新
    }
}