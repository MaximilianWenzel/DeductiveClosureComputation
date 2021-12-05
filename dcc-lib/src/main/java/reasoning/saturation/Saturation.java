package reasoning.saturation;

import data.Closure;

import java.io.Serializable;

public interface Saturation<C extends Closure<A>, A extends Serializable> {

    C saturate();
}
