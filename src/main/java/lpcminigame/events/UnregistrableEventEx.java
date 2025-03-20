package lpcminigame.events;

public class UnregistrableEventEx<T> extends UnregistrableEvent<T>{
    public Initializer initializer;
    public T invoker;
    public UnregistrableEventEx(Initializer initializer, T invoker) {
        super(null);
        this.initializer = initializer;
        this.invoker = invoker;
    }
    public T invoker(){
        return invoker;
    }
    public interface Initializer{
        void init();
    }
    @Override public UnregistrableEvent<T> register(T invokable){
        if(initializer != null){
            initializer.init();
            initializer = null;
        }
        return super.register(invokable);
    }
}
