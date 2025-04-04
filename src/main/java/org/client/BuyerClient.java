package org.client;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;
import org.server.ShareMarketServer;

public class BuyerClient {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter your Buyer ID (e.g., NYKBXXXX, LONBXXXX, TOKBXXXX): ");
        String buyerID = scanner.next();

        // Define market URLs
        Map<String, String> marketUrls = new HashMap<>();
        marketUrls.put("NewYork", "http://localhost:8080/ShareMarketService?wsdl");
        marketUrls.put("London", "http://localhost:8081/ShareMarketService?wsdl");
        marketUrls.put("Tokyo", "http://localhost:8082/ShareMarketService?wsdl");

        // Determine correct server location based on buyer ID
        String serverName = ClientMap.getLocation(buyerID);
        if (serverName == null) {
            System.out.println("Invalid Buyer ID. Exiting.");
            return;
        }

        //Dynamically create the port
        String wsdlUrl = marketUrls.get(serverName);

        URL url = new URL(wsdlUrl);
        QName qname = new QName("http://server.org/", "ShareMarketServerImplService");
        Service service = Service.create(url, qname);
        ShareMarketServer port = service.getPort(ShareMarketServer.class);

        System.out.println("Connected to " + serverName + " Server.");

        while (true) {
            System.out.println("\nBuyer Menu (" + serverName + ")");
            System.out.println("1. Purchase Share (Local Market)");
            System.out.println("2. Purchase Share (Cross-Market)");
            System.out.println("3. View My Shares");
            System.out.println("4. Sell Share (Local Market)");
            System.out.println("5. Sell Share (Cross-Market)");
            System.out.println("6. Swap Shares");
            System.out.println("7. Exit");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();
            String response;
            String firstID;

            switch (choice) {
                case 1:
                    System.out.println("Example Share ID: LOCTDDMMYY (LOC: New York/T:Time/DDMMYY)");
                    System.out.print("Enter Share ID: ");
                    String shareID = scanner.next();
                    System.out.print("Enter Share Type (Equity/Bonus/Dividend): ");
                    String shareType = scanner.next();
                    System.out.print("Enter Quantity: ");
                    int quantity = scanner.nextInt();
                    response = port.purchaseShare(buyerID, shareID, shareType, quantity);
                    logAction(buyerID, "purchaseShare", response);
                    System.out.println(response);
                    break;

                case 2:
                    System.out.println("Example Share ID: LOCTDDMMYY (LOC: New York/T:Time/DDMMYY)");
                    System.out.print("Enter Share ID: ");
                    shareID = scanner.next();
                    System.out.print("Enter Share Type (Equity/Bonus/Dividend): ");
                    shareType = scanner.next();
                    System.out.print("Enter Quantity: ");
                    quantity = scanner.nextInt();
                    System.out.print("Enter Target Market (NewYork/London/Tokyo): ");
                    String targetMarket = scanner.next();
                    firstID = shareID.substring(0,1);
                    if(firstID != null && firstID.equalsIgnoreCase(targetMarket.substring(0,1))){
                        response = port.purchaseRemoteShare(buyerID, shareID, shareType, quantity, targetMarket);
                        logAction(buyerID, "purchaseRemoteShare", response);
                        System.out.println(response);
                    }
                    else{
                        System.out.println("Purchase failed because Market is not matching with ShareID.");
                    }
                    break;

                case 3:
                    System.out.println("--- Share Holdings Across Markets ---");
                    StringBuilder allShares = new StringBuilder();
                    for (Map.Entry<String, String> entry : marketUrls.entrySet()) {
                        String marketName = entry.getKey();
                        String wsdlURL = entry.getValue();
                        try {
                            URL url_market = new URL(wsdlURL);
                            QName qname_market = new QName("http://server.org/", "ShareMarketServerImplService");
                            Service service_market = Service.create(url_market, qname_market);
                            ShareMarketServer marketPort = service_market.getPort(ShareMarketServer.class);

                            String marketShares = marketPort.getShares(buyerID);
                            allShares.append("---Your ").append(marketName).append(" Market Shares---\n");
                            if (marketShares.trim().isEmpty() || marketShares.contains("No shares")) {
                                allShares.append("No shares found.\n");
                            } else {
                                allShares.append(marketShares).append("\n");
                            }
                        } catch (Exception e) {
                            allShares.append("Error retrieving shares from ").append(marketName).append(" market.\n");
                        }
                    }
                    System.out.println(allShares.toString());
                    break;

                case 4:
                    System.out.println("Example Share ID: LOCTDDMMYY (LOC: New York/T:Time/DDMMYY)");
                    System.out.print("Enter Share ID to sell: ");
                    shareID = scanner.next();
                    System.out.print("Enter Share Type (Equity/Bonus/Dividend): ");
                    shareType = scanner.next();
                    System.out.print("Enter Quantity to sell: ");
                    quantity = scanner.nextInt();

                    response = port.sellShare(buyerID, shareID, shareType, quantity);
                    logAction(buyerID, "sellShare", response);
                    System.out.println(response);
                    break;

                case 5:
                    System.out.println("Example Share ID: LOCTDDMMYY (LOC: New York/T:Time/DDMMYY)");
                    System.out.print("Enter Share ID to sell: ");
                    shareID = scanner.next();
                    System.out.print("Enter Share Type (Equity/Bonus/Dividend): ");
                    shareType = scanner.next();
                    System.out.print("Enter Quantity to sell: ");
                    quantity = scanner.nextInt();
                    System.out.print("Enter Target Market (NewYork/London/Tokyo): ");
                    targetMarket = scanner.next();

                    response = port.sellRemoteShare(buyerID, shareID, shareType, quantity, targetMarket);
                    logAction(buyerID, "sellRemoteShare", response);
                    System.out.println(response);
                    break;

                case 6:
                    System.out.println("--- Swap Shares ---");
                    System.out.print("Enter Old Share ID to swap out: ");
                    String oldShareID = scanner.next();
                    System.out.print("Enter Old Share Type (Equity/Bonus/Dividend): ");
                    String oldShareType = scanner.next();
                    System.out.print("Enter New Share ID to swap in: ");
                    String newShareID = scanner.next();
                    System.out.print("Enter New Share Type (Equity/Bonus/Dividend): ");
                    String newShareType = scanner.next();
                    response = port.swapShares(buyerID, oldShareID, oldShareType, newShareID, newShareType);
                    logAction(buyerID, "swapShares", response);
                    System.out.println(response);
                    break;

                case 7:
                    System.out.println("Exiting...");
                    scanner.close();
                    return;

                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
    //Logging Action
    private static void logAction(String buyerID, String methodName, String response) {
        try {
            FileWriter writer = new FileWriter("logs/Buyer_" + buyerID + "_Client.log", true);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.write("[" + timestamp + "] " + methodName + " | Response: " + response + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
