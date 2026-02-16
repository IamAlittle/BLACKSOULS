package com.iamalittle.black_souls_options.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SimpleEventHandler {
    
    private final List<Consumer<Void>> listeners = new ArrayList<>();
    
    public void add(Consumer<Void> listener) {
        listeners.add(listener);
    }
    
    public void remove(Consumer<Void> listener) {
        listeners.remove(listener);
    }
    
    public void trigger() {
        // 创建副本以避免ConcurrentModificationException
        for (Consumer<Void> listener : new ArrayList<>(listeners)) {
            listener.accept(null);
        }
    }
    
    public void clear() {
        listeners.clear();
    }
}