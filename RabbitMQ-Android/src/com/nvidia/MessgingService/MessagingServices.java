package com.nvidia.MessgingService;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.recovery.AutorecoveringConnection;

import java.io.File;
import java.io.FileOutputStream;


public class MessagingServices extends IntentService {
	final public static String broadcastExchangeName = "broadcast_group";
	final public static int RECONNECT_WAITING_TIME = 60000;
	final public static String username = "lhc";
	final public static String password = "123";
	final static Messenger messenger = new Messenger(new IncomingHandler());
	public static NotificationManager notificationManager;
	static int notificationID=0;
	static ConnectionFactory factory;
	static Channel sendChannel = null, queueReceiverChannel, BroadcastReceiverChannel;
	static String queueReceiverConsumerTag;
	static String IP;
	static String ID;
	static NotificationCompat.Builder nBuilder;
	static Thread broadcastReceiver, queueReceiver;
	static AutorecoveringConnection conn;

	public MessagingServices() {
		super("NVIDIA Messaging Services");
	}

	public static synchronized void processBinary(com.nvidia.MessgingService.Message msg) {
		new Thread() {
			public void run() {
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
		}.start();
	}

	public static void startReceiverThreads() {
		queueReceiver = new Thread() {
			public void run() {
				try {
					try {
						queueReceiverChannel.basicCancel(queueReceiverConsumerTag);
					} catch (Exception e) {
					}
					try {
						queueReceiverChannel.close();
					} catch (Exception e) {
					}
					queueReceiverChannel = conn.createChannel();
					queueReceiverChannel.queueDeclare(MessagingServices.ID, false, false, false, null);
					QueueingConsumer consumer = new QueueingConsumer(queueReceiverChannel);
					queueReceiverConsumerTag = queueReceiverChannel.basicConsume(MessagingServices.ID, false, consumer);
					queueReceiverChannel.basicRecover();
					Log.i("info", "queueReceiver Running");
					while (true) {
						try {
							QueueingConsumer.Delivery delivery = consumer.nextDelivery();
							Log.i("Tag", "" + delivery.getEnvelope().getDeliveryTag());
							com.nvidia.MessgingService.Message msg = new com.nvidia.MessgingService.Message(delivery.getBody());
							Log.i("rabbitMQ", " [x] Received '" + msg + "'");
							MessagingServices.nBuilder.setContentText("From " + msg.from + ":\n" + msg);
							MessagingServices.nBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText("Received Message From " + msg.from + ":\n" + msg));
							MessagingServices.nBuilder.setTicker(msg.toString());
							MessagingServices.notificationManager.notify(MessagingServices.notificationID++, MessagingServices.nBuilder.build());
							queueReceiverChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
						} catch (InterruptedException e) {
							Log.i("info", "queueReceiver Killed");
							try {
								queueReceiverChannel.basicCancel(queueReceiverConsumerTag);
								queueReceiverChannel.close();
							} catch (Exception e1) {
								Log.e("ERROR", "ERROR", e1);
							}
							return;
						} catch (ClassCastException e) {
							Log.e("ERROR", "ERROR", e);
						}
					}
				} catch (Exception e) {
					Log.e("ERROR", "ERROR", e);
				}
			}
		};

		broadcastReceiver = new Thread() {
			String ConsumerTag = null;
			Channel channel;

			public void run() {
				if ("".length() == 0) return;
				try {
					channel = conn.createChannel();
					channel.exchangeDeclare(broadcastExchangeName, "fanout");
					String queueName = channel.queueDeclare().getQueue();
					channel.queueBind(queueName, broadcastExchangeName, "");

					QueueingConsumer consumer = new QueueingConsumer(channel);
					ConsumerTag = channel.basicConsume(queueName, true, consumer);
					channel.basicRecover();
					Log.i("info", "BroadcastReceiver Running");
					while (true) {
						try {
							com.nvidia.MessgingService.Message msg = new com.nvidia.MessgingService.Message(consumer.nextDelivery().getBody());
							if (msg.from.equals(ID)) continue;
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
							Log.i("info", "BroadcastReceiver Killed");
							try {
								channel.basicCancel(ConsumerTag);
								channel.close();
							} catch (Exception e1) {
								Log.e("ERROR", "ERROR", e1);
							}
							return;
						} catch (ClassCastException e) {
							Log.e("ERROR", "ERROR", e);
						}
					}
				} catch (Exception e) {
					Log.e("ERROR", "ERROR", e);
					//connectState = -1;
					//Connect();
				}
			}
		};

		queueReceiver.start();
		//broadcastReceiver.start();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nBuilder = new NotificationCompat.Builder(this);
		nBuilder.setContentTitle("NVIDIA Messaging Service");
		nBuilder.setSmallIcon(R.mipmap.notification_icon);
		//nBuilder.setVibrate(new long[]{500, 500});
	}

	@Override
	public IBinder onBind(Intent intent) {
		return messenger.getBinder();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		IP = intent.getStringExtra("IP");
		ID = intent.getStringExtra("ID");
		try {
			conn.close();
		} catch (Exception e) {
		}
		while (true) {
			Log.w("Connect", "Start Connection");
			if (sendChannel != null) {
				try {
					queueReceiver.interrupt();
					broadcastReceiver.interrupt();
					sendChannel.close();
				} catch (Exception e) {
					Log.e("ERROR", "ERROR", e);
				}
			}

			try {
				factory = new ConnectionFactory();
				factory.setUri("amqp://" + username + ":" + password + "@" + IP + ":5672");
				factory.setConnectionTimeout(5000);
				factory.setRequestedHeartbeat(600);
				factory.setAutomaticRecoveryEnabled(true);
				factory.setTopologyRecoveryEnabled(true);
				factory.setNetworkRecoveryInterval(5000);
				conn = (AutorecoveringConnection) factory.newConnection();
				conn.addRecoveryListener(new RecoveryListener() {
					@Override
					public void handleRecovery(Recoverable recoverable) {
						Log.i("Recover", "Recovered");
						startReceiverThreads();
					}
				});
				sendChannel = conn.createChannel();
				Log.i("info", "success");
				startReceiverThreads();
				return;
			} catch (Exception e) {
				Log.e("ERROR", "ERROR", e);
				Log.w("Connect", "Connecting Failed");
				try {
					Thread.sleep(RECONNECT_WAITING_TIME);
				} catch (InterruptedException e1) {
				}
			}

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

	public static class BootBroadcastReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			Intent service = new Intent(context, MessagingServices.class);
			context.startService(service);
		}

	}

}

class RThread extends Thread {
	public Channel channel;
	public String ConsumerTag;
	public QueueingConsumer consumer;

	public void run() {

	}
}