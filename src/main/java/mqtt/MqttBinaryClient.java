package mqtt;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * MqttBinaryClient is a simple MQTT client that connects to a broker, subscribes to topics, and publishes messages.
 */
public class MqttBinaryClient {
    private static Random random = new Random();
    private static final Logger log = Logger.getLogger(MqttBinaryClient.class.getName());
    private final int MAX_LENGTH = 268435455;
    private final int MAX_STRING_LEN = 65535;
    private final byte PROTOCOL_VERSION = 0x4;

    private int qos, will_qos;
    private boolean cleanSession, will, retain, dup;

    private String willTopic, willMessage;

    private String password, username;

    private int keepAlive;

    private String brokerURI;
    private int port;
    private Socket brokerSocket;
    private String id;
    private boolean connected;

    private AtomicBoolean suback, puback;

    private enum MessageType {
        CONNECT, CONNACK, PUBLISH, PUBACK, PUBREC, PUBREL, PUBCOMP, SUBSCRIBE, SUBACK, UNSUBSCRIBE, UNSUBACK, PINGREQ,
        PINGRESP, DISCONNECT, RESERVED
    }

    /**
     * Constructs a new MqttBinaryClient.
     *
     * @param id        the client ID
     * @param brokerURI the broker URI
     * @param port      the broker port
     */
    public MqttBinaryClient(String id, String brokerURI, int port) {
        qos = 0;
        will_qos = 0;
        cleanSession = false;
        will = false;
        retain = false;
        dup = false;
        password = null;
        username = null;
        brokerSocket = null;
        keepAlive = 0;
        willMessage = null;
        willTopic = null;
        this.brokerURI = brokerURI;
        this.port = port;
        this.id = id;
        connected = false;
        suback = new AtomicBoolean(false);
        puback = new AtomicBoolean(false);
    }

