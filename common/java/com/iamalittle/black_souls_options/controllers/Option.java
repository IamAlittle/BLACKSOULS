package com.iamalittle.black_souls_options.controllers;

import net.minecraft.network.chat.Component;

public class Option {
    private Component textComponent;
    private int defaultColor;
    private int confirmedColor;
    private int shadowColor;
    private int index;
    private boolean isConfirmed;
    
    public Option(Component textComponent, int defaultColor, int confirmedColor) {
        this.textComponent = textComponent;
        this.defaultColor = defaultColor;
        this.confirmedColor = confirmedColor;
        this.shadowColor = 0x000000; // 默认黑色阴影
        this.isConfirmed = false;
    }
    
    public Component getTextComponent() {
        return textComponent;
    }
    
    public String getText() {
        return textComponent.getString();
    }
    
    public int getDefaultColor() {
        return defaultColor;
    }
    
    public int getConfirmedColor() {
        return confirmedColor;
    }
    
    public int getShadowColor() {
        return shadowColor;
    }
    
    public void setShadowColor(int shadowColor) {
        this.shadowColor = shadowColor;
    }
    
    public int getIndex() {
        return index;
    }
    
    public void setIndex(int index) {
        this.index = index;
    }
    
    public boolean isConfirmed() {
        return isConfirmed;
    }
    
    public void setConfirmed(boolean confirmed) {
        this.isConfirmed = confirmed;
    }
    
    public int getCurrentColor() {
        return isConfirmed ? confirmedColor : defaultColor;
    }
}