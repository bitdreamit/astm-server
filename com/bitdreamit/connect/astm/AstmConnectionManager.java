package com.bitdreamit.connect.astm;

import com.mirth.connect.donkey.model.channel.DeployedState;
import com.mirth.connect.donkey.model.event.ConnectionStatusEventType;
import com.mirth.connect.donkey.server.ConnectorTaskException;
import com.mirth.connect.donkey.server.StopException;
import com.mirth.connect.donkey.server.channel.Connector;
import com.mirth.connect.donkey.server.channel.DestinationConnector;
import com.mirth.connect.donkey.server.channel.SourceConnector;
import com.mirth.connect.donkey.server.event.ConnectionStatusEvent;
import com.mirth.connect.server.controllers.EventController;
import com.mirth.connect.server.util.TemplateValueReplacer;
import com.bitdreamit.astm.asyncastm.AsyncAstmTcpDriver;
import com.bitdreamit.astm.asyncastm.service.connection.Protocol;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmConnectionStatus;
import com.bitdreamit.astm.asyncastm.service.states.callback.AstmStatusCallback;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

/**
 * @deprecated This class is orphaned dead code. AstmReceiver and AstmDispatcher now use
 *             AstmService directly, which handles driver lifecycle internally.
 *             Kept for reference only — do not use in new code.
 */
@Deprecated
public class AstmConnectionManager {
    private Logger logger = Logger.getLogger(this.getClass());
    private AsyncAstmTcpDriver asyncAstm;
    private AstmProperties connectorProperties;
    private EventController eventController;
    private Connector connector;
    TemplateValueReplacer replacer = new TemplateValueReplacer();

    public AstmConnectionManager(final Connector connector, EventController eventController) {
        this.connector = connector;
        this.connectorProperties = (AstmProperties)connector.getConnectorProperties();
        this.eventController = eventController;
        AstmStatusCallback statusCallback = new AstmStatusCallback() {
            public void reportStatus(AstmConnectionStatus status) {
                switch (status) {
                    case CONNECTING:
                        if (AstmConnectionManager.this.connectorProperties.isServerMode()) {
                            AstmConnectionManager.this.logger.debug("Started ASTM server with address " + AstmConnectionManager.this.asyncAstm.getBindAddress() + ":" + AstmConnectionManager.this.asyncAstm.getListeningPort());
                            AstmConnectionManager.this.setConnectorEvent(ConnectionStatusEventType.IDLE, "Listening connections in " + AstmConnectionManager.this.asyncAstm.getBindAddress() + ":" + AstmConnectionManager.this.asyncAstm.getListeningPort());
                        } else {
                            AstmConnectionManager.this.logger.debug("Connecting ASTM server with address: " + AstmConnectionManager.this.asyncAstm.getDestinationAddress() + ":" + AstmConnectionManager.this.asyncAstm.getDestinationPort());
                            AstmConnectionManager.this.setConnectorEvent(ConnectionStatusEventType.CONNECTING, "Connecting to " + AstmConnectionManager.this.asyncAstm.getDestinationAddress() + ":" + AstmConnectionManager.this.asyncAstm.getDestinationPort());
                        }
                        break;
                    case DISCONNECTING:
                        if (AstmConnectionManager.this.connectorProperties.isServerMode()) {
                            AstmConnectionManager.this.setConnectorEvent(ConnectionStatusEventType.IDLE, "Disconnecting from client");
                        } else {
                            AstmConnectionManager.this.setConnectorEvent(ConnectionStatusEventType.IDLE, "Disconnecting from server");
                        }
                        break;
                    case EXITING:
                        AstmConnectionManager.this.logger.debug("ASTM Connector current state: " + connector.getCurrentState().toString());
                        if (connector.getCurrentState() != DeployedState.STOPPING) {
                            try {
                                connector.getChannel().stop();
                            } catch (StopException var3) {
                                StopException e = var3;
                                e.printStackTrace();
                            }
                        }
                        break;
                    case IDLE:
                        if (AstmConnectionManager.this.connector instanceof AstmReceiver) {
                            AstmConnectionManager.this.setConnectorEvent(ConnectionStatusEventType.CONNECTED, "Waiting for receiving messages");
                        } else {
                            AstmConnectionManager.this.setConnectorEvent(ConnectionStatusEventType.CONNECTED, "Waiting for sending messages");
                        }
                        break;
                    case RECEIVING:
                        if (AstmConnectionManager.this.connector instanceof AstmReceiver) {
                            AstmConnectionManager.this.setConnectorEvent(ConnectionStatusEventType.RECEIVING, "Receiving new message");
                        } else {
                            AstmConnectionManager.this.setConnectorEvent(ConnectionStatusEventType.IDLE, "Stopping message sending");
                        }
                        break;
                    case SENDING:
                        if (AstmConnectionManager.this.connector instanceof AstmDispatcher) {
                            AstmConnectionManager.this.setConnectorEvent(ConnectionStatusEventType.SENDING, "Sending new message");
                        } else {
                            AstmConnectionManager.this.setConnectorEvent(ConnectionStatusEventType.IDLE, "Stopping message receiving");
                        }
                        break;
                    case RECONNECTING:
                        AstmConnectionManager.this.setConnectorEvent(ConnectionStatusEventType.DISCONNECTED, "Trying to reconnect");
                        break;
                    case STARTING:
                        AstmConnectionManager.this.setConnectorEvent(ConnectionStatusEventType.INFO, "Starting ASTM driver");
                }

            }
        };
        this.asyncAstm = new AsyncAstmTcpDriver("AsyncAstm - " + connector.getChannel().getName(), statusCallback);
    }

