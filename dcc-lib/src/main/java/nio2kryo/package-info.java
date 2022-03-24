/**
 * A simple echo test using Nio2 for asynchronous communication and Kryo for
 * serialization. {@link nio2kryo.Nio2KryoClient} generates random objects
 * {@link nio2kryo.Edge} that are sent to {@link nio2kryo.Nio2KryoServer}, and
 * then sent back.
 * 
 * To test, first start {@link nio2kryo.Nio2KryoServer}, and then start
 * {@link nio2kryo.Edge}. By default, {@link nio2kryo.Nio2KryoServer} runs on
 * localhost, but this can be configured using the command line argument.
 */
package nio2kryo;