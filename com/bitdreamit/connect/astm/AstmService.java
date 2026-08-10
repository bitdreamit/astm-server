/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Kimi AI (Moonshot AI) — MIT License
 * 
 * COMPLETE AstmService.java — Drop-in replacement for your existing service.
 * Adds Serial (RS-232) support alongside existing TCP modes.
 */
package com.bitdreamit.connect.astm;

import com.bitdreamit.astm.asyncastm.AsyncAstmSerialDriver;
import com.bitdreamit.astm.asyncastm.AsyncAstmTcpDriver;
import com.bitdreamit.astm.asyncastm.service.connection.AstmConnectionListener;
import com.bitdreamit.astm.asyncastm.service.connection.AstmConnectionManager;
import org.apache.log4j.Logger;

/**
 * AstmService — Manages ASTM connection lifecycle.
 * Supports: TCP Client, TCP Server, Serial (RS-232)
 */
public class AstmService {
    private static final Logger logger = Logger.getLogger(AstmService.class);

    private AstmConnectionManager connectionManager;
    private AstmConnectionListener connectionListener;
    private AstmProperties properties;
    private volatile boolean running = false;

    public AstmService() {}

    /**
     * Initialize the service with properties.
     */
    public void init(AstmProperties properties) {
        this.properties = properties;
        logger.info("AstmService initialized with transport mode: " + properties.getTransportMode());
    }

    /**
     * Start the ASTM connection.
     */
    public void start() throws Exception {
        if (properties == null) {
            throw new IllegalStateException("Properties not set. Call init() first.");
        }

        connectionManager = createConnection(properties);

        if (connectionListener != null) {
            connectionManager.setConnectionListener(connectionListener);
        }

        connectionManager.start();
        running = true;
        logger.info("AstmService started successfully");
    }

    /**
     * Stop the ASTM connection.
     */
    public void stop() throws Exception {
        running = false;
        if (connectionManager != null) {
            connectionManager.stop();
        }
        logger.info("AstmService stopped");
    }

    /**
     * Send data through the connection.
     */
    public boolean send(byte[] data) throws Exception {
        if (!running || connectionManager == null) {
            throw new IllegalStateException("Service not running");
        }
        return connectionManager.send(data);
    }

    /**
     * Receive data from the connection.
     */
    public byte[] receive() throws Exception {
        if (!running || connectionManager == null) {
            throw new IllegalStateException("Service not running");
        }
        return connectionManager.receive();
    }

    /**
     * Check if connected.
     */
    public boolean isConnected() {
        return running && connectionManager != null && connectionManager.isConnected();
    }

    /**
     * Set connection listener for callbacks.
     */
    public void setConnectionListener(AstmConnectionListener listener) {
        this.connectionListener = listener;
        if (connectionManager != null) {
            connectionManager.setConnectionListener(listener);
        }
    }

    // ========================================================================
    // FACTORY METHOD — Creates the correct driver based on transport mode
    // ========================================================================

    private AstmConnectionManager createConnection(AstmProperties props) {
        switch (props.getTransportMode()) {
            case SERIAL:
                return createSerialDriver(props);

            case TCP_SERVER:
                logger.info("Creating TCP Server driver on port: " + props.getPort());
                return new AsyncAstmTcpDriver(props.getPort(), true, props.getProtocol());

            case TCP_CLIENT:
            default:
                logger.info("Creating TCP Client driver to " + props.getHost() + ":" + props.getPort());
                return new AsyncAstmTcpDriver(props.getHost(), props.getPort(), false, props.getProtocol());
        }
    }

    /**
     * Creates and fully configures the Serial driver.
     * ALL properties are mapped — nothing is left as "..."
     */
    private AstmConnectionManager createSerialDriver(AstmProperties props) {
        logger.info("Creating Serial driver on port: " + props.getSerialPort()
            + " @ " + props.getBaudRate() + " baud"
            + ", protocol=" + props.getProtocol());

        AsyncAstmSerialDriver driver = new AsyncAstmSerialDriver();

        // ===== SERIAL PORT SETTINGS (ALL mapped) =====
        driver.setPortName(props.getSerialPort());
        driver.setBaudRate(props.getBaudRate());
        driver.setDataBits(props.getDataBits());

        // Stop bits mapping: 1=ONE, 2=ONE_POINT_FIVE, 3=TWO
        int stopBits;
        switch (props.getStopBits()) {
            case 2:  stopBits = com.fazecast.jSerialComm.SerialPort.ONE_POINT_FIVE_STOP_BITS; break;
            case 3:  stopBits = com.fazecast.jSerialComm.SerialPort.TWO_STOP_BITS; break;
            case 1:
            default: stopBits = com.fazecast.jSerialComm.SerialPort.ONE_STOP_BIT; break;
        }
        driver.setStopBits(stopBits);

        // Parity mapping: 0=NONE, 1=ODD, 2=EVEN, 3=MARK, 4=SPACE
        int parity;
        switch (props.getParity()) {
            case 1:  parity = com.fazecast.jSerialComm.SerialPort.ODD_PARITY; break;
            case 2:  parity = com.fazecast.jSerialComm.SerialPort.EVEN_PARITY; break;
            case 3:  parity = com.fazecast.jSerialComm.SerialPort.MARK_PARITY; break;
            case 4:  parity = com.fazecast.jSerialComm.SerialPort.SPACE_PARITY; break;
            case 0:
            default: parity = com.fazecast.jSerialComm.SerialPort.NO_PARITY; break;
        }
        driver.setParity(parity);

        // Flow control mapping: 0=NONE, 1=RTS_CTS, 2=XON_XOFF, 3=DSR_DTR
        int flowControl;
        switch (props.getFlowControl()) {
            case 1:  flowControl = com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_RTS_ENABLED
                                 | com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_CTS_ENABLED; break;
            case 2:  flowControl = com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED
                                 | com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED; break;
            case 3:  flowControl = com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_DSR_ENABLED
                                 | com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_DTR_ENABLED; break;
            case 0:
            default: flowControl = com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_DISABLED; break;
        }
        driver.setFlowControl(flowControl);

        driver.setCharsetName(props.getCharsetName());
        driver.setReadTimeout(props.getReadTimeout());
        driver.setWriteTimeout(props.getWriteTimeout());

        // ===== ASTM PROTOCOL SETTINGS (ALL mapped) =====
        driver.setUseEnqAck(props.isUseEnqAck());
        driver.setUseChecksum(props.isUseChecksum());
        driver.setMaxRetries(props.getMaxRetries());
        driver.setMaxFrameSize(props.getMaxFrameSize());
        driver.setInterFrameDelay(props.getInterFrameDelay());

        logger.info("Serial driver fully configured: "
            + "port=" + props.getSerialPort()
            + ", baud=" + props.getBaudRate()
            + ", dataBits=" + props.getDataBits()
            + ", stopBits=" + props.getStopBits()
            + ", parity=" + props.getParity()
            + ", flow=" + props.getFlowControl()
            + ", ENQ/ACK=" + props.isUseEnqAck()
            + ", checksum=" + props.isUseChecksum()
            + ", retries=" + props.getMaxRetries()
            + ", frameSize=" + props.getMaxFrameSize()
            + ", interFrameDelay=" + props.getInterFrameDelay());

        return driver;
    }
}