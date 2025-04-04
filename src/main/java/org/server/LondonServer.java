package org.server;

import javax.xml.ws.Endpoint;

public class LondonServer {
    public static void main(String[] args) {
        try {
            ShareMarketServerImpl serverImpl = new ShareMarketServerImpl("London", 5001);

            // Publish the web service
            Endpoint.publish("http://localhost:8081/ShareMarketService", serverImpl);

            System.out.println("London ShareMarket Server ready at http://localhost:8081/ShareMarketService");

            serverImpl.addRemoteServer("NewYork", 5000);
            serverImpl.addRemoteServer("Tokyo", 5002);

            // Start UDP server thread
            UDPServerThread udpThread = new UDPServerThread(5001, serverImpl, serverImpl.getShareDatabase());
            udpThread.start();

        } catch (Exception e) {
            System.err.println("ERROR: " + e);
            e.printStackTrace(System.out);
        }
    }
}