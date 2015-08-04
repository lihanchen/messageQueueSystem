/**
 * Created by hanchenl on 6/4/15.
 */

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class Tester {
	private static final String EXCHANGE_NAME = "broadcast_group";

	static Connection conn;

	public static void main(String[] argv) {
		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setUri("amqp://localhost:5672");
			conn = factory.newConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}

		new Thread() {
			public void run() {
				try {
					Channel channel = conn.createChannel();
					channel.queueDeclare("Computer", false, false, false, null);
					QueueingConsumer consumer = new QueueingConsumer(channel);
					channel.basicConsume("Computer", true, consumer);

					QueueingConsumer.Delivery delivery = null;
					while (true) {
						try {
							delivery = consumer.nextDelivery();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						String message = new String(delivery.getBody());
						System.out.println(" [x] Received '" + message + "'");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();


		try {
			Channel channel = conn.createChannel();

			channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
			String message;
			Scanner scanner = new Scanner(System.in);

			while (true) {
				message = scanner.nextLine();
				channel.basicPublish(EXCHANGE_NAME, "", null, generateOneMsg("Computer", 0, message));
				System.out.println(" [x] Broadcast '" + message + "'");
			}
		} catch (Exception e) {
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
