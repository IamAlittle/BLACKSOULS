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
        for (Consumer<Void> listener : listeners) {
            listener.accept(null);
        }
    }
    
    public void clear() {
        listeners.clear();
    }
}