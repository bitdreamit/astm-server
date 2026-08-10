/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Kimi AI (Moonshot AI) — MIT License
 *
 * COMPLETE AstmReceiver.java — Fixed for Mirth 4.5.2 API
 */
package com.bitdreamit.connect.astm;

import com.bitdreamit.astm.asyncastm.service.connection.AstmConnectionListener;
import com.mirth.connect.donkey.model.message.RawMessage;
import com.mirth.connect.donkey.server.channel.DispatchResult;
import com.mirth.connect.donkey.server.channel.SourceConnector;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;

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

        // Set up listener to receive data asynchronously
        astmService.setConnectionListener(new AstmConnectionListener() {
            @Override
            public void onConnected() {
                logger.info("ASTM connection established");
            }

            @Override
            public void onDisconnected() {
                logger.info("ASTM connection disconnected");
            }

            @Override
            public void onDataReceived(byte[] data) {
                try {
                    String payload = new String(data, Charset.forName(properties.getCharsetName()));
                    dispatchRawMessage(new RawMessage(payload));
                } catch (Exception e) {
                    logger.error("Error dispatching received ASTM message", e);
                }
            }

            @Override
            public void onError(Exception e) {
                logger.error("ASTM receiver error", e);
            }
        });

        try {
            astmService.start();
            running = true;
            logger.info("AstmReceiver started with mode: " + properties.getTransportMode());
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

    // ======================================================================
    // FIX: Required by SourceConnector in Mirth 4.5.2
    // ======================================================================
    @Override
    public void handleRecoveredResponse(DispatchResult dispatchResult) {
        // No recovery handling needed for ASTM
    }
}