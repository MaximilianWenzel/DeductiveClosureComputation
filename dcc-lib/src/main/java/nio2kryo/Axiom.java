package nio2kryo;

import java.io.Serializable;

import until.serialization.async.AsyncSerializable;

/**
 * Axioms can be derived by inference rules and used as premises of inference
 * rules
 * 
 * @author Yevgeny Kazakov
 */
public interface Axiom extends Serializable, AsyncSerializable {

}
