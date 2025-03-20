package lpcminigame.events;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class UnregistrableEvent<T> implements Iterable<T>{
    public final T invokable;
    public UnregistrableEvent(T invokable){
        this.invokable = invokable;
    }
    public UnregistrableEvent<T> register(T invokable){
        UnregistrableEvent<T> newNode = new UnregistrableEvent<>(invokable);
        newNode.last = this;
        newNode.next = next;
        next = newNode;
        return newNode;
    }
    public void unregister(){
        if(last != null) last.next = next;
        if(next != null) next.last = last;
    }
    //注：遍历不包括自己
    @Override public @NotNull Iterator<T> iterator() {
        return new IteratorClass<>(next);
    }

    public static class IteratorClass<T> implements Iterator<T>{
        UnregistrableEvent<T> next;
        IteratorClass(UnregistrableEvent<T> next){
            this.next = next;
        }
        @Override public boolean hasNext() {
            return next != null;
        }
        @Override public T next() {
            T ret = next.invokable;
            next = next.next;
            return ret;
        }
    }

    private UnregistrableEvent<T> last, next;
}
