package com.bitdreamit.connect.astm;

import com.mirth.connect.donkey.server.channel.DispatchResult;
import com.mirth.connect.donkey.server.channel.SourceConnector;
import org.apache.log4j.Logger;

public class AstmReceiver extends SourceConnector {
    private static final Logger logger = Logger.getLogger(AstmReceiver.class);

    private AstmService astmService;
    private AstmProperties properties;
    private volatile boolean running = false;

    @Override
    public void onDeploy() {
        logger.info("AstmReceiver deployed");
    }

    @Override
    public void onUndeploy() {
        logger.info("AstmReceiver undeployed");
    }

    @Override
    public void onStart() {
        properties = (AstmProperties) getConnectorProperties();
        astmService = new AstmService();
        astmService.init(properties);

        try {
            astmService.start();
            running = true;
            logger.info("AstmReceiver started with mode: " + properties.getTransportMode());

            // Start the polling service thread
            AstmReceiverService receiverService = new AstmReceiverService(this, astmService.getDriver());
            Thread receiverThread = new Thread(receiverService);
            receiverThread.setName("AstmReceiver-" + getChannelId());
            receiverThread.start();

        } catch (Exception e) {
            logger.error("Failed to start ASTM receiver", e);
            throw new RuntimeException("ASTM receiver start failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void onStop() {
        running = false;
        try {
            if (astmService != null) {
                astmService.stop();
            }
        } catch (Exception e) {
            logger.error("Error stopping ASTM receiver", e);
        }
    }

    @Override
    public void onHalt() {
        running = false;
        try {
            if (astmService != null) {
                astmService.stop();
            }
        } catch (Exception e) {
            logger.error("Error halting ASTM receiver", e);
        }
    }

    @Override
    public void handleRecoveredResponse(DispatchResult dispatchResult) {
        // No recovery handling needed for ASTM
    }
}