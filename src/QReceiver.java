import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;


public class QReceiver {
	public static void main(String args[]){
		ConnectionFactory factory = new ConnectionFactory();
		try {
			factory.setUri("amqp://localhost:5672");
		} catch (URISyntaxException | NoSuchAlgorithmException | KeyManagementException e) {
			e.printStackTrace();
		}

		try {
			Connection conn = factory.newConnection();
			Channel channel = conn.createChannel();
			channel.queueDeclare("lhc", false, false, false, null);
			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicConsume("lhc", true, consumer);

			QueueingConsumer.Delivery delivery=null;
			while (true){
				try {
					delivery = consumer.nextDelivery();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				String message = new String(delivery.getBody());
				System.out.println(" [x] Received '" + message + "'");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}


}
