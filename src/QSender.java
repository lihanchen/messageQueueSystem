import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.Scanner;


public class QSender {
	public static void main(String args[]){
		ConnectionFactory factory = new ConnectionFactory();
		try {
			factory.setUri("amqp://lhc:123@172.17.187.114:5672");
			Connection conn = factory.newConnection();
			Channel channel = conn.createChannel();
			channel.queueDeclare(args[0], false, false, false, null);
			String message = null;
			Scanner scanner=new Scanner(System.in);
			while (true) {
				message=scanner.nextLine();
				channel.basicPublish("", args[0], null, message.getBytes());
				System.out.println(" [x] Sent '" + message + "'");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
