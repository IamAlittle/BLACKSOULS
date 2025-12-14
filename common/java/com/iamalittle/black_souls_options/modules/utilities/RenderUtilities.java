package com.iamalittle.black_souls_options.modules.utilities;

import com.iamalittle.black_souls_options.render.LineRenderer;

public class RenderUtilities {
    
    public static final RenderUtilities instance = new RenderUtilities();
    
    public LineRenderer getLineRenderer() {
        return LineRenderer.instance;
    }
}