    public static void main(String[] args) {
        String brokerURI = "localhost";
        int port = 1883;
        String id = "PYTHON1";
        MqttBinaryClient mqttClient = new MqttBinaryClient(id, brokerURI, port);
        mqttClient.setCleanSession(true);
        mqttClient.setKeepAlive(60);
        mqttClient.setQos(1);
        try {
            mqttClient.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mqttClient.subscribe("/labs/new-topic");
        log.info("Subscribed to topic /labs/new-topic");
        mqttClient.publish("/labs/bin", "I see skies of blue");
        mqttClient.publish("/labs/bin", "And clouds of white");
        mqttClient.publish("/labs/bin", "The bright blessed day");
    }

    /**
     * Connects to the MQTT broker.
     *
     * @throws UnknownHostException if the broker URI is unknown
     * @throws IOException          if an I/O error occurs
     */
    public void connect() throws UnknownHostException, IOException {
        log.info("connecting to " + brokerURI + ":" + port);
        int payload_length = id.length() + 2;
        if (will)
            payload_length += willTopic.length() + willMessage.length() + 4;
        if (username != null)
            payload_length += username.length() + 2;
        if (password != null)
            payload_length += password.length() + 2;
        byte[] header = createConnectHeader(payload_length);

        ByteBuffer packet = ByteBuffer.allocate(header.length + payload_length);
        if (username != null)
            packet.put(stringToMQTTFormat(username));
        if (password != null)
            packet.put(stringToMQTTFormat(password));
        byte[] message = packet.array();
        logMessageBytes(message);
        brokerSocket = new Socket(brokerURI, port);
        brokerSocket.getOutputStream().write(packet.array());
        log.info(packet.array().toString());
        byte[] connack = new byte[4];
        int connack_len = brokerSocket.getInputStream().read(connack);
        log.info("Connack length : " + connack_len);
        connected = connack_decode(connack);
        if (connected) {
            new Thread(new Recevier()).start();
            new Thread(new keepAliveTask()).start();
        }

    }

    private boolean connack_decode(byte[] connack) {
        log.info("Session Present : " + (connack[2] == 1));
        switch (connack[3]) {
            case 0x0:
                log.info("Connection Accepted");
                return true;
            case 0x1:
                log.warning("Connection Refused, unacceptable protocol version");
                return false;
            case 0x2:
                log.warning("Connection Refused, identifier rejected");
                return false;
            case 0x3:
                log.warning("Connection Refused, Server unavailable");
                return false;
            case 0x4:
                log.warning("Connection Refused, bad user name or password");
                return false;
            case 0x5:
                log.warning("Connection Refused, not authorized");
                return false;
            default:
                log.warning("Connection Refused, unknown reason");
                return false;

        }

    }

    public void disconnect() {
        if (brokerSocket.isConnected()) {
            try {
                log.info("Disconnecting from broker");
                brokerSocket.getOutputStream().write(createControlHeader(MessageType.DISCONNECT));
                brokerSocket.close();
            } catch (IOException e) {
                log.warning("Error disconnecting from broker: " + e.getMessage());
            }
        }
    }

    public void publish(String topic, String message) {
        if (!brokerSocket.isConnected()) {
            log.warning("Not connected to broker");
            return;
        }
        synchronized (puback) {
            puback.set(false);
        }
        log.info("Publishing message to topic " + topic + " : " + message);
        int payload_length = message.length() + 2;
        byte[] header = createPublishHeader(payload_length, topic);
        ByteBuffer packet = ByteBuffer.allocate(header.length + payload_length);
        packet.put(header).put(message.getBytes());

        byte[] publish = packet.array();
        logMessageBytes(publish);
        try {
            brokerSocket.getOutputStream().write(publish);
        } catch (IOException e) {
            log.warning("Error publishing message: " + e.getMessage());
        }
        synchronized (puback) {
            while (!puback.get()) {
                try {
                    puback.wait();
                } catch (InterruptedException e) {
                    log.warning("Error waiting for Puback: " + e.getMessage());
                }
            }
        }
    }


    private void subscribe(String topic) {
        List<String> topics = new LinkedList<>();
        topics.add(topic);
        subscribe(topics);
    }

    private void subscribe(List<String> topics) {
        synchronized (suback) {
            suback.set(false);
        }
        StringBuilder sb = new StringBuilder();
        topics.forEach(t -> sb.append(t).append(" "));
        log.info("Subscribing to topics: " + sb.toString());
        final int[] tmp = { 0 };
        topics.forEach(t -> tmp[0] += t.length() + 3);
        int payload_length = tmp[0];
        byte[] header = createSubscribeHeader(payload_length);
        ByteBuffer packet = ByteBuffer.allocate(payload_length + header.length).put(header);
        topics.forEach(t -> packet.put(stringToMQTTFormat(t)).put((byte) qos));

        byte[] message = packet.array();
        logMessageBytes(message);
        try {
            brokerSocket.getOutputStream().write(message);
        } catch (IOException e) {
            log.warning("Error publishing message: " + e.getMessage());
        }
        synchronized (suback) {
            while (!suback.get()) {
                try {
                    suback.wait();
                } catch (InterruptedException e) {
                    log.warning("Error waiting for Suback: " + e.getMessage());
                }
            }
        }
    }

    private void suback_decode(byte[] suback) {
        log.info("Suback received for message ID: " + Byte.toUnsignedInt(suback[0]) + "\t"
                + Byte.toUnsignedInt(suback[1]));
    }

    private void logMessageBytes(byte[] message) {
        StringBuilder sb = new StringBuilder();
        for (byte b : message) {
            sb.append(String.format("%02X ", b));
        }
        log.fine("Message bytes: " + sb.toString());
    }

    private void logMessageChars(byte[] message) {
        StringBuilder sb = new StringBuilder();
        for (byte b : message) {
            sb.append((char) b);
        }
        log.info("Message chars: " + sb.toString());
    }

    private byte[] stringToMQTTFormat(String data) {
        if (data.length() > MAX_STRING_LEN)
            throw new IllegalArgumentException("Length must be less than " + MAX_LENGTH);
        byte[] len = ByteBuffer.allocate(4).putInt(data.length()).array();
        return ByteBuffer.allocate(data.length() + 2)
                .put(len[2]).put(len[3]).put(data.getBytes()).array();
    }

    private ArrayList<Byte> remainingLength(int length) {
        if (length > MAX_LENGTH) {
            throw new IllegalArgumentException("Length must be less than " + MAX_LENGTH);
        }
        ArrayList<Byte> encodedBytes = new ArrayList<>();
        do {
            byte encodedByte = (byte) (length % 128);
            length = length / 128;
            // if there are more data to encode, set the top bit of this byte
            if (length > 0) {
                encodedByte = (byte) (encodedByte | 128);
            }
            encodedBytes.add(encodedByte);
        } while (length > 0);
        return encodedBytes;
    }

    private byte createControlHeader(MessageType messageType) {
        BitSet bitSet = new BitSet(8);
        switch (messageType) {
            case CONNECT:// 0001
                bitSet.set(4);
                break;
            case PUBLISH:// 0011
                bitSet.set(5);
                bitSet.set(4);
                bitSet.set(3, dup);
                switch (qos) {
                    case 0:
                        break;
                    case 1:
                        bitSet.set(2);
                        break;
                    case 2:
                        bitSet.set(1);
                        break;
                }
                bitSet.set(0, retain);
                break;
            case PUBACK:// 0100
                bitSet.set(6);
                break;
            case PUBREC:// 0101
                bitSet.set(6);
                bitSet.set(4);
                break;
            case PUBREL:// 0110
                bitSet.set(6);
                bitSet.set(5);
                bitSet.set(1);

                break;
            case PUBCOMP:// 0111
                bitSet.set(6);
                bitSet.set(5);
                bitSet.set(4);
                break;
            case SUBSCRIBE:// 1000
                bitSet.set(7);
                bitSet.set(1);
                break;
            case UNSUBSCRIBE:// 1010
                bitSet.set(7);
                bitSet.set(5);
                break;
            case PINGREQ:// 1100
                bitSet.set(7);
                bitSet.set(6);
                break;
            case DISCONNECT:// 1110
                bitSet.set(7);
                bitSet.set(6);
                bitSet.set(5);
                break;
        }
        return bitSet.toByteArray()[0];
    }

    private byte[] createConnectHeader(int payload_length) {
        log.fine("Payload_length = " + payload_length);
        ArrayList<Byte> varLength = remainingLength(payload_length + 10);

        ByteBuffer buffer = ByteBuffer.allocate(11 + varLength.size());
        log.fine("Buffer size : " + (payload_length + 10));
        buffer.put(createControlHeader(MessageType.CONNECT));
        varLength.forEach(b -> buffer.put(b));
        buffer.put((byte) 0)
                .put((byte) 4)
                .put((byte) 'M')
                .put((byte) 'Q')
                .put((byte) 'T')
                .put((byte) 'T')
                .put(PROTOCOL_VERSION);
        BitSet connect_flags = new BitSet(8);
        connect_flags.set(1, cleanSession);
        connect_flags.set(2, will);
        switch (will_qos) {
            case 0:
                break;
            case 1:
                connect_flags.set(3);
                break;
            case 2:
                connect_flags.set(4);
                break;
        }
        connect_flags.set(5, retain);
        connect_flags.set(6, password != null);
        connect_flags.set(7, username != null);

        buffer.put(connect_flags.toByteArray());
        byte[] keepAlive_buf = ByteBuffer.allocate(4).putInt(keepAlive).array();
        buffer.put(keepAlive_buf[2]).put(keepAlive_buf[3]);
        return buffer.array();

    }

    private byte[] createPublishHeader(int payload_length, String topic) {
        ArrayList<Byte> varLength = remainingLength(payload_length + topic.length() + 2);
        log.fine("Payload_length = " + payload_length);
        ByteBuffer buffer = ByteBuffer.allocate(2 + varLength.size() + topic.length() + 3);
        log.fine("Buffer size : " + (topic.length() + varLength.size() + 2));
        buffer.put(createControlHeader(MessageType.PUBLISH));
        varLength.forEach(b -> buffer.put(b));
        buffer.put(stringToMQTTFormat(topic));
        byte[] message_id = ByteBuffer.allocate(4).putInt(random.nextInt()).array();
        log.fine("Message id : " + Byte.toUnsignedInt(message_id[2]) + "\t" + Byte.toUnsignedInt(message_id[3]));
        buffer.put(message_id[2]).put(message_id[3]);
        return buffer.array();
    }

    private byte[] createSubscribeHeader(int payload_length) {
        ArrayList<Byte> varLength = remainingLength(payload_length + 2);

        ByteBuffer buffer = ByteBuffer.allocate(varLength.size() + 3)
                .put(createControlHeader(MessageType.SUBSCRIBE));
        varLength.forEach(b -> buffer.put(b));
        buffer.put((byte) random.nextInt(255)).put((byte) random.nextInt(255));
        return buffer.array();
    }

    public byte[] receive_publish(byte[] message) {
        int topic_len = message[0] << 8 | message[1];
        if (topic_len == 0)
            return null;
        byte[] topic_raw = new byte[topic_len];
        System.arraycopy(message, 2, topic_raw, 0, topic_len);
        StringBuilder topic = new StringBuilder();
        for (byte b : topic_raw) {
            topic.append((char) b);
        }
        byte[] message_id = new byte[2];
        System.arraycopy(message, topic_len + 2, message_id, 0, 2);
        StringBuilder content = new StringBuilder();
        for (int i = topic_len + message_id.length; i < message.length; i++) {
            content.append((char) message[i]);
        }
        log.info("Received message on topic " + topic.toString() + " : " + content.toString());
        return message_id;
    }

    public void receive_suback(byte[] message) {
        synchronized (suback) {
            suback.set(true);
            suback.notifyAll();
        }

        log.info("Suback received for message ID: " + Byte.toUnsignedInt(message[0]) + "\t"
                + Byte.toUnsignedInt(message[1]));
    }

    public void receive_puback(byte[] message) {
        synchronized (puback) {
            puback.set(true);
            puback.notifyAll();
        }
        log.info("Received Puback for message ID: " + Byte.toUnsignedInt(message[0]) + "\t"
                + Byte.toUnsignedInt(message[1]));
    }

    public void send_pubrel(byte[] message) {
        byte[] pubrel = new byte[4];
        pubrel[0] = createControlHeader(MessageType.PUBREL);
        pubrel[1] = 2;
        pubrel[2] = message[0];
        pubrel[3] = message[1];
        try {
            brokerSocket.getOutputStream().write(pubrel);
        } catch (IOException e) {
            log.warning("Error sending Pubrel: " + e.getMessage());
        }
    }

    public void send_puback(byte[] mesId) {
        if (mesId == null)
            return;
        byte[] puback = new byte[4];
        puback[0] = createControlHeader(MessageType.PUBACK);
        puback[1] = 2;
        puback[2] = mesId[0];
        puback[3] = mesId[1];
        try {
            brokerSocket.getOutputStream().write(puback);
        } catch (IOException e) {
            log.warning("Error sending Puback: " + e.getMessage());
        }

    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public void setWill_qos(int will_qos) {
        this.will_qos = will_qos;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public void setWill(boolean will) {
        this.will = will;
    }

    public void setRetain(boolean retain) {
        this.retain = retain;
    }

    public void setDup(boolean dup) {
        this.dup = dup;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isConnected() {
        return connected;
    }

    private MessageType byteToMessageType(int type) {
        log.fine("Message type : " + type);
        switch (type) {
            case 1:
                return MessageType.CONNECT;
            case 2:
                return MessageType.CONNACK;
            case 3:
                return MessageType.PUBLISH;
            case 4:
                return MessageType.PUBACK;
            case 5:
                return MessageType.PUBREC;
            case 6:
                return MessageType.PUBREL;
            case 7:
                return MessageType.PUBCOMP;
            case 8:
                return MessageType.SUBSCRIBE;
            case 9:
                return MessageType.SUBACK;
            case 10:
                return MessageType.UNSUBSCRIBE;
            case 11:
                return MessageType.UNSUBACK;
            case 12:
                return MessageType.PINGREQ;
            case 13:
                return MessageType.PINGRESP;
            case 14:
                return MessageType.DISCONNECT;
            default:
                return MessageType.RESERVED;
        }
    }

    private class Recevier implements Runnable {
        @Override
        public void run() {
            while (isConnected()) {
                byte[] header = new byte[2];
                try {
                    int header_len = brokerSocket.getInputStream().read(header);
                    byte[] message = new byte[Byte.toUnsignedInt(header[1])];
                    MessageType messageType = byteToMessageType(Byte.toUnsignedInt(header[0]) >> 4);
                    int message_len = brokerSocket.getInputStream().read(message);
                    switch (messageType) {
                        case PUBLISH:
                            byte[] mesId = receive_publish(message);
                            send_puback(mesId);
                            break;
                        case PUBCOMP:
                        case PUBACK:
                            receive_puback(message);
                            break;
                        case SUBACK:
                            receive_suback(message);
                            break;
                        case PUBREC:
                            send_pubrel(message);
                        break;

                        default:
                            break;
                    }
                } catch (IOException e) {
                    log.warning("Error reading message: " + e.getMessage());
                }

            }
        }
    }

    private class keepAliveTask implements Runnable {
        @Override
        public void run() {
            while (isConnected()) {
                try {
                    byte[] mess = new byte[2];
                    mess[0] = createControlHeader(MessageType.PINGREQ);
                    mess[1] = 0;
                    brokerSocket.getOutputStream().write(mess);
                    Thread.sleep(keepAlive * 1000);
                } catch (IOException | InterruptedException e) {
                    log.warning("Error sending ping request: " + e.getMessage());
                }
            }
        }
    }

}
