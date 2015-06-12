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

	final public String username="lhc";
	final public String password="123";
	final public String broadcastExchangeName="broadcast_group";
	static Thread broadcastReceiver,queueReceiver;
	static ConnectionFactory factory;
	static Channel sendChannel=null;
	static String IP;
	static String ID;

	@Override
	protected void onHandleIntent(Intent intent) {
		if (sendChannel!=null) {
			try {
				queueReceiver.interrupt();
				broadcastReceiver.interrupt();
				sendChannel.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		try {
			IP=intent.getStringExtra("IP");
			ID=intent.getStringExtra("ID");
			factory = new ConnectionFactory();
			factory.setUri("amqp://"+username+":"+password+"@"+IP+":5672");
			factory.setConnectionTimeout(2000);
			factory.setRequestedHeartbeat(60);
			Connection conn = factory.newConnection();
			sendChannel = conn.createChannel();
			Log.i("info","successs");
		} catch (Exception e) {
			e.printStackTrace();
		}

		queueReceiver=new Thread(){
			public void run() {
				Channel channel=null;
				String ConsumerTag=null;
				try {
					Connection conn = factory.newConnection();
					channel = conn.createChannel();
					channel.queueDeclare(ID, false, false, false, null);
					QueueingConsumer consumer = new QueueingConsumer(channel);
					ConsumerTag=channel.basicConsume(ID, true, consumer);
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
					Log.e("ERROR", e.getMessage());
					try {
						channel.basicCancel(ConsumerTag);
						channel.close();
					}catch(Exception e1) {
						Log.e("ERROR", e1.getMessage());
					}
				}
			}
		};
		queueReceiver.start();

		broadcastReceiver=new Thread(){
			String ConsumerTag=null;
			Channel channel;
			public void run() {
				try {

					Connection conn = factory.newConnection();
					channel = conn.createChannel();
					channel.exchangeDeclare(broadcastExchangeName, "fanout");
					String queueName = channel.queueDeclare().getQueue();
					channel.queueBind(queueName, broadcastExchangeName, "");

					QueueingConsumer consumer = new QueueingConsumer(channel);
					ConsumerTag=channel.basicConsume(queueName, true, consumer);
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
					try {
						channel.basicCancel(ConsumerTag);
						channel.close();
					}catch(Exception e1){
						e1.printStackTrace();
					}
					e.printStackTrace();
				}
			}
		};
		broadcastReceiver.start();
	}

}
