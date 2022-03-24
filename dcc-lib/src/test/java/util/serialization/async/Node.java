package util.serialization.async;

import java.nio.ByteBuffer;

import com.github.jsonldjava.shaded.com.google.common.base.Preconditions;

import until.serialization.async.AsyncSerializable;
import until.serialization.async.AsyncSerializer;

public class Node implements AsyncSerializable {

	private String name_;

	private int value_;

	private Node left_;

	private Node right_;

	public Node() {

	}

	public Node(String name, int value, Node left, Node right) {
		Preconditions.checkNotNull(name);
		this.name_ = name;
		this.value_ = value;
		this.left_ = left;
		this.right_ = right;
	}

	@Override
	public String toString() {
		return "[" + (left_ == null ? "" : left_) + " " + name_ + " " + value_
				+ " " + (right_ == null ? "" : right_) + "]";
	}

	@Override
	public int hashCode() {
		return name_.hashCode() + value_
				+ (left_ == null ? 0 : left_.hashCode())
				+ (right_ == null ? 0 : right_.hashCode());
	}

	public static int getDepth(Node n) {
		if (n == null) {
			return 0;
		}
		int leftDepth = getDepth(n.left_);
		int rightDepth = getDepth(n.right_);
		return (Math.max(leftDepth, rightDepth)) + 1;
	}

	String getName() {
		return name_;
	}

	int getValue() {
		return value_;
	}

	Node getLeft() {
		return left_;
	}

	Node getRight() {
		return right_;
	}

	void setName(String name) {
		Preconditions.checkNotNull(name);
		this.name_ = name;
	}

	void setValue(int value) {
		this.value_ = value;
	}

	void setLeft(Node left) {
		this.left_ = left;
	}

	void setRight(Node right) {
		this.right_ = right;
	}

	@Override
	public void write(AsyncSerializer s, ByteBuffer in) {
		s.writeClassAndObject(in, left_);
		s.writeString(in, name_);
		s.writeInt(in, value_);
		s.writeClassAndObject(in, right_);
	}

	@Override
	public void read(AsyncSerializer s, ByteBuffer out) {
		s.readClassAndObject(out, this::setLeft);
		s.readString(out, this::setName);
		s.readInt(out, this::setValue);
		s.readClassAndObject(out, this::setRight);
	}

}
