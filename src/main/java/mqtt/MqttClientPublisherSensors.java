package mqtt;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.paho.client.mqttv3.*;

public class MqttClientPublisherSensors implements MqttCallback {
	private static Random random = new Random();
	private static final Logger LOGGER = Logger.getLogger(MqttClientPublisherSensors.class.getName());
	private AtomicReference<Double> avgTemperature = new AtomicReference<>(0.0);
	private AtomicReference<Double> avgHumidity = new AtomicReference<>(0.0);
	private MqttClient client;
	private String[] sensors = {"dht22"};
	private int qos = 0;
	private String topic;
	private String clientId;

	public MqttClientPublisherSensors(String topic, String clientId) {
		this.topic = topic;
		this.clientId = clientId;
	}

	public static void main(String[] args) {
		String topic = "/home/Lyon/sido/";
		String clientId = "myClientID_PubSensors" + random.nextInt(1000);
		String brokerURI = "tcp://137.194.140.157:1883";

		try {
			MqttClientPublisherSensors mqttClientPublisherSensors = new MqttClientPublisherSensors(topic, clientId);
			mqttClientPublisherSensors.connect(brokerURI);
		} catch (MqttException e) {
			LOGGER.severe(e.getMessage());
		}

	}

	@Override
	public void connectionLost(Throwable cause) {
		LOGGER.info("Connection lost because: " + cause);
		System.exit(1);
	}

	private void connect(String brokerURI) throws MqttException {

		client = new MqttClient(brokerURI, clientId);
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		connectOptions.setCleanSession(true);

		LOGGER.info("Mqtt Client: Connecting to Mqtt Broker running at: " + brokerURI);
		client.connect(connectOptions);
		LOGGER.info("Mqtt Client: successfully Connected.");
		client.subscribe(topic+"averages/#");

		client.setCallback(this);

		new Thread(new DataSender()).start();
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
			if (topic.contains("temperature")) {
				String regex = "Average temperature: (\\d+\\.\\d+)";
				Pattern p = Pattern.compile(regex);
				Matcher m = p.matcher(message.toString());
				double t = m.find() ? Double.parseDouble(m.group(1)) : 0.0;
				LOGGER.info("Average temperature: " +t);
				avgTemperature.set(t);

			} else {
				String regex = "Average humidity: (\\d+\\.\\d+)";
				Pattern p = Pattern.compile(regex);
				Matcher m = p.matcher(message.toString());
				double h = m.find() ? Double.parseDouble(m.group(1)) : 0.0;
				LOGGER.info("Average humidity: " +h);
				avgHumidity.set(h);
			}
		}


	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// LOGGER.info(clientId + " - Delivery complete");
	};

	private class DataSender implements Runnable {

		@Override
		public void run() {
			Random random = new Random();
			boolean sendTemperature = true;

			while (true) {
				for (String sensor : sensors) {
					double value = sendTemperature ? 15.0 + (20.0 * random.nextDouble()) * (random.nextDouble() > 0.5 ? -1 : 1)
							: 45.0 + (20.0 * random.nextDouble()) * (random.nextDouble() > 0.5 ? -1 : 1);
					if (avgTemperature.get() != 0) {
						if (sendTemperature) {
							LOGGER.info("Mqtt Sensors: Difference between the temperature and the average temperature: "
									+ (value - avgTemperature.get()));
						} else {
							LOGGER.info("Mqtt Sensors: Difference between the humidity and the average humidity: "
									+ (value - avgHumidity.get()));
						}
					}
					String suffix = sendTemperature ? "/value" : "/value2";
					String data_topic = topic + sensor + suffix;
					MqttMessage message = new MqttMessage(String.valueOf(value).getBytes());
					message.setQos(qos);
					message.setRetained(true);
					try {
						client.publish(data_topic, message);
					} catch (MqttException e) {
						LOGGER.severe(e.getMessage());
					}
					LOGGER.info("Mqtt Sensors: sent " + value + " to " + data_topic);
				}
				sendTemperature = !sendTemperature;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					LOGGER.severe(e.getMessage());
				}
			}
		}

	}

}
