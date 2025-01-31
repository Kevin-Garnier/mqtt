package mqtt;

import java.util.Random;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.*;

public class MqttSubscriber {
	private static final String BROKER = "tcp://localhost:1883";
	private static final String TOPIC = "/home/Lyon/sido/#";
	private static final String[] SENSORS = {"dht20", "sht30"};
	private static final Logger LOGGER = Logger.getLogger(MqttSubscriber.class.getName());
	
	public static void main(String[] args) {
		try {
			MqttClient client = new MqttClient(BROKER, MqttClient.generateClientId());
			client.connect();
			LOGGER.info("Connected to MQTT broker");
			
			Random random = new Random();
			boolean sendTemperature = true;
			
			double value;
			String suffix;
			String topic;
			while(true) {
				for (String sensor : SENSORS) {
					if (sendTemperature) {
						value = 19.0 + (10.0 * random.nextDouble());
						suffix = "/value";
						topic = TOPIC + sensor + suffix;
					} else {
						value = 45.0 + (20.0 * random.nextDouble());
						suffix = "/value2";
						topic = TOPIC + sensor + suffix;
					}
				}
				sendTemperature = !sendTemperature;
				Thread.sleep(1000);
			}
		} catch (Exception e) {
			LOGGER.severe("Error occurred: " + e.getMessage());
		}
	}
}
