package org.server;

import javax.xml.ws.Endpoint;

public class TokyoServer {
    public static void main(String[] args) {
        try {
            ShareMarketServerImpl serverImpl = new ShareMarketServerImpl("Tokyo", 5002);

            // Publish the web service
            Endpoint.publish("http://localhost:8082/ShareMarketService", serverImpl);

            System.out.println("Tokyo ShareMarket Server ready at http://localhost:8082/ShareMarketService");

            serverImpl.addRemoteServer("NewYork", 5000);
            serverImpl.addRemoteServer("London", 5001);

            // Start UDP server thread
            UDPServerThread udpThread = new UDPServerThread(5002, serverImpl, serverImpl.getShareDatabase());
            udpThread.start();

        } catch (Exception e) {
            System.err.println("ERROR: " + e);
            e.printStackTrace(System.out);
        }
    }
}