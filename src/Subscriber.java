/**
 * Created by hanchenl on 6/4/15.
 */

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

public class Subscriber {
	private static final String EXCHANGE_NAME = "broadcast_group";

	public static void main(String[] argv){
		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setUri("amqp://lhc:123@172.17.187.114:5672");
			Connection connection = factory.newConnection();
			Channel channel = connection.createChannel();

			channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
			String queueName = channel.queueDeclare().getQueue();
			channel.queueBind(queueName, EXCHANGE_NAME, "");

			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicConsume(queueName, true, consumer);

			while (true) {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				String message = new String(delivery.getBody());

				System.out.println(" [x] Received '" + message + "'");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
