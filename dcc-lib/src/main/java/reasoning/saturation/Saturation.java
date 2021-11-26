package reasoning.saturation;

import data.Closure;
import reasoning.rules.Rule;

import java.util.Collection;

public interface Saturation {

    Closure saturate();

    void setRules(Collection<? extends Rule> rules);
}
