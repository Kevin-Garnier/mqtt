# MQTT Lab

## Exercice 4

### 4.1 Qos = 0 + clean session

Jan 30, 2025 8:07:23 PM mqtt.SubscribingMqttClient connect
INFO: Connecting to broker: tcp://localhost:1883

Mqtt Client: Connecting to Mqtt Broker running at: tcp://localhost:1883
Mqtt Client: sucessfully Connected.
Mqtt Client: Publishing message: Message from my Lab's Paho Mqtt Client
Mqtt Client: successfully published the message.
Mqtt Client: Disconnected.

Jan 30, 2025 8:14:13 PM mqtt.SubscribingMqttClient connect
INFO: Connecting to broker: tcp://localhost:1883

-> The message sent by the publisher aren't stored by the broker and tht's why the subscriber receive nothing

### 4.2 Qos = 0

Jan 30, 2025 8:21:12 PM mqtt.SubscribingMqttClient connect
INFO: Connecting to broker: tcp://localhost:1883

Mqtt Client: Connecting to Mqtt Broker running at: tcp://localhost:1883
Mqtt Client: sucessfully Connected.
Mqtt Client: Publishing message: Message from my Lab's Paho Mqtt Client
Mqtt Client: successfully published the message.
Mqtt Client: Disconnected.

Jan 30, 2025 8:22:20 PM mqtt.SubscribingMqttClient connect
INFO: Connecting to broker: tcp://localhost:1883


-> Same here

### 4.3 Qos = 1
Jan 30, 2025 8:26:10 PM mqtt.SubscribingMqttClient connect
INFO: Connecting to broker: tcp://localhost:1883

Mqtt Client: Connecting to Mqtt Broker running at: tcp://localhost:1883
Mqtt Client: sucessfully Connected.
Mqtt Client: Publishing message: Message from my Lab's Paho Mqtt Client 2025-01-30T20:26:52.752284817
Mqtt Client: successfully published the message.
Mqtt Client: Disconnected.

Jan 30, 2025 8:27:36 PM mqtt.SubscribingMqttClient connect
INFO: Connecting to broker: tcp://localhost:1883
Jan 30, 2025 8:27:37 PM mqtt.SubscribingMqttClient messageArrived
INFO: Message arrived from topic labs/paho-example-topic :
Content: Message from my Lab's Paho Mqtt Client 2025-01-30T20:26:52.752284817

-> The broker store the message in order to deliver at least once the message to the client

### 4.4 Qos = 1 + clean_session

Jan 30, 2025 8:29:46 PM mqtt.SubscribingMqttClient connect
INFO: Connecting to broker: tcp://localhost:1883

Mqtt Client: Connecting to Mqtt Broker running at: tcp://localhost:1883
Mqtt Client: sucessfully Connected.
Mqtt Client: Publishing message: Message from my Lab's Paho Mqtt Client 2025-01-30T20:29:37.697772995
Mqtt Client: successfully published the message.
Mqtt Client: Disconnected.

Jan 30, 2025 8:29:46 PM mqtt.SubscribingMqttClient connect
INFO: Connecting to broker: tcp://localhost:1883

-> Since clean_session was set to true, the broker drop the saved messages.

### 4.5

#### Qos = 1 subscriber , qos = 0 publisher

No message received after reconnection

#### Qos = 1 subscriber, qos = 2 publisher

Message received

#### Qos = 2

Message received

### 4.6

Only the last sent message was received on the reconnection

### 4.7

Here a gain only the last message is received.
