package com.bitdreamit.connect.astm;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.message.ConnectorMessage;
import com.mirth.connect.donkey.model.message.MessageContent;
import com.mirth.connect.donkey.model.message.Response;
import com.mirth.connect.donkey.model.message.Status;
import com.mirth.connect.donkey.server.channel.DestinationConnector;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;

public class AstmDispatcher extends DestinationConnector {
    private static final Logger logger = Logger.getLogger(AstmDispatcher.class);

    private AstmService astmService;

    @Override
    public void onDeploy() {
        logger.info("AstmDispatcher deployed");
    }

    @Override
    public void onUndeploy() {
        logger.info("AstmDispatcher undeployed");
    }

    @Override
    public void onStart() {
        try {
            astmService = new AstmService();
            astmService.init((AstmProperties) getConnectorProperties());
            astmService.start();
            logger.info("AstmDispatcher started");
        } catch (Exception e) {
            logger.error("Failed to start ASTM dispatcher", e);
            throw new RuntimeException("ASTM dispatcher start failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void onStop() {
        try {
            if (astmService != null) {
                astmService.stop();
            }
        } catch (Exception e) {
            logger.error("Error stopping ASTM dispatcher", e);
        }
    }

    @Override
    public void onHalt() {
        try {
            if (astmService != null) {
                astmService.stop();
            }
        } catch (Exception e) {
            logger.error("Error halting ASTM dispatcher", e);
        }
    }

    @Override
    public Response send(ConnectorProperties connectorProperties, ConnectorMessage message) {
        try {
            AstmProperties props = (AstmProperties) connectorProperties;
            MessageContent encoded = message.getEncoded();
            String payload = encoded != null ? encoded.getContent() : "";
            byte[] data = payload.getBytes(Charset.forName(props.getCharsetName()));

            boolean sent = astmService.send(data);
            if (sent) {
                return new Response(String.valueOf(Status.SENT));
            } else {
                return new Response(Status.ERROR, "ASTM send returned false");
            }
        } catch (Exception e) {
            logger.error("ASTM dispatch error", e);
            return new Response(Status.ERROR, e.getMessage());
        }
    }

    @Override
    public void replaceConnectorProperties(ConnectorProperties connectorProperties, ConnectorMessage message) {
        // No dynamic property replacement needed for ASTM
    }
}
