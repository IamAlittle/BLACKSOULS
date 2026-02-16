package com.iamalittle.black_souls_options.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ParameterizedEventHandler<T> {
    
    private final List<Consumer<T>> listeners = new ArrayList<>();
    
    public void add(Consumer<T> listener) {
        listeners.add(listener);
    }
    
    public void remove(Consumer<T> listener) {
        listeners.remove(listener);
    }
    
    public void trigger(T event) {
        // 创建副本以避免ConcurrentModificationException
        for (Consumer<T> listener : new ArrayList<>(listeners)) {
            listener.accept(event);
        }
    }
    
    public void clear() {
        listeners.clear();
    }
}