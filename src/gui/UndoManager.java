package gui;

import java.util.*;

public class UndoManager {
    public static final class UndoAction {
        String type;
        int componenteId;
        Object data;
        long timestamp;

        public String getType() { return type; }
        public int getComponenteId() { return componenteId; }
        public Object getData() { return data; }

        UndoAction(String type, int componenteId, Object data) {
            this.type = type;
            this.componenteId = componenteId;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final Deque<UndoAction> stack = new ArrayDeque<>();
    private static final int MAX_UNDO = 5;
    private static final long UNDO_EXPIRATION_MS = 10000;

    public void push(String type, int componenteId, Object data) {
        if (stack.size() >= MAX_UNDO) stack.removeLast();
        stack.push(new UndoAction(type, componenteId, data));
    }

    public Optional<UndoAction> pop() {
        if (stack.isEmpty()) return Optional.empty();
        UndoAction action = stack.peek();
        long age = System.currentTimeMillis() - action.timestamp;
        if (age > UNDO_EXPIRATION_MS) {
            stack.pop();
            return Optional.empty();
        }
        return Optional.of(stack.pop());
    }

    public boolean hasUndo() {
        if (stack.isEmpty()) return false;
        UndoAction action = stack.peek();
        return (System.currentTimeMillis() - action.timestamp) <= UNDO_EXPIRATION_MS;
    }

    public void clear() {
        stack.clear();
    }

    public String getType() {
        return hasUndo() ? stack.peek().type : null;
    }

    public int getComponenteId() {
        return hasUndo() ? stack.peek().componenteId : -1;
    }

    public Object getData() {
        return hasUndo() ? stack.peek().data : null;
    }
}