    public void connect() throws ConnectorTaskException {
        Protocol astmProtocol = Protocol.ELECSYS;
        if (this.connectorProperties.getAstmProtocol().toUpperCase().equals("ELECSYS")) {
            astmProtocol = Protocol.ELECSYS;
        } else if (this.connectorProperties.getAstmProtocol().toUpperCase().equals("COBAS")) {
            astmProtocol = Protocol.COBAS;
        }

        try {
            if (this.connectorProperties.isServerMode()) {
                this.asyncAstm.listenConnections(this.getLocalPort(), this.getAddressBind(), astmProtocol);
            } else {
                this.asyncAstm.initiateConnection(this.getRemoteAddress(), this.getRemotePort(), astmProtocol);
            }

        } catch (Exception var5) {
            Exception e = var5;
            this.setConnectorEvent(ConnectionStatusEventType.FAILURE, e.getMessage());
            String connectionType = this.connectorProperties.isServerMode() ? "server" : "client";
            throw new ConnectorTaskException("Failed to start ASTM " + connectionType + " in connector " + this.getConnectorName(), e);
        }
    }

    public void disconnect() throws ConnectorTaskException {
        ConnectorTaskException firstCause = null;
        if (this.asyncAstm != null) {
            try {
                this.logger.debug("stopping ASTM connection");
                if (this.asyncAstm != null) {
                    this.asyncAstm.close();
                    this.asyncAstm = null;
                }
            } catch (Exception var3) {
                Exception e = var3;
                firstCause = new ConnectorTaskException("Unable to stop ASTM connection", e);
            }
        }

        if (firstCause != null) {
            throw firstCause;
        } else {
            this.setConnectorEvent(ConnectionStatusEventType.DISCONNECTED, "Disconnected");
        }
    }

    public AsyncAstmTcpDriver getAsyncAstm() {
        return this.asyncAstm;
    }

    private void setConnectorEvent(ConnectionStatusEventType event, String info) {
        this.eventController.dispatchEvent(new ConnectionStatusEvent(this.connector.getChannelId(), this.connector.getMetaDataId(), this.getConnectorName(), event, info));
    }

    private String getAddressBind() {
        if (this.connectorProperties.isAllInterfaces()) {
            return null;
        } else {
            String addressBind = this.replacer.replaceValues(this.connectorProperties.getAddressBind(), this.connector.getChannelId(), this.connector.getChannel().getName());
            return addressBind;
        }
    }

    private int getLocalPort() {
        String localPort = this.replacer.replaceValues(this.connectorProperties.getLocalPort(), this.connector.getChannelId(), this.connector.getChannel().getName());
        return NumberUtils.toInt(localPort);
    }

    private int getRemotePort() {
        String remotePort = this.replacer.replaceValues(this.connectorProperties.getRemotePort(), this.connector.getChannelId(), this.connector.getChannel().getName());
        return NumberUtils.toInt(remotePort);
    }

    private String getRemoteAddress() {
        String remoteAddress = this.replacer.replaceValues(this.connectorProperties.getRemoteAddress(), this.connector.getChannelId(), this.connector.getChannel().getName());
        return remoteAddress;
    }

    private String getConnectorName() {
        if (this.connector instanceof SourceConnector) {
            return ((SourceConnector)this.connector).getSourceName();
        } else if (this.connector instanceof DestinationConnector) {
            return ((DestinationConnector)this.connector).getDestinationName();
        } else {
            throw new RuntimeException("Connector of unknown type");
        }
    }
}
