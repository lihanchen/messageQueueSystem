package com.nvidia.MessgingService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by hanchenl on 6/12/15.
 */
public class Message {
	public String from;
	public String to;
	public Object content;
	public Type type;
	public Message(String from, String to, String content) {
		this.from = from;
		this.to = to;
		this.content = content;
		type = Type.string;
	}

	public Message(byte[] input) throws Exception {
		String[] split = new String(input).split("\0");
		from = split[0];


		if (split[3].equals(Type.JSON.toString())) {
			content = split[4];
			type = Type.JSON;
		} else if (split[3].equals(Type.string.toString())) {
			content = split[4];
			type = Type.string;
		} else throw new Exception("Unrecognized Message");

		if ((length() != Integer.parseInt(split[1])) || (!getSHA1().equals(split[2])))
			throw new Exception("Verification Failed. Broken message");

		to = null;
	}

	public String toString() {
		switch (type) {
			case string:
				return (String) content;
			case JSON:
				return "JSON:\n" + (String) content;
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
			default:
				return -1;
		}
	}

	public String getSHA1() {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		return new String(md.digest(toString().getBytes()));
	}

	public byte[] generateOneMsg() {
		String str = from;
		str += '\0';
		str += length();
		str += '\0';
		str += getSHA1();
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
