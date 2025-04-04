package org.server;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.jws.WebService;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@WebService(endpointInterface = "org.server.ShareMarketServer")
public class ShareMarketServerImpl implements ShareMarketServer{

    private final String city;
    private final int udpPort;
    private final Map<String, Map<String, Share>> shareDatabase = new HashMap<>();
    private final Map<String, Integer> remoteServers = new HashMap<>();
    private final Map<String, Map<String, Integer>> buyerHoldings = new HashMap<>();

    public ShareMarketServerImpl(String city, int udpPort) {
        this.city = city;
        this.udpPort = udpPort;
        initializeShareTypes();
    }

    private void initializeShareTypes() {
        shareDatabase.put("Equity", new HashMap<>());
        shareDatabase.put("Bonus", new HashMap<>());
        shareDatabase.put("Dividend", new HashMap<>());
    }

    public void addRemoteServer(String city, int port) {
        remoteServers.put(city, port);
    }

    public Map<String, Map<String, Share>> getShareDatabase() {
        return this.shareDatabase;
    }

    public Map<String, Map<String, Integer>> getBuyerHoldings() {
        return this.buyerHoldings;
    }

    private void logAction(String requestType, String requestParams, boolean success) {
        try {
            FileWriter writer = new FileWriter("logs" + city + "_Server.log", true);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.write("[" + timestamp + "] " + requestType + " | Params: " + requestParams + " | Status: " + (success ? "Successfully Completed" : "Failed") + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized String addShare(String shareID, String shareType, int availableCapacity) {
        shareDatabase.putIfAbsent(shareType, new HashMap<>());
        if (shareDatabase.get(shareType).containsKey(shareID)) {
            logAction("Add Share", "ShareID: " + shareID + ", ShareType: " + shareType, false);
            return "Share already exists with ID " + shareID + " and Type " + shareType;
        }

        if (shareType.equalsIgnoreCase("equity") || shareType.equalsIgnoreCase("bonus") || shareType.equalsIgnoreCase("dividend")) {
            shareDatabase.get(shareType).put(shareID, new Share(shareID, shareType, availableCapacity, city));
            logAction("Add Share", "ShareID: " + shareID + ", ShareType: " + shareType + ", Capacity: " + availableCapacity, true);
            return "Share added successfully: " + shareType + "-" + shareID;
        }

        logAction("Add Share", "ShareID: " + shareID + " , ShareType: "+ shareType, false);
        return "Share not added: "+shareType + "-" +shareID;
    }

    @Override
    public synchronized String getShares(String buyerID) {
        StringBuilder result = new StringBuilder();
        if (buyerHoldings.containsKey(buyerID) && !buyerHoldings.get(buyerID).isEmpty()) {
            for (Map.Entry<String, Integer> entry : buyerHoldings.get(buyerID).entrySet()) {
                String[] shareParts = entry.getKey().split("-");
                String shareType = shareParts[0];
                String shareID = shareParts[1];
                int count = entry.getValue();
                result.append("Share: ").append(shareID)
                        .append(", Type: ").append(shareType)
                        .append(", Available: ").append(count).append("\n");
            }
            return result.toString().trim();
        }
        return "No shares found.";
    }


    @Override
    public synchronized String purchaseShare(String buyerID, String shareID, String shareType, int quantity) {
        if (!shareDatabase.containsKey(shareType) || !shareDatabase.get(shareType).containsKey(shareID)) {
            logAction("Purchase Share", "buyerID: " + buyerID + ", shareID: " + shareID + ", shareType: " + shareType + ", quantity: " + quantity, false);
            return "Share not available: " + shareType + "-" + shareID;
        }
        Share share = shareDatabase.get(shareType).get(shareID);
        if (share.getAvailableCapacity() < quantity) {
            logAction("Purchase Share", "buyerID: " + buyerID + ", shareID: " + shareID + ", shareType: " + shareType + ", quantity: " + quantity, false);
            return "Not enough shares available for " + shareType + "-" + shareID;
        }
        // Update share and buyer holdings
        synchronized (shareDatabase) {
            synchronized (buyerHoldings) {
                share.reduceCapacity(quantity);
                String uniqueKey = shareType + "-" + shareID;
                buyerHoldings.putIfAbsent(buyerID, new HashMap<>());
                buyerHoldings.get(buyerID).put(uniqueKey, buyerHoldings.get(buyerID).getOrDefault(uniqueKey, 0) + quantity); // Accumulate quantity
                logAction("Purchase Share", "buyerID: " + buyerID + ", shareID: " + shareID + ", shareType: " + shareType + ", quantity: " + quantity, true);
            }
        }
        return buyerID + " successfully purchased " + quantity + " shares of " + shareType + "-" + shareID;
    }

    @Override
    public synchronized String removeShare(String shareID, String shareType) {
        if (!shareDatabase.containsKey(shareType) || !shareDatabase.get(shareType).containsKey(shareID)) {
            logAction("Remove Share", "ShareID: " + shareID + ", ShareType: " + shareType, false);
            return "Share not available: " + shareType + "-" + shareID;
        }
        shareDatabase.get(shareType).remove(shareID);
        logAction("Remove Share", "ShareID: " + shareID + ", ShareType: " + shareType, true);
        return "Share removed successfully: " + shareType + "-" + shareID;
    }

    private String sendUDPRequest(String server, int port, String message) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName("localhost");
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(packet);

            byte[] receiveBuffer = new byte[4096];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);
            return new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
        }
    }

