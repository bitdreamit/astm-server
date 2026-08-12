package com.bitdreamit.connect.astm;

import com.bitdreamit.astm.asyncastm.AsyncAstmDriver;
import com.bitdreamit.astm.asyncastm.service.states.bundle.ReceivedMessage;
import com.bitdreamit.astm.asyncastm.service.states.bundle.TransmissionResult.Status;
import com.mirth.connect.donkey.model.channel.DeployedState;
import com.mirth.connect.donkey.model.event.ErrorEventType;
import com.mirth.connect.donkey.model.message.RawMessage;
import com.mirth.connect.donkey.server.channel.DispatchResult;
import com.mirth.connect.donkey.server.event.ErrorEvent;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.EventController;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AstmReceiverService implements Runnable {
    private Logger logger = Logger.getLogger(this.getClass());
    AstmReceiver source;
    AsyncAstmDriver asyncAstm;
    EventController eventController = ControllerFactory.getFactory().createEventController();

    public AstmReceiverService(AstmReceiver source, AsyncAstmDriver asyncAstm) {
        this.source = source;
        this.asyncAstm = asyncAstm;
    }

    public void run() {
        try {
            while (this.source.getCurrentState() == DeployedState.STARTED) {
                try {
                    Map<String, Object> sourceMap = new HashMap<>();
                    ReceivedMessage received = this.asyncAstm.getReceivedMessage();

                    if (received.getResult().getStatus() == Status.SUCCESS) {
                        DispatchResult dispatchResult = null;
                        try {
                            dispatchResult = this.source.dispatchRawMessage(
                                    new RawMessage(received.getMessage(), (Collection) null, sourceMap));
                        } catch (Exception dispatchEx) {
                            // IMPROVED: Catch per-message dispatch failures so the listener thread survives.
                            // Mirth queue full, database timeout, or script error should not kill the listener.
                            this.logger.error("Failed to dispatch ASTM message to Mirth channel. Message dropped.", dispatchEx);
                        } finally {
                            if (dispatchResult != null) {
                                try {
                                    this.source.finishDispatch(dispatchResult);
                                } catch (Exception e) {
                                    this.logger.error("Error finishing dispatch", e);
                                }
                            }
                        }
                    } else {
                        Exception exception = new Exception(
                                received.getResult().getStatus().name() + ": " + received.getResult().getDescription());
                        StringBuilder receivedStr = new StringBuilder();
                        receivedStr.append("\nRecovered message:\n");
                        receivedStr.append(received.getMessage());
                        this.logger.error("ASTM Receiver exception (channel: "
                                + this.source.getChannel().getName() + ")" + receivedStr.toString(), exception);
                        this.eventController.dispatchEvent(new ErrorEvent(
                                this.source.getChannelId(), this.source.getMetaDataId(),
                                (Long) null, ErrorEventType.SOURCE_CONNECTOR,
                                this.source.getSourceName(),
                                this.source.getConnectorProperties().getName(),
                                "Error receiving ASTM message", exception));
                    }
                } catch (InterruptedException ie) {
                    this.logger.debug("Received interruption from driver when waiting for incoming messages");
                    Thread.currentThread().interrupt();
                    break;
                } catch (RuntimeException ex) {
                    this.logger.error("Recoverable error processing ASTM message. Continuing to listen.", ex);
                }
            }
        } catch (Exception fatal) {
            this.logger.error("ASTM Listener fatal exception. ASTM Listener from channel "
                    + this.source.getChannel().getName() + " has been disabled", fatal);
            this.eventController.dispatchEvent(new ErrorEvent(
                    this.source.getChannelId(), this.source.getMetaDataId(),
                    (Long) null, ErrorEventType.SOURCE_CONNECTOR,
                    this.source.getSourceName(),
                    this.source.getConnectorProperties().getName(),
                    "Fatal Error in ASTM Listener", fatal));
        }
    }
}
