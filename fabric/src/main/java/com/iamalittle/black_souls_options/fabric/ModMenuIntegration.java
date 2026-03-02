package com.iamalittle.black_souls_options.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.iamalittle.black_souls_options.config.ConfigScreenBuilder;

/**
 * Mod Menu集成类，为Fabric提供配置界面入口
 */
public class ModMenuIntegration implements ModMenuApi {
    
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreenBuilder::createConfigScreen;
    }
}