package mqtt;

import java.util.Random;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.*;

public class MqttClientPublisherSensors {
	private static final String BROKER = "tcp://localhost:1883";
	private static final String TOPIC = "/home/Lyon/sido/";
	private static final String[] SENSORS = {"dht20", "sht30"};
	private static final Logger LOGGER = Logger.getLogger(MqttClientPublisherSensors.class.getName());
	
	public static void main(String[] args) {
		try {
			MqttClient client = new MqttClient(BROKER, MqttClient.generateClientId());
			client.connect();
			LOGGER.info("Connected to MQTT broker");
			
			Random random = new Random();
			boolean sendTemperature = true;

			int i=0;
			while(i < 1000) {
				for (String sensor : SENSORS) {
					double value = sendTemperature ? 19.0 + (10.0 * random.nextDouble()) : 45.0 + (20.0 * random.nextDouble());
					String suffix = sendTemperature ? "/value" : "/value2";
					String topic = TOPIC + sensor + suffix;
					MqttMessage message = new MqttMessage(String.valueOf(value).getBytes());
					message.setQos(0);
					message.setRetained(true);
					client.publish(topic, message);
					LOGGER.info("Mqtt Client: sent " + value + " to " + topic);
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
