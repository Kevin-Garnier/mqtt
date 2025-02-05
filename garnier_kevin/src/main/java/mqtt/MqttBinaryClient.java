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
 * MqttBinaryClient is a client for connecting to an MQTT broker and
 * publishing/subscribing to topics using the MQTT protocol.
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
     * Constructs an MqttBinaryClient with the specified client ID, broker URI, and
     * port.
     *
     * @param id        the client ID
     * @param brokerURI the broker URI
     * @param port      the port number
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

    /**
     * The main method to run the MqttBinaryClient.
     *
     * @param args command line arguments
     */
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
     * @throws UnknownHostException if the IP address of the host could not be
     *                              determined
     * @throws IOException          if an I/O error occurs when creating the socket
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
        packet.put(header).put(stringToMQTTFormat(id));
        if (will)
            packet.put(stringToMQTTFormat(willTopic)).put(stringToMQTTFormat(willMessage));
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

    /**
     * Decodes the CONNACK message from the broker.
     *
     * @param connack the CONNACK message
     * @return true if the connection is accepted, false otherwise
     */
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

    /**
     * Disconnects from the MQTT broker.
     */
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

    /**
     * Publishes a message to the specified topic.
     *
     * @param topic   the topic to publish to
     * @param message the message to publish
     */
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

    /**
     * Subscribes to a single topic.
     *
     * @param topic the topic to subscribe to
     */
    private void subscribe(String topic) {
        List<String> topics = new LinkedList<>();
        topics.add(topic);
        subscribe(topics);
    }

    /**
     * Subscribes to a list of topics.
     *
     * @param topics the list of topics to subscribe to
     */
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

    /**
     * Decodes the SUBACK message from the broker.
     *
     * @param suback the SUBACK message
     */
    private void suback_decode(byte[] suback) {
        log.info("Suback received for message ID: " + Byte.toUnsignedInt(suback[0]) + "\t"
                + Byte.toUnsignedInt(suback[1]));
    }

    /**
     * Logs the message bytes in hexadecimal format.
     *
     * @param message the message bytes
     */
    private void logMessageBytes(byte[] message) {
        StringBuilder sb = new StringBuilder();
        for (byte b : message) {
            sb.append(String.format("%02X ", b));
        }
        log.fine("Message bytes: " + sb.toString());
    }

    /**
     * Logs the message characters.
     *
     * @param message the message bytes
     */
    private void logMessageChars(byte[] message) {
        StringBuilder sb = new StringBuilder();
        for (byte b : message) {
            sb.append((char) b);
        }
        log.info("Message chars: " + sb.toString());
    }

    /**
     * Converts a string to MQTT format.
     *
     * @param data the string data
     * @return the MQTT formatted byte array
     */
    private byte[] stringToMQTTFormat(String data) {
        if (data.length() > MAX_STRING_LEN)
            throw new IllegalArgumentException("Length must be less than " + MAX_LENGTH);
        byte[] len = ByteBuffer.allocate(4).putInt(data.length()).array();
        return ByteBuffer.allocate(data.length() + 2)
                .put(len[2]).put(len[3]).put(data.getBytes()).array();
    }

    /**
     * Encodes the remaining length field.
     *
     * @param length the length to encode
     * @return the encoded length as a list of bytes
     */
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

    /**
     * Creates the control header for the specified message type.
     *
     * @param messageType the message type
     * @return the control header byte
     */
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

    /**
     * Creates the CONNECT header.
     *
     * @param payload_length the payload length
     * @return the CONNECT header byte array
     */
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

    /**
     * Creates the PUBLISH header.
     *
     * @param payload_length the payload length
     * @param topic          the topic
     * @return the PUBLISH header byte array
     */
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

    /**
     * Creates the SUBSCRIBE header.
     *
     * @param payload_length the payload length
     * @return the SUBSCRIBE header byte array
     */
    private byte[] createSubscribeHeader(int payload_length) {
        ArrayList<Byte> varLength = remainingLength(payload_length + 2);

        ByteBuffer buffer = ByteBuffer.allocate(varLength.size() + 3)
                .put(createControlHeader(MessageType.SUBSCRIBE));
        varLength.forEach(b -> buffer.put(b));
        buffer.put((byte) random.nextInt(255)).put((byte) random.nextInt(255));
        return buffer.array();
    }

    /**
     * Receives and processes a PUBLISH message.
     *
     * @param message the PUBLISH message
     * @return the message ID
     */
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

    /**
     * Receives and processes a SUBACK message.
     *
     * @param message the SUBACK message
     */
    public void receive_suback(byte[] message) {
        synchronized (suback) {
            suback.set(true);
            suback.notifyAll();
        }

        log.info("Suback received for message ID: " + Byte.toUnsignedInt(message[0]) + "\t"
                + Byte.toUnsignedInt(message[1]));
    }

    /**
     * Receives and processes a PUBACK message.
     *
     * @param message the PUBACK message
     */
    public void receive_puback(byte[] message) {
        synchronized (puback) {
            puback.set(true);
            puback.notifyAll();
        }
        log.info("Received Puback for message ID: " + Byte.toUnsignedInt(message[0]) + "\t"
                + Byte.toUnsignedInt(message[1]));
    }

    /**
     * Sends a PUBREL message.
     *
     * @param message the message ID
     */
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

    /**
     * Sends a PUBACK message.
     *
     * @param mesId the message ID
     */
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

    /**
     * Decodes the remaining length field from the MQTT message.
     *
     * @param buffer the ByteBuffer containing the MQTT message
     * @return the decoded remaining length
     */
    private int decodeRemainingLength() throws IOException{
        int multiplier = 1;
        int value = 0;
        byte encodedByte;
        do {
            encodedByte = (byte) brokerSocket.getInputStream().read();
            value += (encodedByte & 127) * multiplier;
            if (multiplier > 128 * 128 * 128) {
                throw new IllegalArgumentException("Malformed Remaining Length");
            }
            multiplier *= 128;

        } while ((encodedByte & 128) != 0);
        return value;
    }


    /**
     * Sets the QoS level.
     *
     * @param qos the QoS level
     */
    public void setQos(int qos) {
        this.qos = qos;
    }

    /**
     * Sets the Will QoS level.
     *
     * @param will_qos the Will QoS level
     */
    public void setWill_qos(int will_qos) {
        this.will_qos = will_qos;
    }

    /**
     * Sets the clean session flag.
     *
     * @param cleanSession the clean session flag
     */
    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    /**
     * Sets the Will flag.
     *
     * @param will the Will flag
     */
    public void setWill(boolean will) {
        this.will = will;
    }

    /**
     * Sets the retain flag.
     *
     * @param retain the retain flag
     */
    public void setRetain(boolean retain) {
        this.retain = retain;
    }

    /**
     * Sets the duplicate flag.
     *
     * @param dup the duplicate flag
     */
    public void setDup(boolean dup) {
        this.dup = dup;
    }

    /**
     * Sets the password.
     *
     * @param password the password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets the username.
     *
     * @param username the username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Sets the keep-alive interval.
     *
     * @param keepAlive the keep-alive interval in seconds
     */
    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
    }

    /**
     * Checks if the client is connected to the broker.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Converts a byte to a MessageType.
     *
     * @param type the byte representing the message type
     * @return the MessageType
     */
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

    /**
     * Runnable class for receiving messages from the broker.
     */
    private class Recevier implements Runnable {
        @Override
        public void run() {
            while (isConnected()) {
                byte[] header = new byte[1];
                try {
                    int header_len = brokerSocket.getInputStream().read(header);
                    byte[] message = new byte[decodeRemainingLength()];
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

    /**
     * Runnable class for sending keep-alive messages to the broker.
     */
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
