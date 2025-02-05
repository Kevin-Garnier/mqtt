# MQTT Lab

To compile the client, please use `mvn compile`
To start the client, please use `mvn exec:java@binary-client`


By default the client is configured to connect to localhost:1883
It will subscribe to the topic `/labs/new-topic` and will send messages to `/labs/bin`


## Demo

### Binary Client
Feb 05, 2025 11:35:09 AM mqtt.MqttBinaryClient connect
INFO: connecting to localhost:1883
Feb 05, 2025 11:35:09 AM mqtt.MqttBinaryClient connect
INFO: B@77176bae
Feb 05, 2025 11:35:09 AM mqtt.MqttBinaryClient connect
INFO: Connack length : 4
Feb 05, 2025 11:35:09 AM mqtt.MqttBinaryClient connack_decode
INFO: Session Present : false
Feb 05, 2025 11:35:09 AM mqtt.MqttBinaryClient connack_decode
INFO: Connection Accepted
Feb 05, 2025 11:35:09 AM mqtt.MqttBinaryClient subscribe
INFO: Subscribing to topics: /labs/new-topic
Feb 05, 2025 11:35:09 AM mqtt.MqttBinaryClient receive_publish
INFO: Received message on topic /labs/new-topic : Message from my Lab's Paho Mqtt Client 2025-02-05T11:34:19.533021687
Feb 05, 2025 11:35:09 AM mqtt.MqttBinaryClient receive_suback
INFO: Suback received for message ID: 43        154
Feb 05, 2025 11:35:09 AM mqtt.MqttBinaryClient main
INFO: Subscribed to topic /labs/new-topic
Feb 05, 2025 11:35:09 AM mqtt.MqttBinaryClient publish
INFO: Publishing message to topic /labs/bin : I see skies of blue
Feb 05, 2025 11:35:09 AM mqtt.MqttBinaryClient receive_puback
INFO: Received Puback for message ID: 166       134
Feb 05, 2025 11:35:09 AM mqtt.MqttBinaryClient publish
INFO: Publishing message to topic /labs/bin : And clouds of white
Feb 05, 2025 11:35:09 AM mqtt.MqttBinaryClient receive_puback
INFO: Received Puback for message ID: 107       59
Feb 05, 2025 11:35:09 AM mqtt.MqttBinaryClient publish
INFO: Publishing message to topic /labs/bin : The bright blessed day
Feb 05, 2025 11:35:09 AM mqtt.MqttBinaryClient receive_puback
INFO: Received Puback for message ID: 236       75
Feb 05, 2025 11:35:10 AM mqtt.MqttBinaryClient receive_publish
INFO: Received message on topic /labs/new-topic : Message from my Lab's Paho Mqtt Client 2025-02-05T11:35:09.880806722

### SubscriberClient (given in the lab)
Feb 05, 2025 11:36:20 AM mqtt.SubscribingMqttClient messageArrived
INFO: Message arrived from topic /labs/bin :
Content: I see skies of blue
Feb 05, 2025 11:36:20 AM mqtt.SubscribingMqttClient messageArrived
INFO: Message arrived from topic /labs/bin :
Content: And clouds of white
Feb 05, 2025 11:36:20 AM mqtt.SubscribingMqttClient messageArrived
INFO: Message arrived from topic /labs/bin :
Content: The bright blessed day
Feb 05, 2025 11:36:22 AM mqtt.SubscribingMqttClient messageArrived
INFO: Message arrived from topic /labs/new-topic :
Content: Message from my Lab's Paho Mqtt Client 2025-02-05T11:36:21.711605773

### PublisherClient

Feb 05, 2025 11:36:21 AM mqtt.PublishingMqttClient main
INFO: Mqtt Client: Connecting to Mqtt Broker running at: tcp://localhost:1883
Feb 05, 2025 11:36:22 AM mqtt.PublishingMqttClient main
INFO: Mqtt Client: sucessfully Connected.
Feb 05, 2025 11:36:22 AM mqtt.PublishingMqttClient main
INFO: Mqtt Client: Publishing message: Message from my Lab's Paho Mqtt Client 2025-02-05T11:36:21.711605773
Feb 05, 2025 11:36:22 AM mqtt.PublishingMqttClient main
INFO: Mqtt Client: successfully published the message.
Feb 05, 2025 11:36:22 AM mqtt.PublishingMqttClient main
INFO: Mqtt Client: Disconnected.

