package org.client;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;
import org.server.ShareMarketServer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdminClient {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your Admin ID (e.g., NYKAXXXX, LONAXXXX, TOKAXXXX): ");
        String adminID = scanner.next();

        // Define market URLs
        Map<String, String> marketUrls = new HashMap<>();
        marketUrls.put("NewYork", "http://localhost:8080/ShareMarketService?wsdl");
        marketUrls.put("London", "http://localhost:8081/ShareMarketService?wsdl");
        marketUrls.put("Tokyo", "http://localhost:8082/ShareMarketService?wsdl");

        // Determine correct server location based on admin ID
        String cityCode = adminID.substring(0, 3);
        String serverName = getFullCityName(cityCode);

        if (serverName == null) {
            System.out.println("Invalid Admin ID. Exiting.");
            return;
        }

        // Dynamically create the web service client
        String wsdlUrl = marketUrls.get(serverName);

        try {
            URL url = new URL(wsdlUrl);
            QName qname = new QName("http://server.org/", "ShareMarketServerImplService");
            Service service = Service.create(url, qname);
            ShareMarketServer port = service.getPort(ShareMarketServer.class);

            System.out.println("Connected to " + serverName + " Server.");

            while (true) {
                System.out.println("\nAdmin Menu (" + serverName + ")");
                System.out.println("1. Add Share");
                System.out.println("2. Remove Share");
                System.out.println("3. List Share Availability");
                System.out.println("4. Purchase Share (Buyer Function)");
                System.out.println("5. View My Shares (Buyer Function)");
                System.out.println("6. Sell Share (Buyer Function)");
                System.out.println("7. Exit");
                System.out.print("Enter your choice: ");

                int choice = scanner.nextInt();
                String response;
                String shareID;
                String shareType;
                int quantity;

                switch (choice) {
                    case 1:
                        System.out.println("Example Share ID: LOCTDDMMYY (LOC: New York/T:Time/DDMMYY)");
                        System.out.print("Enter Share ID: ");
                        shareID = scanner.next();
                        if (shareID == null || shareID.length() != 10 || !shareID.contains(cityCode)) {
                            System.out.println("Invalid Share ID.");
                            logAction(adminID, "addShare", "failed");
                        } else {
                            System.out.print("Enter Share Type (Equity/Bonus/Dividend): ");
                            shareType = scanner.next();
                            System.out.print("Enter Capacity: ");
                            int capacity = scanner.nextInt();

                            response = port.addShare(shareID, shareType, capacity);
                            logAction(adminID, "addShare", response);
                            System.out.println(response);
                        }
                        break;

                    case 2:
                        System.out.print("Enter Share ID to remove: ");
                        shareID = scanner.next();
                        if (shareID == null || shareID.length() != 10 || !shareID.contains(cityCode)) {
                            System.out.println("Invalid Share ID.");
                            logAction(adminID, "removeShare", "failed");
                        } else {
                            System.out.print("Enter Share Type: ");
                            shareType = scanner.next();
                            response = port.removeShare(shareID, shareType);
                            logAction(adminID, "removeShare", response);
                            System.out.println(response);
                        }
                        break;

                    case 3:
                        System.out.print("Enter Share Type to list availability: ");
                        shareType = scanner.next();
                        //call listshareAvailability in local and other ports

                        StringBuilder availabilityInfo = new StringBuilder();
                        for (Map.Entry<String, String> entry : marketUrls.entrySet()) {
                            String marketName = entry.getKey();
                            String wsdlURL = entry.getValue();

                            try {
                                // Create a dynamic client for each market
                                URL url_market = new URL(wsdlURL);
                                QName qname_market = new QName("http://server.org/", "ShareMarketServerImplService");
                                Service service_market = Service.create(url_market, qname_market);
                                ShareMarketServer marketPort = service_market.getPort(ShareMarketServer.class);

                                String marketAvailability = marketPort.listShareAvailability(shareType);
                                availabilityInfo.append("\n---").append(marketName).append(" Market Availability---\n")
                                        .append(marketAvailability);
                            } catch (Exception e) {
                                availabilityInfo.append("\n---").append(marketName).append(" Market Availability---\n")
                                        .append("Error retrieving availability: ").append(e.getMessage());
                            }
                        }

                        response = availabilityInfo.toString();
                        logAction(adminID, "listShareAvailability", response);
                        System.out.println(response);
                        break;

                    case 4:
                        System.out.print("Enter Share ID: ");
                        shareID = scanner.next();
                        System.out.print("Enter Share Type (Equity/Bonus/Dividend): ");
                        shareType = scanner.next();
                        System.out.print("Enter Quantity: ");
                        quantity = scanner.nextInt();

                        response = port.purchaseShare(adminID, shareID, shareType, quantity);
                        logAction(adminID, "purchaseShare", response);
                        System.out.println(response);
                        break;

                    case 5:
                        response = port.getShares(adminID);
                        logAction(adminID, "getShares", response);
                        System.out.println("Your Shares: " + response);
                        break;

                    case 6:
                        System.out.print("Enter Share ID to sell: ");
                        shareID = scanner.next();
                        System.out.print("Enter Share Type (Equity/Bonus/Dividend): ");
                        shareType = scanner.next();
                        System.out.print("Enter Quantity: ");
                        quantity = scanner.nextInt();

                        response = port.sellShare(adminID, shareID,shareType, quantity);
                        logAction(adminID, "sellShare", response);
                        System.out.println(response);
                        break;

                    case 7:
                        System.out.println("Exiting Admin System.");
                        scanner.close();
                        return;

                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static String getFullCityName(String cityCode) {
        switch (cityCode) {
            case "NYK": return "NewYork";
            case "LON": return "London";
            case "TOK": return "Tokyo";
            default: return null;
        }
    }

    private static void logAction(String userID, String action, String response) {
        try {
            FileWriter writer = new FileWriter("C:\\Users\\barat\\Desktop\\COMP 6231\\Assignments\\3\\DSD A3\\ShareMarket\\src\\main\\java\\org\\logs\\client\\Admin_" + userID + ".log", true);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.write("[" + timestamp + "] " + action + " → " + response + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