    @Override
    public synchronized String sellShare(String buyerID, String shareID, String shareType, int quantity) {
        if (!shareDatabase.containsKey(shareType) || !shareDatabase.get(shareType).containsKey(shareID)) {
            logAction("Sell Share", "buyerID: " + buyerID + ", shareID: " + shareID + ", quantity: " + quantity, false);
            return "Share not found: " + shareID;
        }

        String uniqueKey = shareType + "-" + shareID;
        if (!buyerHoldings.containsKey(buyerID) || !buyerHoldings.get(buyerID).containsKey(uniqueKey) || buyerHoldings.get(buyerID).get(uniqueKey) < quantity) {
            logAction("Sell Share", "buyerID: " + buyerID + ", shareID: " + shareID + ", quantity: " + quantity, false);
            return "Not enough shares to sell";
        }

        synchronized (shareDatabase) {
            synchronized (buyerHoldings) {
                // Update share capacity and buyer holdings
                Share share = shareDatabase.get(shareType).get(shareID);
                share.increaseCapacity(quantity);
                buyerHoldings.get(buyerID).put(uniqueKey, buyerHoldings.get(buyerID).get(uniqueKey) - quantity);
                if (buyerHoldings.get(buyerID).get(uniqueKey) == 0) {
                    buyerHoldings.get(buyerID).remove(uniqueKey);
                }

                logAction("Sell Share", "buyerID: " + buyerID + ", shareID: " + shareID + ", quantity: " + quantity, true);
            }
        }
        return buyerID + " successfully sold " + quantity + " shares of " + shareID;
    }

    @Override
    public synchronized String listShareAvailability(String shareType) {
        StringBuilder result = new StringBuilder();
        boolean shareFound = false;

        if (shareDatabase.containsKey(shareType)) {
            for (Share share : shareDatabase.get(shareType).values()) {
                result.append("Share: ").append(share.getShareID())
                        .append(", Type: ").append(share.getShareType())
                        .append(", Available: ").append(share.getAvailableCapacity())
                        .append(", Origin Market: ").append(share.getOriginMarket())
                        .append("\n");
                shareFound = true;
            }
        }

        if (!shareFound) {
            return "No shares of type " + shareType + " found.";
        }

        return result.toString().trim();
    }

    @Override
    public synchronized String purchaseRemoteShare(String buyerID, String shareID, String shareType,
                                                   int shareCount, String targetMarket) {
        try {
            // Determine the URL for the target market's web service
            String targetUrl = getWebServiceUrl(targetMarket);
            if (targetUrl == null) {
                logAction("Purchase Remote Share", "BuyerID: " + buyerID + ", ShareID: " + shareID +
                        ", Target: " + targetMarket, false);
                return "Purchase failed. Invalid target market.";
            }

            // Create a web service client for the remote server
            URL url = new URL(targetUrl);
            QName qname = new QName("http://server.org/", "ShareMarketServerImplService");
            Service service = Service.create(url, qname);
            ShareMarketServer remoteServer = service.getPort(ShareMarketServer.class);

            // Execute the purchase on the remote server
            String result = remoteServer.purchaseShare(buyerID, shareID, shareType, shareCount);

            // Log the cross-server transaction
            boolean success = result.startsWith(buyerID);  // Check if purchase was successful
            logAction("Purchase Remote Share", "BuyerID: " + buyerID + ", ShareID: " + shareID +
                    ", Target: " + targetMarket + ", Quantity: " + shareCount, success);

            return "Cross-server purchase: " + result;
        } catch (Exception e) {
            logAction("Purchase Remote Share", "BuyerID: " + buyerID + ", ShareID: " + shareID +
                    ", Target: " + targetMarket, false);
            return "Cross-server purchase failed: " + e.getMessage();
        }
    }

    private String getWebServiceUrl(String targetMarket) {
        // Map market names to their respective web service URLs
        Map<String, String> marketUrls = new HashMap<>();
        marketUrls.put("NewYork", "http://localhost:8080/ShareMarketService");
        marketUrls.put("London", "http://localhost:8081/ShareMarketService");
        marketUrls.put("Tokyo", "http://localhost:8082/ShareMarketService");

        return marketUrls.get(targetMarket);
    }

