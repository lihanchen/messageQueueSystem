import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;


public class QSender {
	public static void main(String args[]){
		ConnectionFactory factory = new ConnectionFactory();
		try {
			factory.setUri("amqp://localhost:5672");
			Connection conn = factory.newConnection();
			Channel channel = conn.createChannel();
			channel.queueDeclare("lhc", false, false, false, null);
			String message = null;
			Scanner scanner=new Scanner(System.in);
			while (true) {
				message=scanner.nextLine();
				channel.basicPublish("", "lhc", null, message.getBytes());
				System.out.println(" [x] Sent '" + message + "'");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
