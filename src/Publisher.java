/**
 * Created by hanchenl on 6/4/15.
 */

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class Publisher {
	private static final String EXCHANGE_NAME = "broadcast_group";

	public static void main(String[] argv){
		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setUri("amqp://127.0.0.1:5672");
			Connection connection = factory.newConnection();
			Channel channel = connection.createChannel();

			channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
			String message;
			Scanner scanner=new Scanner(System.in);

			while (true) {
				message=scanner.nextLine();
				channel.basicPublish(EXCHANGE_NAME, "", null, generateOneMsg("Computer", 1, message));
				System.out.println(" [x] Broadcast '" + message + "'");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static byte[] generateOneMsg(String source, int channel, String content) {
		byte header[] = new byte[source.length() + 35];
		byte temp[];
		int headerLength = 0;

		header[headerLength++] = 1;

		ByteBuffer bb = ByteBuffer.allocate(4).putInt(channel);
		header[headerLength++] = bb.get(0);
		header[headerLength++] = bb.get(1);
		header[headerLength++] = bb.get(2);
		header[headerLength++] = bb.get(3);


		temp = source.getBytes();
		System.arraycopy(temp, 0, header, headerLength, temp.length);
		headerLength += temp.length;

		header[headerLength++] = 0;
		temp = Integer.toString(content.length()).getBytes();
		System.arraycopy(temp, 0, header, headerLength, temp.length);
		headerLength += temp.length;

		header[headerLength++] = 0;
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}

		temp = content.getBytes();
		temp = md.digest(temp);
		for (int i = 0; i < temp.length; i++) if (temp[i] == 0) temp[i] = 1;

		System.arraycopy(temp, 0, header, headerLength, temp.length);
		headerLength += temp.length;

		header[headerLength++] = 0;
		temp = "string".getBytes();
		System.arraycopy(temp, 0, header, headerLength, temp.length);
		headerLength += temp.length;

		header[headerLength++] = 0;

		temp = (content).getBytes();
		byte ret[] = new byte[headerLength + temp.length];
		System.arraycopy(header, 0, ret, 0, headerLength);
		System.arraycopy(temp, 0, ret, headerLength, temp.length);
		return ret;
	}
}