    @Override
    public synchronized String sellRemoteShare(String buyerID, String shareID, String shareType,
                                               int shareCount, String targetMarket) {
        try {
            // Determine the URL for the target market's web service
            String targetUrl = getWebServiceUrl(targetMarket);
            if (targetUrl == null) {
                logAction("Sell Remote Share", "BuyerID: " + buyerID + ", ShareID: " + shareID +
                        ", Target: " + targetMarket, false);
                return "Sell failed. Invalid target market.";
            }

            // Create a web service client for the remote server
            URL url = new URL(targetUrl);
            QName qname = new QName("http://server.org/", "ShareMarketServerImplService");
            Service service = Service.create(url, qname);
            ShareMarketServer remoteServer = service.getPort(ShareMarketServer.class);

            // Execute the sell on the remote server
            String result = remoteServer.sellShare(buyerID, shareID, shareType, shareCount);

            // Log the cross-server transaction
            boolean success = result.startsWith(buyerID);  // Check if sell was successful
            logAction("Sell Remote Share", "BuyerID: " + buyerID + ", ShareID: " + shareID +
                    ", Target: " + targetMarket + ", Quantity: " + shareCount, success);

            return "Cross-server sell: " + result;
        } catch (Exception e) {
            logAction("Sell Remote Share", "BuyerID: " + buyerID + ", ShareID: " + shareID +
                    ", Target: " + targetMarket, false);
            return "Cross-server sell failed: " + e.getMessage();
        }
    }

    @Override
    public synchronized String swapShares(String buyerID, String oldShareID, String oldShareType, String newShareID, String newShareType) {
        // Check if the buyer owns the old share
        String oldShareKey = oldShareType + "-" + oldShareID;
        if (!buyerHoldings.containsKey(buyerID) || !buyerHoldings.get(buyerID).containsKey(oldShareKey)) {
            logAction("Swap Shares", "buyerID: " + buyerID + ", oldShare: " + oldShareKey + ", newShare: " + newShareType + "-" + newShareID, false);
            return "Buyer does not own the share to be swapped";
        }

        int oldShareCount = buyerHoldings.get(buyerID).get(oldShareKey);

        // Check availability of the new share locally
        String localAvailability = checkSwapAvailability(newShareID, newShareType, oldShareCount);
        if (localAvailability.startsWith("AVAILABLE")) {
            // Execute the swap locally
            return executeLocalSwap(buyerID, oldShareID, oldShareType, newShareID, newShareType, oldShareCount);
        }

        // If not available locally, check remote servers
        for (Map.Entry<String, Integer> remoteServer : remoteServers.entrySet()) {
            String remoteCity = remoteServer.getKey();
            int remotePort = remoteServer.getValue();

            try {
                String remoteAvailability = sendUDPRequest(remoteCity, remotePort, "CHECK_SWAP_AVAILABILITY " + newShareID + " " + newShareType + " " + oldShareCount);
                if (remoteAvailability.startsWith("AVAILABLE")) {
                    // Execute the swap with the remote server
                    String swapResult = sendUDPRequest(remoteCity, remotePort, "EXECUTE_SWAP " + buyerID + " " + oldShareID + " " + oldShareType + " " + newShareID + " " + newShareType + " " + oldShareCount);
                    if (swapResult.startsWith("SUCCESS")) {
                        // Update local records
                        buyerHoldings.get(buyerID).remove(oldShareKey);
                        String newShareKey = newShareType + "-" + newShareID;
                        buyerHoldings.get(buyerID).put(newShareKey, oldShareCount);
                        logAction("Swap Shares", "buyerID: " + buyerID + ", oldShare: " + oldShareKey + ", newShare: " + newShareKey, true);
                        return "Successfully swapped " + oldShareCount + " shares of " + oldShareKey + " for " + newShareKey + " in " + remoteCity;
                    }
                }
            } catch (IOException e) {
                System.err.println("Error communicating with " + remoteCity + ": " + e.getMessage());
            }
        }

        logAction("Swap Shares", "buyerID: " + buyerID + ", oldShare: " + oldShareKey + ", newShare: " + newShareType + "-" + newShareID, false);
        return "Unable to swap shares. New share not available in any market.";
    }

    private String checkSwapAvailability(String shareID, String shareType, int requiredCount) {
        if (!shareDatabase.containsKey(shareType) || !shareDatabase.get(shareType).containsKey(shareID)) {
            return "NOT_AVAILABLE:Share not found";
        }

        Share share = shareDatabase.get(shareType).get(shareID);
        if (share.getAvailableCapacity() < requiredCount) {
            return "NOT_AVAILABLE:Not enough shares available. Required: " + requiredCount + ", Available: " + share.getAvailableCapacity();
        }

        return "AVAILABLE:Share available for swap";
    }

    private String executeLocalSwap(String buyerID, String oldShareID, String oldShareType, String newShareID, String newShareType, int shareCount) {
        // Remove old share from buyer's holdings
        String oldShareKey = oldShareType + "-" + oldShareID;
        buyerHoldings.get(buyerID).remove(oldShareKey);

        // Add new share to buyer's holdings
        String newShareKey = newShareType + "-" + newShareID;
        buyerHoldings.get(buyerID).put(newShareKey, shareCount);

        // Update share capacities
        shareDatabase.get(oldShareType).get(oldShareID).increaseCapacity(shareCount);
        shareDatabase.get(newShareType).get(newShareID).reduceCapacity(shareCount);

        logAction("Local Swap", "buyerID: " + buyerID + ", oldShare: " + oldShareKey + ", newShare: " + newShareKey, true);
        return "SUCCESS:Swapped " + shareCount + " shares of " + oldShareKey + " for " + newShareKey;
    }

}
