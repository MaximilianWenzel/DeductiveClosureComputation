package nio2kryo;

interface Writer {

	boolean canWrite();

	void write(Object o);
}