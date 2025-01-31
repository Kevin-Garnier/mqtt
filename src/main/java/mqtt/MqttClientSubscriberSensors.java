package mqtt;

import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttClientSubscriberSensors implements MqttCallback {
    private MqttClient client;
    private String topic;
    private String clientId;
    private double sumTemperature;
    private double sumHumidity;

    private static int COUNTER = 0;
    Logger log = Logger.getLogger(MqttClientSubscriberSensors.class.getName());

    public MqttClientSubscriberSensors(String topic, String clientId) {
        this.topic = topic;
        this.clientId = clientId;
        this.sumTemperature = 0;
        this.sumHumidity = 0;
    }

    public static void main(String[] args) {

        String topic        = "/home/Lyon/sido/#";
        String messageContent = "Message from my Lab's Paho Mqtt Client";
        int qos             = 0;
        String brokerURI       = "tcp://localhost:1883";
        String clientId     = "myClientID_SubSensors";
        //MemoryPersistence persistence = new MemoryPersistence();

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

    public double getAverage(double number){
        return number / 100;
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        log.info("Message arrived from topic " + topic +" : " + "\nContent: " + message.toString());
        String value = topic.substring(topic.lastIndexOf("/") + 1);
        if (value.equals("value")) {
            sumTemperature += Double.parseDouble(message.toString());
        } else {
            sumHumidity += Double.parseDouble(message.toString());
        }
        COUNTER++;
        if (COUNTER % 100 == 0) {
            double avgTemp = getAverage(sumTemperature);
            double avgHum = getAverage(sumHumidity);
            String messageContent = "Average temperature: " + avgTemp + " and Humidity: " + avgHum;
            log.info(messageContent);
            MqttMessage msg = new MqttMessage(messageContent.getBytes());
            msg.setQos(0);
            msg.setRetained(true);
            String topicAvg = "/home/Lyon/sido/averages";
            client.publish(topicAvg, msg);
            sumTemperature = 0;
            sumHumidity = 0;
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        log.info(clientId + " - Delivery complete");
    }
}
