package com.thorn.vm;

/**
 * Information about an upvalue in a closure.
 * Upvalues are variables from enclosing scopes that are captured by a function.
 */
public class UpvalueInfo {
    private final int index;         // Index in the upvalue array
    private final boolean isLocal;   // True if upvalue refers to a local in the immediately enclosing function
    private final int slot;          // Slot number in the enclosing function's locals or upvalues
    private final String name;       // Variable name for debugging
    
    public UpvalueInfo(int index, boolean isLocal, int slot, String name) {
        this.index = index;
        this.isLocal = isLocal;
        this.slot = slot;
        this.name = name;
    }
    
    public int getIndex() {
        return index;
    }
    
    public boolean isLocal() {
        return isLocal;
    }
    
    public int getSlot() {
        return slot;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return String.format("UpvalueInfo{index=%d, isLocal=%s, slot=%d, name='%s'}", 
                           index, isLocal, slot, name);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        UpvalueInfo that = (UpvalueInfo) obj;
        return index == that.index &&
               isLocal == that.isLocal &&
               slot == that.slot &&
               name.equals(that.name);
    }
    
    @Override
    public int hashCode() {
        int result = index;
        result = 31 * result + (isLocal ? 1 : 0);
        result = 31 * result + slot;
        result = 31 * result + name.hashCode();
        return result;
    }
}