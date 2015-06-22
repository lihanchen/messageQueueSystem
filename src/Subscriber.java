/**
 * Created by hanchenl on 6/4/15.
 */

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

public class Subscriber {
	public static void main(String[] argv){
		for (int i = 0; i < 1; i++) {
			new SubscriberThread(i).start();
		}
	}
}

class SubscriberThread extends Thread {
	private static final String EXCHANGE_NAME = "broadcast_group";
	int index;

	public SubscriberThread(int index) {
		super();
		this.index = index;
	}

	public void run() {
		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setUri("amqp://lhc:123@localhost:5672");
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

				System.out.println(" [x] Received from " + index + "   '" + message + "'");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}