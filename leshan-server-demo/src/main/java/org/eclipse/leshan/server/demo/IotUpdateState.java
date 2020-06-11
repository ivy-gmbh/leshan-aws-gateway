package org.eclipse.leshan.server.demo;

import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IotUpdateState extends AWSIotMessage {

    private static final Logger LOG = LoggerFactory.getLogger(IotUpdateState.class);
    public IotUpdateState(String topic, AWSIotQos qos, String payload) {
        super(topic, qos, payload);

    }
    @Override
    public void onSuccess() {
        LOG.info("Published message: {} -> {}", this.getTopic(), this.getStringPayload());
    }

    @Override
    public void onFailure() {
        LOG.error("Failed to publish message: {} -> {}", this.getTopic(), this.getStringPayload());
    }

    @Override
    public void onTimeout() {
        LOG.error("Timeout sending message: {} -> {}", this.getTopic(), this.getStringPayload());
    }
}
