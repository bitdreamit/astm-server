package com.bitdreamit.connect.astm;

import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.server.api.MirthServlet;
import com.mirth.connect.server.controllers.ConfigurationController;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.ExtensionController;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

public class AstmServlet extends MirthServlet implements AstmServletInterface {
    public static final String PLUGIN_POINT = "ASTM Settings";
    private static final ExtensionController extensionController = ControllerFactory.getFactory().createExtensionController();
    ConfigurationController configurationController = ControllerFactory.getFactory().createConfigurationController();

    public AstmServlet(@Context HttpServletRequest request, @Context SecurityContext sc) {
        super(request, sc, "ASTM Settings");
    }

    public Map<String, Object> getStatusMap() throws ClientException {
        String serverUUID = this.configurationController.getServerId();
        Map<String, Object> statusMap = new HashMap();
        return statusMap;
    }

    public void setLicense(byte[] license) throws ClientException {

    }

    public void removeLicense() throws ClientException {

    }
}
