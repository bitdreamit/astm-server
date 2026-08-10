package com.bitdreamit.connect.astm;

import com.bitdreamit.astm.asyncastm.AsyncAstmDriver;
import com.bitdreamit.astm.asyncastm.AsyncAstmSerialDriver;
import com.bitdreamit.astm.asyncastm.AsyncAstmTcpDriver;
import com.bitdreamit.astm.asyncastm.service.connection.Protocol;
import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;

public class AstmService {
    private static final Logger logger = Logger.getLogger(AstmService.class);

    private AstmProperties properties;
    private AsyncAstmDriver driver;

    // Original API: no-arg constructor
    public AstmService() {
    }

    // Original API: init method (called by AstmReceiver and AstmDispatcher)
    public void init(AstmProperties props) {
        this.properties = props;
        this.driver = createDriver(props);
    }

    public void start() throws Exception {
        if (driver != null) {
            driver.start();
        }
    }

    public void stop() throws Exception {
        if (driver != null) {
            driver.stop();
        }
    }

    public boolean send(byte[] data) throws Exception {
        return driver != null && driver.send(data);
    }

    public byte[] receive() throws Exception {
        return driver != null ? driver.receive() : new byte[0];
    }

    // Original API: getDriver (called by AstmReceiver)
    public AsyncAstmDriver getDriver() {
        return driver;
    }

    private AsyncAstmDriver createDriver(AstmProperties props) {
        switch (props.getTransportMode()) {
            case SERIAL:
                return createSerialDriver(props);
            case TCP_SERVER:
                // FIX: Pass charset and normalize protocol case
                return new AsyncAstmTcpDriver(
                        props.getPort(), true,
                        props.getAstmProtocol().trim().toUpperCase(),
                        props.getCharsetName());
            case TCP_CLIENT:
            default:
                // FIX: Pass charset and normalize protocol case
                return new AsyncAstmTcpDriver(
                        props.getHost(), props.getPort(), false,
                        props.getAstmProtocol().trim().toUpperCase(),
                        props.getCharsetName());
        }
    }

    private AsyncAstmDriver createSerialDriver(AstmProperties props) {
        AsyncAstmSerialDriver driver = new AsyncAstmSerialDriver();
        driver.setPortName(props.getSerialPort());
        driver.setBaudRate(props.getBaudRate());
        driver.setDataBits(props.getDataBits());

        int stopBits;
        switch (props.getStopBits()) {
            case 2:  stopBits = SerialPort.ONE_POINT_FIVE_STOP_BITS; break;
            case 3:  stopBits = SerialPort.TWO_STOP_BITS; break;
            default: stopBits = SerialPort.ONE_STOP_BIT; break;
        }
        driver.setStopBits(stopBits);

        int parity;
        switch (props.getParity()) {
            case 1:  parity = SerialPort.ODD_PARITY; break;
            case 2:  parity = SerialPort.EVEN_PARITY; break;
            case 3:  parity = SerialPort.MARK_PARITY; break;
            case 4:  parity = SerialPort.SPACE_PARITY; break;
            default: parity = SerialPort.NO_PARITY; break;
        }
        driver.setParity(parity);

        int flow;
        switch (props.getFlowControl()) {
            case 1:  flow = SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED; break;
            case 2:  flow = SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED | SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED; break;
            case 3:  flow = SerialPort.FLOW_CONTROL_DSR_ENABLED | SerialPort.FLOW_CONTROL_DTR_ENABLED; break;
            default: flow = SerialPort.FLOW_CONTROL_DISABLED; break;
        }
        driver.setFlowControl(flow);

        // FIX #6: Normalize protocol case
        driver.setProtocol(props.getAstmProtocol().trim().toUpperCase());
        driver.setCharset(props.getCharsetName());
        logger.info("Created Serial ASTM driver on " + props.getSerialPort());
        return driver;
    }
}