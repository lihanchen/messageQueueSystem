package com.nvidia.MessgingService;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.io.File;
import java.io.FileOutputStream;


public class MessagingServices extends IntentService {
	final public static String broadcastExchangeName = "broadcast_group";

	public static NotificationManager notificationManager;
	static int notificationID=0;
	static Thread broadcastReceiver, queueReceiver;
	static ConnectionFactory factory;
	static Channel sendChannel = null;
	static String IP;
	static String ID;
	final public String username="lhc";
	final public String password="123";
	final Messenger messenger=new Messenger(new IncomingHandler());
	NotificationCompat.Builder nBuilder;

	public MessagingServices() {
		super("NVIDIA Messaging Services");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nBuilder = new NotificationCompat.Builder(this);
		nBuilder.setContentTitle("NVIDIA Messaging Service");
		nBuilder.setSmallIcon(R.mipmap.notification_icon);
		nBuilder.setVibrate(new long[]{500, 500});

	}

	@Override
	public IBinder onBind(Intent intent) {
		return messenger.getBinder();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (sendChannel!=null) {
			try {
				queueReceiver.interrupt();
				broadcastReceiver.interrupt();
				sendChannel.close();
			} catch (Exception e) {
				Log.e("ERROR","ERROR",e);
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
			nBuilder.setContentText("Cannot connect to server!");
			nBuilder.setTicker("Cannot connect to server!");
			notificationManager.notify(notificationID++, nBuilder.build());
			Log.e("ERROR","ERROR",e);
		}

		queueReceiver=new Thread(){
			public void run() {
				Channel channel;
				String ConsumerTag;
				try {
					Connection conn = factory.newConnection();
					channel = conn.createChannel();
					channel.queueDeclare(ID, false, false, false, null);
					QueueingConsumer consumer = new QueueingConsumer(channel);
					ConsumerTag=channel.basicConsume(ID, true, consumer);
					while (true){
						try {
							com.nvidia.MessgingService.Message msg = new com.nvidia.MessgingService.Message(consumer.nextDelivery().getBody());
							if (msg.type == com.nvidia.MessgingService.Message.Type.binary) {
								processBinary(msg);
								continue;
							}
							Log.i("rabbitMQ", " [x] Received '" + msg + "'");
							nBuilder.setContentText("From " + msg.from + ":\n" + msg);
							nBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText("Received Message From " + msg.from + ":\n" + msg));
							nBuilder.setTicker(msg.toString());
							notificationManager.notify(notificationID++, nBuilder.build());
						} catch (InterruptedException e) {
							try {
								channel.basicCancel(ConsumerTag);
								channel.close();
							}catch(Exception e1) {
								Log.e("ERROR","ERROR",e1);
							}
							return;
						} catch (ClassCastException e) {
							Log.e("ERROR", "ERROR", e);
						}
					}
				} catch (Exception e) {
					Log.e("ERROR","ERROR",e);
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
							com.nvidia.MessgingService.Message msg = new com.nvidia.MessgingService.Message(consumer.nextDelivery().getBody());
							if (msg.from.equals(ID)) return;
							if (msg.type == com.nvidia.MessgingService.Message.Type.binary) {
								processBinary(msg);
								continue;
							}
							Log.i("rabbitMQ", " [x] Received Broadcast'" + msg + "'");
							nBuilder.setContentText("Broadcast from " + msg.from + ":\n" + msg);
							nBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText("Received broadcast From " + msg.from + ":\n" + msg));
							nBuilder.setTicker(msg.toString());
							notificationManager.notify(notificationID++, nBuilder.build());
						} catch (InterruptedException e) {
							try {
								channel.basicCancel(ConsumerTag);
								channel.close();
							}catch(Exception e1) {
								Log.e("ERROR","ERROR",e1);
							}
							return;
						} catch (ClassCastException e) {
							Log.e("ERROR", "ERROR", e);
						}
					}
				} catch (Exception e) {
					Log.e("ERROR","ERROR",e);
				}
			}

		};
		broadcastReceiver.start();
	}

	public void processBinary(com.nvidia.MessgingService.Message msg) {
		try {
			File file = new File("/sdcard/vim2.jpg");
			FileOutputStream fos = new FileOutputStream(file);
			fos.write((byte[]) msg.content);
			fos.close();
			nBuilder.setContentText("Received binary file from " + msg.from);
			nBuilder.setTicker("Received binary file");
			notificationManager.notify(notificationID++, nBuilder.build());
		} catch (Exception e) {
			Log.e("ERROR", "ERROR", e);
		}

	}

	public enum messageWhat {
		Send, ReceivedP2PMessage, ReceivedBroadCast, sendFailed, FatalErr
	}

	static class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what==messageWhat.Send.ordinal()){
				com.nvidia.MessgingService.Message pendingMsg=(com.nvidia.MessgingService.Message)msg.obj;
				if (pendingMsg.to==null)
					try {
						sendChannel.basicPublish(broadcastExchangeName, "", null, pendingMsg.generateOneMsg());
					} catch (Exception e) {
						Log.e("ERROR", "ERROR", e);
					}
				else
					try {
						sendChannel.basicPublish("", pendingMsg.to, null, pendingMsg.generateOneMsg());
					} catch (Exception e) {
						Log.e("ERROR", "ERROR", e);
					}
			}
		}
	}

}
