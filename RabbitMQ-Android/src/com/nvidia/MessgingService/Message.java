package com.nvidia.MessgingService;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Message {
	public static final char VERSION = 1;
	public String from;
	public String to;
	public Object content;
	public Type type;

	public Message(String from, String to, String content) {
		this(from, to, content, Type.string);
	}

	public Message(String from, String to, Object content, Type type) {
		this.from = from;
		this.to = to;
		this.content = content;
		this.type = type;
	}


	public Message(byte[] input) throws Exception {
		if (input[0] != VERSION) throw new ClassCastException("Message of a different version");

		int first0, second0, third0, fourth0;
		String string = new String(input);
		first0 = string.indexOf(0);
		second0 = string.indexOf(0, first0 + 1);
		third0 = string.indexOf(0, second0 + 1);
		fourth0 = string.indexOf(0, third0 + 1);

		String type = string.substring(third0 + 1, fourth0);
		from = string.substring(1, first0);
		to = null;

		if (type.equals(Type.JSON.toString())) {
			content = string.substring(fourth0 + 1);
			this.type = Type.JSON;
		} else if (type.equals(Type.string.toString())) {
			content = string.substring(fourth0 + 1);
			this.type = Type.string;
		} else if (type.equals(Type.binary.toString())) {
			int length = input.length - fourth0 - 1;
			content = new byte[length];
			System.arraycopy(input, fourth0 + 1, content, 0, length);
			this.type = Type.binary;
			if (!getModifiedMD5().equals(string.substring(second0 + 1, third0)))
				throw new ClassCastException("Verification Failed. Broken message");
			return;
		} else throw new ClassCastException("Unrecognized Message");

		if ((length() != Integer.parseInt(string.substring(first0 + 1, second0))) || (!getModifiedMD5().equals(string.substring(second0 + 1, third0))))
			throw new ClassCastException("Verification Failed. Broken message");

	}

	public String toString() {
		switch (type) {
			case string:
				return (String) content;
			case JSON:
				return "JSON:\n" + content;
			case binary:
				return new String((byte[]) content);
			default:
				return "Unknown Message Type";
		}
	}

	public int length() {
		switch (type) {
			case string:
				return ((String) content).length();
			case JSON:
				return ((String) content).length();
			case binary:
				return ((byte[]) content).length;
			default:
				return -1;
		}
	}

	public String getModifiedMD5() { //replace all \0 to \1 for MD5
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		Log.e("MD5", (new String(md.digest((byte[]) content))).replace('\0', '\1'));
		if (type == Type.binary)
			return (new String(md.digest((byte[]) content))).replace('\0', '\1');
		else
			return (new String(md.digest(toString().getBytes()))).replace('\0', '\1');
	}

	public byte[] generateOneMsg() {
		String str = "" + VERSION + from;
		str += '\0';
		str += length();
		str += '\0';
		str += getModifiedMD5();
		str += '\0';
		str += type.toString();
		str += '\0';
		str += toString();
		return str.getBytes();
	}

	public enum Type {
		JSON, binary, string
	}
}
