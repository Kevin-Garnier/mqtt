package mqtt;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttClientSubscriberSensors implements MqttCallback {
	private static Random random = new Random();
	private MqttClient client;
	private String topic;
	private String clientId;
	private Double sumTemperature;
	private Double sumHumidity;

	private Thread tempWriter, humWriter;

	private static AtomicInteger COUNTER_TEMP = new AtomicInteger(0);
	private static AtomicInteger COUNTER_HUM = new AtomicInteger(0);
	Logger log = Logger.getLogger(MqttClientSubscriberSensors.class.getName());

	public MqttClientSubscriberSensors(String topic, String clientId) {
		this.topic = topic;
		this.clientId = clientId;
		this.sumTemperature = new Double(0);
		this.sumHumidity = new Double(0);
		tempWriter = new Thread(new TemperatureWriterTask());
		humWriter = new Thread(new HumidityWriterTask());
	}

	public static void main(String[] args) {

		String topic = "/home/Lyon/sido/#";
		String messageContent = "Message from my Lab's Paho Mqtt Client";
		int qos = 0;
		String brokerURI = "tcp://localhost:1883";
		String clientId = "myClientID_SubSensors" + random.nextInt(1000);
		// MemoryPersistence persistence = new MemoryPersistence();

		try {
			MqttClientSubscriberSensors subscribingMqttClient = new MqttClientSubscriberSensors(topic, clientId);
			subscribingMqttClient.connect(brokerURI, topic);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	public void connect(String Uri, String topic) throws MqttException {
		log.info("Connecting to broker: " + Uri);
		client = new MqttClient("tcp://localhost:1883", clientId);
		client.setCallback(this);

		// specify the Mqtt Client's connection options
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		// clean session
		connectOptions.setCleanSession(false);

		client.connect(connectOptions);
		client.subscribe(topic);
		humWriter.start();
		tempWriter.start();

	}

	@Override
	public void connectionLost(Throwable cause) {
		log.info("Connection lost because: " + cause);
		System.exit(1);
	}

	public double getAverage(double sum, int n) {
		return sum / n;
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		// log.info("Message arrived from topic " + topic + " : " + "\nContent: " + message.toString());
		String value = topic.substring(topic.lastIndexOf("/") + 1);
		if (value.equals("value")) {
			// log.info("COUNTER_TEMP: " + COUNTER_TEMP);
			sumTemperature += Double.parseDouble(message.toString());
			COUNTER_TEMP.incrementAndGet();
		} else if (value.equals("value2")) {
			// log.info("COUNTER_HUM: " + COUNTER_HUM);
			sumHumidity += Double.parseDouble(message.toString());
			COUNTER_HUM.incrementAndGet();
		}
		synchronized (COUNTER_HUM) {
			if (COUNTER_HUM.get() > 10) {
				COUNTER_HUM.notifyAll();
			}
		}

		synchronized (COUNTER_TEMP) {
			if (COUNTER_TEMP.get() > 10)
				COUNTER_TEMP.notifyAll();
		}

	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// log.info(clientId + " - Delivery complete");
	}

	private class TemperatureWriterTask implements Runnable {

		private boolean sendTemperature = false;
		private double avgTemp;

		@Override
		public void run() {
			while (true) {
				synchronized (COUNTER_TEMP) {
					try {
						COUNTER_TEMP.wait(); // Wait for the condition to be met
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					if (COUNTER_TEMP.get() > 10) {
						avgTemp = getAverage(sumTemperature, COUNTER_TEMP.get());
						sumTemperature = 0.0;
						COUNTER_TEMP.set(0);
						sendTemperature = true;
					}
				}
				if (sendTemperature) {
					try {

						String messageContent = "Average temperature: " + avgTemp;
						log.info(messageContent);
						MqttMessage msg = new MqttMessage(messageContent.getBytes());
						msg.setQos(0);
						msg.setRetained(true);
						String topicAvg = "/home/Lyon/sido/averages/temperature";
						synchronized (client){
						client.publish(topicAvg, msg);
						}
					} catch (MqttException e) {
						e.printStackTrace();
					}
				}
				sendTemperature = false;
			}
		}
	}

	private class HumidityWriterTask implements Runnable {

		private boolean sendHumidity = false;
		private double avgHum;

		@Override
		public void run() {
			while (true) {
				synchronized (COUNTER_HUM) {
					try {
						COUNTER_HUM.wait(); // Wait for the condition to be met
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					if (COUNTER_HUM.get() > 10) {
						avgHum = getAverage(sumHumidity, COUNTER_HUM.get());
						sumHumidity = 0.0;
						COUNTER_HUM.set(0);
						sendHumidity = true;
					}
				}
				if (sendHumidity) {
					try {
						String messageContent = "Average humidity: " + avgHum;
						log.info(messageContent);
						MqttMessage msg = new MqttMessage(messageContent.getBytes());
						msg.setQos(0);
						msg.setRetained(true);
						String topicAvg = "/home/Lyon/sido/averages/humidity";
						synchronized (client) {
							client.publish(topicAvg, msg);
						}
					} catch (MqttException e) {
						e.printStackTrace();
					}
				}
				sendHumidity = false;
			}
		}
	}
}
