package com.iamalittle.black_souls_options.utils;

import com.mojang.blaze3d.vertex.VertexBuffer;

public class SharedVertexBuffer {
    public static final VertexBuffer instance = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
}