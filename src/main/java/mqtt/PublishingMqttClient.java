package mqtt;

import java.time.LocalDateTime;
import java.util.logging.Logger;

//added external jar: c:\ada\work\lectures\slr203\mqtt\paho\paho-java-maven\org.eclipse.paho.client.mqttv3-1.2.5.jar

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class PublishingMqttClient {//synchronous client
	private static final Logger LOGGER = Logger.getLogger(PublishingMqttClient.class.getName());

	public static void main(String[] args) {

		String topic        = "labs/new-topic";
	    String messageContent = "Message from my Lab's Paho Mqtt Client " + LocalDateTime.now();
	    int qos             = 0;
	    String brokerURI       = "tcp://localhost:1883";
	    String clientId     = "myClientID_Sub";
	    //MemoryPersistence persistence = new MemoryPersistence();


	    try(
	    	////instantiate a synchronous MQTT Client to connect to the targeted Mqtt Broker
	    	MqttClient mqttClient = new MqttClient(brokerURI, clientId);) {


	    	////specify the Mqtt Client's connection options
	    	MqttConnectOptions connectOptions = new MqttConnectOptions();
	    	//clean session
	    	connectOptions.setCleanSession(true);
	    	//customise other connection options here...
	    	//...

	    	////connect the mqtt client to the broker
	    	LOGGER.info("Mqtt Client: Connecting to Mqtt Broker running at: " + brokerURI);
	    	mqttClient.connect(connectOptions);
            LOGGER.info("Mqtt Client: sucessfully Connected.");

            ////publish a message
            LOGGER.info("Mqtt Client: Publishing message: " + messageContent);
            MqttMessage message = new MqttMessage(messageContent.getBytes());//instantiate the message including its content (payload)
            message.setQos(qos);//set the message's QoS
			message.setRetained(true); //set the retained flag
            mqttClient.publish(topic, message);//publish the message to a given topic
            LOGGER.info("Mqtt Client: successfully published the message.");

            ////disconnect the Mqtt Client
            mqttClient.disconnect();
            LOGGER.info("Mqtt Client: Disconnected.");


	    }
	    catch(MqttException e) {
	    	LOGGER.severe("Mqtt Exception reason: " + e.getReasonCode());
            LOGGER.severe("Mqtt Exception message: " + e.getMessage());
            LOGGER.severe("Mqtt Exception location: " + e.getLocalizedMessage());
            LOGGER.severe("Mqtt Exception cause: " + e.getCause());
            LOGGER.severe("Mqtt Exception reason: " + e);
            e.printStackTrace();
	    }

	}



}
