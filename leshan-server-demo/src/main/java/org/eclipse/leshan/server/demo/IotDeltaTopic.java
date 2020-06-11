package org.eclipse.leshan.server.demo;

import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.*;

import java.util.Iterator;

interface OnDelta {
    void onResourceDelta(
            Number objectId,
            Number objectInstanceId,
            Number resourceId,
            Boolean value
    );
}

public class IotDeltaTopic extends AWSIotTopic {
    private static final Logger LOG = LoggerFactory.getLogger(IotDeltaTopic.class);
    private final OnDelta onDelta;
    public IotDeltaTopic(String thingName, OnDelta onDelta) {
        super("$aws/things/?/shadow/update/delta".replace("?", thingName), AWSIotQos.QOS0);
        this.onDelta = onDelta;
    }

    @Override
    public void onMessage(AWSIotMessage message) {
        // called when a message is received
        LOG.info("{}: {}", message.getTopic(), message.getStringPayload());
        JSONObject payload = (JSONObject) new JSONTokener(message.getStringPayload()).nextValue();
        JSONObject state = payload.getJSONObject("state");
        Iterator<String> objectIds = state.keys();
        while(objectIds.hasNext()) {
            String objectId = objectIds.next();
            if (state.get(objectId) instanceof JSONObject) {
                JSONObject objectInstances = (JSONObject) state.get(objectId);
                Iterator<String> objectInstanceIds = objectInstances.keys();
                while(objectInstanceIds.hasNext()) {
                    String objectInstanceId = objectInstanceIds.next();
                    if (objectInstances.get(objectInstanceId) instanceof JSONObject) {
                        JSONObject resources = (JSONObject) objectInstances.get(objectInstanceId);
                        Iterator<String> resourceIds = resources.keys();
                        while(resourceIds.hasNext()) {
                            String resourceId = resourceIds.next();
                            if (resources.get(resourceId) instanceof Boolean) {
                                this.onDelta.onResourceDelta(
                                        Integer.parseInt(objectId),
                                        Integer.parseInt(objectInstanceId),
                                        Integer.parseInt(resourceId),
                                        (Boolean) resources.get(resourceId)
                                );
                            }
                        }
                    }
                }
            }
        }
    }
}
