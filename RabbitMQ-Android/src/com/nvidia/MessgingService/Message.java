package com.nvidia.MessgingService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


@SuppressWarnings("SuspiciousSystemArraycopy")
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

		int start, offset;

		start = 1;
		offset = 0;
		while (input[start + offset] != 0) {
			offset++;
		}
		from = new String(input, start, offset);

		start += offset + 1;
		offset = 0;
		while (input[start + offset] != 0) {
			offset++;
		}
		int length = Integer.parseInt(new String(input, start, offset));

		start += offset + 1;
		offset = 0;
		while (input[start + offset] != 0) {
			offset++;
		}
		String MD5 = new String(input, start, offset);

		start += offset + 1;
		offset = 0;
		while (input[start + offset] != 0) {
			offset++;
		}
		String type = new String(input, start, offset);

		start += offset + 1;
		byte data[] = new byte[input.length - start];
		System.arraycopy(input, start, data, 0, input.length - start);

		to = null;

		if (type.equals(Type.JSON.toString())) {
			content = new String(data);
			this.type = Type.JSON;
		} else if (type.equals(Type.string.toString())) {
			content = new String(data);
			this.type = Type.string;
		} else if (type.equals(Type.binary.toString())) {
			content = data;
			this.type = Type.binary;
		} else throw new ClassCastException("Unrecognized Message");

		if ((length() != length) || (!getModifiedMD5().equals(MD5)))
			throw new ClassCastException("Verification Failed. Broken message");

	}

	public String toString() {
		switch (type) {
			case string:
				return (String) content;
			case JSON:
				return "JSON:\n" + content;
			case binary:
				throw new ClassCastException("Convert binary to String");
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
				throw new ClassCastException("Unrecognized Message");
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
		byte data[];
		if (type == Type.binary)
			data = (byte[]) content;
		else
			data = toString().getBytes();
		data = md.digest(data);
		for (int i = 0; i < data.length; i++) if (data[i] == 0) data[i] = 1;
		return new String(data);
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
		if (type == Type.binary) {
			byte header[] = str.getBytes();
			byte ret[] = new byte[header.length + length()];
			System.arraycopy(header, 0, ret, 0, header.length);
			System.arraycopy(content, 0, ret, header.length, length());
			return ret;
		} else {
			str += toString();
		}
		return str.getBytes();

	}

	public enum Type {
		JSON, binary, string
	}
}
