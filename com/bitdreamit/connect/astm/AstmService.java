package com.bitdreamit.connect.astm;

import com.bitdreamit.astm.asyncastm.AsyncAstmDriver;
import com.bitdreamit.astm.asyncastm.AsyncAstmSerialDriver;
import com.bitdreamit.astm.asyncastm.AsyncAstmTcpDriver;
import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;

public class AstmService {
    private AsyncAstmDriver driver;
    private static final Logger logger = Logger.getLogger(AstmService.class);

    public void init(AstmProperties props) {
        this.driver = createDriver(props);
    }

    private AsyncAstmDriver createDriver(AstmProperties props) {
        switch (props.getTransportMode()) {
            case SERIAL:
                return createSerialDriver(props);
            case TCP_SERVER:
                AsyncAstmTcpDriver serverDriver = new AsyncAstmTcpDriver(props.getPort(), true, props.getAstmProtocol());
                serverDriver.setCharset(props.getCharsetName());
                return serverDriver;
            case TCP_CLIENT:
            default:
                AsyncAstmTcpDriver clientDriver = new AsyncAstmTcpDriver(props.getHost(), props.getPort(), false, props.getAstmProtocol());
                clientDriver.setCharset(props.getCharsetName());
                return clientDriver;
        }
    }

    private AsyncAstmDriver createSerialDriver(AstmProperties props) {
        AsyncAstmSerialDriver driver = new AsyncAstmSerialDriver();

        driver.setPortName(props.getSerialPort());
        driver.setBaudRate(props.getBaudRate());
        driver.setDataBits(props.getDataBits());

        int stopBits;
        switch (props.getStopBits()) {
            case 1: stopBits = SerialPort.ONE_STOP_BIT; break;
            case 2: stopBits = SerialPort.TWO_STOP_BITS; break;
            default: stopBits = SerialPort.ONE_STOP_BIT; break;
        }
        driver.setStopBits(stopBits);

        int parity;
        switch (props.getParity()) {
            case 0: parity = SerialPort.NO_PARITY; break;
            case 1: parity = SerialPort.ODD_PARITY; break;
            case 2: parity = SerialPort.EVEN_PARITY; break;
            default: parity = SerialPort.NO_PARITY; break;
        }
        driver.setParity(parity);

        int flowControl;
        switch (props.getFlowControl()) {
            case 0: flowControl = SerialPort.FLOW_CONTROL_DISABLED; break;
            case 1: flowControl = SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED; break;
            case 2: flowControl = SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED | SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED; break;
            default: flowControl = SerialPort.FLOW_CONTROL_DISABLED; break;
        }
        driver.setFlowControl(flowControl);

        driver.setProtocol(props.getAstmProtocol());
        driver.setCharset(props.getCharsetName());

        return driver;
    }

    public void start() throws Exception {
        if (driver == null) throw new IllegalStateException("Driver not initialized. Call init() first.");
        driver.start();
    }

    public void stop() throws Exception {
        if (driver != null) driver.stop();
    }

    public AsyncAstmDriver getDriver() {
        return driver;
    }

    public boolean send(byte[] data) throws Exception {
        if (driver == null) throw new IllegalStateException("Driver not initialized");
        return driver.send(data);
    }
}