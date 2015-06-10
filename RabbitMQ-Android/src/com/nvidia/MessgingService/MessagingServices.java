package com.nvidia.MessgingService;

import android.app.IntentService;
import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;


public class MessagingServices extends IntentService {
	/**
	 * Creates an IntentService.  Invoked by your subclass's constructor.
	 *
	 */
	public static NotificationManager notificationManager;
	static int notificationID=0;
	NotificationCompat.Builder nBuilder;

	@Override
	public void onCreate() {
		super.onCreate();
		notificationManager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		nBuilder=new NotificationCompat.Builder(this);
		nBuilder.setContentTitle("NVIDIA Messaging Service");
		nBuilder.setSmallIcon(R.drawable.icon);
		nBuilder.setVibrate(new long[]{500,500});
	}

	public MessagingServices() {
		super("NVIDIA Messaging Services");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	final public String serverURI="amqp://lhc:123@172.17.187.114:5672";
	final public String broadcastExchangeName="broadcast_group";
	Thread broadcastReceiver,queueReceiver;
	public String currentID="lhc";
	public ConnectionFactory factory;

	@Override
	protected void onHandleIntent(Intent intent) {
		try{
			factory = new ConnectionFactory();
			factory.setUri(serverURI);
			factory.setConnectionTimeout(2000);
		}catch(Exception e){
			e.printStackTrace();
		}

		queueReceiver=new Thread(){
			@Override
			public void run() {
				try {
					Channel channel;
					Connection conn = factory.newConnection();
					channel = conn.createChannel();
					channel.queueDeclare(currentID, false, false, false, null);
					QueueingConsumer consumer = new QueueingConsumer(channel);
					channel.basicConsume(currentID, true, consumer);
					while (true){
						try {
							String msg=new String(consumer.nextDelivery().getBody());
							Log.e("rabbitMQ", " [x] Received '" + msg + "'");
							nBuilder.setContentText("Received:\n"+msg);
							notificationManager.notify(notificationID++, nBuilder.build());
						} catch (InterruptedException e) {
							return;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		queueReceiver.start();

		broadcastReceiver=new Thread(){
			@Override
			public void run() {
				try {
					Channel channel;
					Connection conn = factory.newConnection();
					channel = conn.createChannel();
					channel.exchangeDeclare(broadcastExchangeName, "fanout");
					String queueName = channel.queueDeclare().getQueue();
					channel.queueBind(queueName, broadcastExchangeName, "");
					channel.queueDeclare(currentID, false, false, false, null);

					QueueingConsumer consumer = new QueueingConsumer(channel);
					channel.basicConsume(queueName, true, consumer);
					while (true){
						try {
							String msg=new String(consumer.nextDelivery().getBody());
							Log.i("rabbitMQ", " [x] Received Broadcast'" + msg + "'");
							nBuilder.setContentText("Received Broadcast:\n"+ msg);
							notificationManager.notify(notificationID++, nBuilder.build());
						} catch (InterruptedException e) {
							return;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		broadcastReceiver.start();
	}

}
