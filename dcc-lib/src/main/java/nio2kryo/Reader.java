package nio2kryo;

interface Reader {

	boolean canRead();

	Object read();
}