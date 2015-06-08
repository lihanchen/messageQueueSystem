import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;



public class QReceiver {
	public static void main(String args[]){
		ConnectionFactory factory = new ConnectionFactory();
		try {
			factory.setUri("amqp://lhc:123@172.17.187.114:5672");
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
