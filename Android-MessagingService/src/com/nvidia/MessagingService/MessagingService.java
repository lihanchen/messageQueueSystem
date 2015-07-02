package com.nvidia.MessagingService;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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


public class MessagingService extends IntentService {
	final public static String broadcastExchangeName = "broadcast_group";
	final public static int RECONNECT_WAITING_TIME = 30000;
	final static Messenger messenger = new Messenger(new IncomingHandler());
	public static NotificationManager notificationManager;
	static int notificationID=0;
	static ConnectionFactory factory;
	static Channel sendChannel = null, queueReceiverChannel;
	static String queueReceiverConsumerTag;
	static String IP;
	static String ID;
	static NotificationCompat.Builder nBuilder;
	static Thread broadcastReceiver, queueReceiver, connectingThread;
	static AutorecoveringConnection conn;
	static boolean running = false;
	static SharedPreferences sp;


	public MessagingService() {
		super("NVIDIA Messaging Services");
	}

	public static synchronized void processBinary(com.nvidia.MessagingService.Message msg) {
		new Thread() {
			public void run() {
				try {
					File file = new File("/sdcard/vim2.jpg");
					FileOutputStream fos = new FileOutputStream(file);
					fos.write((byte[]) msg.content);
					fos.close();
					nBuilder.setContentText("Received binary file from " + msg.source);
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
						Log.i("ERROR", "ERROR", e);
					}
					try {
						queueReceiverChannel.basicRecover();
						queueReceiverChannel.close();
					} catch (Exception e) {
						Log.i("ERROR", "ERROR", e);
					}
					queueReceiverChannel = conn.createChannel();
					queueReceiverChannel.queueDeclare(MessagingService.ID, false, false, false, null);
					QueueingConsumer consumer = new QueueingConsumer(queueReceiverChannel);
					queueReceiverConsumerTag = queueReceiverChannel.basicConsume(MessagingService.ID, false, consumer);
					Log.i("info", "queueReceiver Running");
					while (true) {
						try {
							QueueingConsumer.Delivery delivery = consumer.nextDelivery();
							com.nvidia.MessagingService.Message msg = new com.nvidia.MessagingService.Message(delivery.getBody());
							Log.i("rabbitMQ", " [x] Received '" + msg + "'");
							MessagingService.nBuilder.setContentText("From " + msg.source + ":\n" + msg);
							MessagingService.nBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText("Received Message From " + msg.source + " at channel " + msg.channel + ":\n" + msg));
							MessagingService.nBuilder.setTicker(msg.toString());
							MessagingService.notificationManager.notify(MessagingService.notificationID++, MessagingService.nBuilder.build());
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
				try {
					channel = conn.createChannel();
					channel.exchangeDeclare(broadcastExchangeName, "fanout");
					String queueName = channel.queueDeclare().getQueue();
					channel.queueBind(queueName, broadcastExchangeName, "");

					QueueingConsumer consumer = new QueueingConsumer(channel);
					ConsumerTag = channel.basicConsume(queueName, true, consumer);
					Log.i("info", "BroadcastReceiver Running");
					while (true) {
						try {
							com.nvidia.MessagingService.Message msg = new com.nvidia.MessagingService.Message(consumer.nextDelivery().getBody());
							if (msg.source.equals(ID)) continue;
							if (msg.type == Enum.MessageType.binary) {
								processBinary(msg);
								continue;
							}
							Log.i("rabbitMQ", " [x] Received Broadcast'" + msg + "'");
							nBuilder.setContentText("Broadcast from " + msg.source + ":\n" + msg);
							nBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText("Received broadcast From " + msg.source + " at channel " + msg.channel + ":\n" + msg));
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
				}
			}
		};

		queueReceiver.start();
		broadcastReceiver.start();
	}

	static void connect() {
		if (running) return;
		running = true;

		connectingThread = new Thread() {
			public void run() {
				Log.w("Connect", "Start Connection");
				factory = new ConnectionFactory();
				factory.setConnectionTimeout(5000);
				factory.setAutomaticRecoveryEnabled(true);
				factory.setTopologyRecoveryEnabled(true);
				factory.setNetworkRecoveryInterval(10000);
				while (running) {
					try {
						factory.setUri("amqp://" + IP + ":5672");
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
							running = false;
							return;
						}
					}
				}
			}
		};
		connectingThread.start();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nBuilder = new NotificationCompat.Builder(this);
		nBuilder.setContentTitle("NVIDIA Messaging Service");
		nBuilder.setSmallIcon(R.mipmap.notification_icon);
		nBuilder.setVibrate(new long[]{500, 100});
		sp = getSharedPreferences("com.nvidia.MessagingService.sp", MODE_PRIVATE);
		IP = sp.getString("IP", "172.17.186.227");
		ID = sp.getString("ID", "defaultID");

	}

	@Override
	public IBinder onBind(Intent intent) {
		return messenger.getBinder();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		connect();
	}

	static class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == Enum.IPCmessageWhat.Send.ordinal()) {
				com.nvidia.MessagingService.Message pendingMsg = (com.nvidia.MessagingService.Message) msg.obj;
				if (pendingMsg.destination == null)
					try {
						sendChannel.basicPublish(broadcastExchangeName, "", null, pendingMsg.generateOneMsg());
					} catch (Exception e) {
						Log.e("ERROR", "ERROR", e);
					}
				else
					try {
						sendChannel.basicPublish("", pendingMsg.destination, null, pendingMsg.generateOneMsg());
					} catch (Exception e) {
						Log.e("ERROR", "ERROR", e);
					}
			} else if (msg.what == Enum.IPCmessageWhat.ChangeConnectionPreferences.ordinal()) {
				IP = ((String[]) msg.obj)[0];
				ID = ((String[]) msg.obj)[1];
				sp.edit().putString("IP", IP).putString("ID", ID).apply();
				try {
					if (connectingThread != null) connectingThread.interrupt();
					if (queueReceiver != null) queueReceiver.interrupt();
					if (queueReceiver != null) broadcastReceiver.interrupt();
					if (conn.isOpen()) conn.close();
				} catch (Exception e) {
					Log.i("Error", "Error", e);
				}
				running = false;
				connect();
			} else if (msg.what == Enum.IPCmessageWhat.CloseConnection.ordinal()) {
				try {
					if (connectingThread != null) connectingThread.interrupt();
					if (queueReceiver != null) queueReceiver.interrupt();
					if (queueReceiver != null) broadcastReceiver.interrupt();
					if (conn.isOpen()) conn.close();
				} catch (Exception e) {
					Log.i("Error", "Error", e);
				}
				running = false;
			}
		}
	}

	public static class BootBroadcastReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			Intent service = new Intent(context, MessagingService.class);
			context.startService(service);
		}

	}

}