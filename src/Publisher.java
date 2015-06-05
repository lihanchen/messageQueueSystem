/**
 * Created by hanchenl on 6/4/15.
 */

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.util.Scanner;

public class Publisher {
	private static final String EXCHANGE_NAME = "group";

	public static void main(String[] argv){
		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost("localhost");
			Connection connection = factory.newConnection();
			Channel channel = connection.createChannel();

			channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
			String message;
			Scanner scanner=new Scanner(System.in);

			while (true) {
				message=scanner.nextLine();
				channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes());
				System.out.println(" [x] Broadcast '" + message + "'");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
