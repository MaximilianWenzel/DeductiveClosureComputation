package reasoning.rules;

import data.Closure;
import data.ToDoQueue;

public abstract class Rule<P> {

    protected Closure<P> closure;
    private ToDoQueue<P> toDo;

    protected Rule() {
    }

    public abstract void apply(P axiom);

    public void setToDo(ToDoQueue<P> toDo) {
        this.toDo = toDo;
    }

    public void setClosure(Closure<P> closure) {
        this.closure = closure;
    }

    protected boolean addToToDo(P axiom) {
        return this.toDo.add(axiom);
    }
}
