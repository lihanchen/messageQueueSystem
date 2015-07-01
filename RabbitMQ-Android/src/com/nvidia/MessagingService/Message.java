package com.nvidia.MessagingService;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


@SuppressWarnings("SuspiciousSystemArraycopy")
public class Message {
	public static final byte VERSION = (byte) 1;
	public String source;
	public String destination;
	public Object content;
	public Type type;
	public int channel;
	//public boolean broadcast;
	byte hashID[];


	public Message(String source, String destination, int channel, String content) {
		this(source, destination, channel, content, Type.string);
	}

	public Message(String source, String destination, int channel, Object content, Type type) {
		this.source = source;
		this.destination = destination;
		this.content = content;
		this.type = type;
		this.channel = channel;
		calculateModifiedMD5();
	}

	public Message(byte[] input) {
		if (input[0] != VERSION) throw new ClassCastException("Message of a different version");
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.put(0, input[1]);
		bb.put(1, input[2]);
		bb.put(2, input[3]);
		bb.put(3, input[4]);
		channel = bb.getInt();

		int start, offset;

		start = 5;
		offset = 0;
		while (input[start + offset] != 0) {
			offset++;
		}
		source = new String(input, start, offset);

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

		destination = null;

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

		if (length() != length)
			throw new ClassCastException("Verification Failed. Broken message");

		calculateModifiedMD5();
		if (!new String(hashID).equals(MD5))
			throw new ClassCastException("Verification Failed. Broken message");

	}

	public String toString() {
		switch (type) {
			case string:
				return (String) content;
			case JSON:
				return "JSON:\n" + content;
			case binary:
				return "Binary Length:" + length() + " Hash:" + new String(hashID);
			default:
				throw new ClassCastException("Unknown Message Type");
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
				throw new ClassCastException("Unknown Message Type");
		}
	}

	public void calculateModifiedMD5() { //replace all \0 to \1 for MD5
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return;
		}
		if (type == Type.binary)
			hashID = (byte[]) content;
		else
			hashID = toString().getBytes();
		hashID = md.digest(hashID);
		for (int i = 0; i < hashID.length; i++) if (hashID[i] == 0) hashID[i] = 1;
	}

	public byte[] generateOneMsg() {
		byte header[] = new byte[source.length() + 35];
		byte temp[];
		int headerLength = 0;

		header[headerLength++] = VERSION;

		ByteBuffer bb = ByteBuffer.allocate(4).putInt(channel);
		header[headerLength++] = bb.get(0);
		header[headerLength++] = bb.get(1);
		header[headerLength++] = bb.get(2);
		header[headerLength++] = bb.get(3);


		temp = source.getBytes();
		System.arraycopy(temp, 0, header, headerLength, temp.length);
		headerLength += temp.length;

		header[headerLength++] = 0;
		temp = Integer.toString(length()).getBytes();
		System.arraycopy(temp, 0, header, headerLength, temp.length);
		headerLength += temp.length;

		header[headerLength++] = 0;
		temp = hashID;
		System.arraycopy(temp, 0, header, headerLength, temp.length);
		headerLength += temp.length;

		header[headerLength++] = 0;
		temp = type.toString().getBytes();
		System.arraycopy(temp, 0, header, headerLength, temp.length);
		headerLength += temp.length;

		header[headerLength++] = 0;

		if (type == Type.binary) {
			byte ret[] = new byte[headerLength + length()];
			System.arraycopy(header, 0, ret, 0, headerLength);
			System.arraycopy(content, 0, ret, headerLength, length());
			return ret;
		} else {
			temp = ((String) content).getBytes();
			byte ret[] = new byte[headerLength + temp.length];
			System.arraycopy(header, 0, ret, 0, headerLength);
			System.arraycopy(temp, 0, ret, headerLength, temp.length);
			return ret;
		}
	}

	public enum Type {
		JSON, binary, string
	}

}