### Server
05/02/2025 11:36:20.150 com.scalagent.jorammq.mqtt.dump INFO [hawtdispatch-DEFAULT-3 MqttConnectionHandler.onTransportCommand(872)]: Frame by client 'PYTHON1' on tcp-1883 from /127.0.0.1:39300: CONNECT {qos: AT_MOST_ONCE, dup:false} Frame.buffers[0]<2/19/21>=[00, 04, 4D, 51, 54, 54, 04, 02, 00, 3C, 00, 07, 50, 59, 54, 48, 4F, 4E, 31, ]
05/02/2025 11:36:20.150 com.scalagent.jorammq.mqtt.connection INFO [hawtdispatch-DEFAULT-3 MqttConnectionHandler.endConnect(1848)]: Successful connection for PYTHON1 (null) from /127.0.0.1:39300
05/02/2025 11:36:20.166 com.scalagent.jorammq.mqtt.dump INFO [hawtdispatch-DEFAULT-1 MqttConnection.doSend(169)]: Frame by server for 'PYTHON1' to /127.0.0.1:39300: CONNACK {qos: AT_MOST_ONCE, dup:false} Frame.buffers[0]<0/2/2>=[00, 00, ]
05/02/2025 11:36:20.180 com.scalagent.jorammq.mqtt.dump INFO [hawtdispatch-DEFAULT-3 MqttConnectionHandler.onTransportCommand(872)]: Frame by client 'PYTHON1' on tcp-1883 from /127.0.0.1:39300: PINGREQ {qos: AT_MOST_ONCE, dup:false}
05/02/2025 11:36:20.181 com.scalagent.jorammq.mqtt.dump INFO [hawtdispatch-DEFAULT-3 MqttConnection.doSend(169)]: Frame by server for 'PYTHON1' to /127.0.0.1:39300: PINGRESP {qos: AT_MOST_ONCE, dup:false}
05/02/2025 11:36:20.182 com.scalagent.jorammq.mqtt.dump INFO [hawtdispatch-DEFAULT-3 MqttConnectionHandler.onTransportCommand(872)]: Frame by client 'PYTHON1' on tcp-1883 from /127.0.0.1:39300: SUBSCRIBE {qos: AT_LEAST_ONCE, dup:false} Frame.buffers[0]<2/20/22>=[85, 07, 00, 0F, 2F, 6C, 61, 62, 73, 2F, 6E, 65, 77, 2D, 74, 6F, 70, 69, 63, 01, ]
05/02/2025 11:36:20.186 com.scalagent.jorammq.mqtt.dump INFO [hawtdispatch-DEFAULT-1 MqttConnection.doSend(169)]: Frame by server for 'PYTHON1' to /127.0.0.1:39300: SUBACK {qos: AT_MOST_ONCE, dup:false} Frame.buffers[0]<0/3/3>=[85, 07, 01, ]
05/02/2025 11:36:20.239 com.scalagent.jorammq.mqtt.dump INFO [hawtdispatch-DEFAULT-3 MqttConnectionHandler.onTransportCommand(872)]: Frame by client 'PYTHON1' on tcp-1883 from /127.0.0.1:39300: - {qos: AT_MOST_ONCE, dup:false}
05/02/2025 11:36:20.240 com.scalagent.jorammq.mqtt.adapter.MqttConnectionHandler WARN [hawtdispatch-DEFAULT-3 MqttConnectionHandler.onSessionCommand(1325)]: Unexpected MQTT frame type: 0
05/02/2025 11:36:20.254 com.scalagent.jorammq.mqtt.dump INFO [hawtdispatch-DEFAULT-3 MqttConnectionHandler.onTransportCommand(872)]: Frame by client 'PYTHON1' on tcp-1883 from /127.0.0.1:39300: - {qos: AT_MOST_ONCE, dup:false}
05/02/2025 11:36:20.254 com.scalagent.jorammq.mqtt.adapter.MqttConnectionHandler WARN [hawtdispatch-DEFAULT-3 MqttConnectionHandler.onSessionCommand(1325)]: Unexpected MQTT frame type: 0
05/02/2025 11:36:20.270 com.scalagent.jorammq.mqtt.dump INFO [hawtdispatch-DEFAULT-3 MqttConnectionHandler.onTransportCommand(872)]: Frame by client 'PYTHON1' on tcp-1883 from /127.0.0.1:39300: - {qos: AT_MOST_ONCE, dup:false}
05/02/2025 11:36:20.270 com.scalagent.jorammq.mqtt.adapter.MqttConnectionHandler WARN [hawtdispatch-DEFAULT-3 MqttConnectionHandler.onSessionCommand(1325)]: Unexpected MQTT frame type: 0
05/02/2025 11:36:22.129 com.scalagent.jorammq.mqtt.dump INFO [hawtdispatch-DEFAULT-4 MqttConnectionHandler.onTransportCommand(872)]: Frame by client 'myClientID_Pub' on tcp-1883 from /127.0.0.1:39306: CONNECT {qos: AT_MOST_ONCE, dup:false} Frame.buffers[0]<2/26/28>=[00, 04, 4D, 51, 54, 54, 04, 02, 00, 3C, 00, 0E, 6D, 79, 43, 6C, 69, 65, 6E, 74, 49, 44, 5F, 50, 75, 62, ]
05/02/2025 11:36:22.129 com.scalagent.jorammq.mqtt.connection INFO [hawtdispatch-DEFAULT-4 MqttConnectionHandler.endConnect(1848)]: Successful connection for myClientID_Pub (null) from /127.0.0.1:39306
05/02/2025 11:36:22.137 com.scalagent.jorammq.mqtt.dump INFO [hawtdispatch-DEFAULT-1 MqttConnection.doSend(169)]: Frame by server for 'myClientID_Pub' to /127.0.0.1:39306: CONNACK {qos: AT_MOST_ONCE, dup:false} Frame.buffers[0]<0/2/2>=[00, 00, ]
05/02/2025 11:36:22.155 com.scalagent.jorammq.mqtt.dump INFO [hawtdispatch-DEFAULT-4 MqttConnectionHandler.onTransportCommand(872)]: Frame by client 'myClientID_Pub' on tcp-1883 from /127.0.0.1:39306: DISCONNECT {qos: AT_MOST_ONCE, dup:false}
05/02/2025 11:36:22.159 com.scalagent.jorammq.mqtt.connection INFO [hawtdispatch-DEFAULT-4 MqttConnectionHandler.onTransportCommand(921)]: Disconnection for myClientID_Pub (null)
05/02/2025 11:36:22.160 com.scalagent.jorammq.mqtt.connection INFO [hawtdispatch-DEFAULT-4 MqttConnectionHandler$16.run(2420)]: Connection closed for client 'myClientID_Pub' on tcp-1883 from /127.0.0.1:39306
05/02/2025 11:36:24.978 com.scalagent.jorammq.mqtt.connection INFO [hawtdispatch-DEFAULT-3 MqttConnectionHandler.terminate(2100)]: Disconnection for PYTHON1 (null)
05/02/2025 11:37:20.280 com.scalagent.jorammq.mqtt.dump INFO [hawtdispatch-DEFAULT-2 MqttConnectionHandler.onTransportCommand(872)]: Frame by client 'myClientID_Sub' on tcp-1883 from /127.0.0.1:59956: PINGREQ {qos: AT_MOST_ONCE, dup:false}
05/02/2025 11:37:20.280 com.scalagent.jorammq.mqtt.dump INFO [hawtdispatch-DEFAULT-2 MqttConnection.doSend(169)]: Frame by server for 'myClientID_Sub' to /127.0.0.1:59956: PINGRESP {qos: AT_MOST_ONCE, dup:false}
05/02/2025 11:38:20.281 com.scalagent.jorammq.mqtt.dump INFO [hawtdispatch-DEFAULT-2 MqttConnectionHandler.onTransportCommand(872)]: Frame by client 'myClientID_Sub' on tcp-1883 from /127.0.0.1:59956: PINGREQ {qos: AT_MOST_ONCE, dup:false}
05/02/2025 11:38:20.281 com.scalagent.jorammq.mqtt.dump INFO [hawtdispatch-DEFAULT-2 MqttConnection.doSend(169)]: Frame by server for 'myClientID_Sub' to /127.0.0.1:59956: PINGRES
