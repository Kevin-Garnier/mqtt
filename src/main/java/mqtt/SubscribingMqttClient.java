package mqtt;

import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class SubscribingMqttClient implements MqttCallback {
    private MqttClient client;
    private String topic;
    private String clientId;

    Logger log = Logger.getLogger(SubscribingMqttClient.class.getName());

    public SubscribingMqttClient(String topic, String clientId) {
        this.topic = topic;
        this.clientId = clientId;
    }

    public static void main(String[] args) {

		String topic        = "/labs/bin";
	    String messageContent = "Message from my Lab's Paho Mqtt Client";
	    int qos             = 0;
	    String brokerURI       = "tcp://localhost:1883";
	    String clientId     = "myClientID_Sub";
	    //MemoryPersistence persistence = new MemoryPersistence();

        try {
            SubscribingMqttClient subscribingMqttcCient = new SubscribingMqttClient(topic, clientId);
            subscribingMqttcCient.connect(brokerURI, topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void connect(String Uri, String topic) throws MqttException {
            log.info("Connecting to broker: " + Uri);
            client = new MqttClient("tcp://localhost:1883", clientId);
            client.setCallback(this);

            //specify the Mqtt Client's connection options
	    	MqttConnectOptions connectOptions = new MqttConnectOptions();
	    	//clean session
	    	connectOptions.setCleanSession(false);


            client.connect(connectOptions);
            client.subscribe(topic);

    }

    @Override
    public void connectionLost(Throwable cause) {
        log.info("Connection lost because: " + cause);
        System.exit(1);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        log.info("Message arrived from topic " + topic +" : " + "\nContent: " + message.toString());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // log.info(clientId + " - Delivery complete");
    }
}
