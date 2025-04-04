package org.server;

import javax.xml.ws.Endpoint;

public class NewYorkServer {
    public static void main(String[] args) {
        try {
            ShareMarketServerImpl serverImpl = new ShareMarketServerImpl("NewYork", 5000);

            // Publish the web service
            Endpoint.publish("http://localhost:8080/ShareMarketService", serverImpl);

            System.out.println("NewYork ShareMarket Server ready at http://localhost:8080/ShareMarketService");

            serverImpl.addRemoteServer("London", 5001);
            serverImpl.addRemoteServer("Tokyo", 5002);

            // Start UDP server thread
            UDPServerThread udpThread = new UDPServerThread(5000, serverImpl, serverImpl.getShareDatabase());
            udpThread.start();

        } catch (Exception e) {
            System.err.println("ERROR: " + e);
            e.printStackTrace(System.out);
        }
    }
}
