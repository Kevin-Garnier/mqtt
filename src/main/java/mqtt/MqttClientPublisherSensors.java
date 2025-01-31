package mqtt;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.paho.client.mqttv3.*;

public class MqttClientPublisherSensors {
	private static final Logger LOGGER = Logger.getLogger(MqttClientPublisherSensors.class.getName());
	private static int COUNTER = 0;

	public static void main(String[] args) {
		String BROKER = "tcp://localhost:1883";
		String TOPIC = "/home/Lyon/sido/";
		int qos = 0;
		String[] SENSORS = {"dht20", "sht30"};
		String clientId = "myClientID_PubSensors";

		try {
			AtomicReference<Double> avgTemperature = new AtomicReference<>(0.0);
			AtomicReference<Double> avgHumidity = new AtomicReference<>(0.0);

			MqttClient client = new MqttClient(BROKER, clientId);
			MqttConnectOptions connectOptions = new MqttConnectOptions();
			connectOptions.setCleanSession(true);

			LOGGER.info("Mqtt Client: Connecting to Mqtt Broker running at: " + BROKER);
			client.connect(connectOptions);
			LOGGER.info("Mqtt Client: successfully Connected.");

			client.subscribe("/home/Lyon/sido/averages");

			client.setCallback(new MqttCallback() {
				@Override
				public void connectionLost(Throwable cause) {
					LOGGER.info("Connection lost because: " + cause);
					System.exit(1);
				}

				@Override
				public void messageArrived(String topic, MqttMessage message) throws Exception {
					String regex = "Average temperature: (\\d+\\.\\d+) and average humidity: (\\d+\\.\\d+)";
					Pattern p = Pattern.compile(regex);
					Matcher m = p.matcher(message.toString());
					if (m.find()) {
						avgTemperature.set(Double.parseDouble(m.group(1)));
						avgHumidity.set(Double.parseDouble(m.group(2)));
					}
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {
					LOGGER.info(clientId + " - Delivery complete");
				}
			});

			Random random = new Random();
			boolean sendTemperature = true;

			int i = 0;
			while (i < 1000) {
				for (String sensor : SENSORS) {
					double value = sendTemperature ? 19.0 + (10.0 * random.nextDouble()) : 45.0 + (20.0 * random.nextDouble());
					if (COUNTER > 100) {
						if (sendTemperature) {
							LOGGER.info("Mqtt Client: Difference between the temperature and the average temperature: " + (value - avgTemperature.get()));
						} else {
							LOGGER.info("Mqtt Client: Difference between the humidity and the average humidity: " + (value - avgHumidity.get()));
						}
					}
					String suffix = sendTemperature ? "/value" : "/value2";
					String topic = TOPIC + sensor + suffix;
					MqttMessage message = new MqttMessage(String.valueOf(value).getBytes());
					message.setQos(qos);
					message.setRetained(true);
					client.publish(topic, message);
					LOGGER.info("Mqtt Client: sent " + value + " to " + topic);
					COUNTER++;
				}
				sendTemperature = !sendTemperature;
				Thread.sleep(1000);
				i++;
			}
			client.disconnect();
		} catch (Exception e) {
			LOGGER.severe("Error occurred: " + e.getMessage());
		}
	}
}