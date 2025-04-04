package org.server;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;


@WebService
@SOAPBinding(style = Style.RPC)
public interface ShareMarketServer{
    // Admin Operations
    @WebMethod
    String addShare(@WebParam(name = "shareID")String shareID, @WebParam(name = "shareType")String shareType,
                    @WebParam(name = "capacity") int capacity);

    @WebMethod
    String removeShare(@WebParam(name = "shareID")String shareID, @WebParam(name = "shareType")String shareType);

    @WebMethod
    String listShareAvailability(@WebParam(name = "shareType")String shareType);

    @WebMethod
    String purchaseRemoteShare(@WebParam(name = "buyerID")String buyerID, @WebParam(name = "shareID")String shareID,
                               @WebParam(name = "shareType")String shareType, @WebParam(name = "shareCount")int shareCount,
                               @WebParam(name = "targetMarket")String targetMarket);

    @WebMethod
    String sellRemoteShare(@WebParam(name = "buyerID")String buyerID, @WebParam(name = "shareID")String shareID,
                           @WebParam(name = "shareType")String shareType, @WebParam(name = "shareCount")int shareCount,
                           @WebParam(name = "targetMarket")String targetMarket);


    // Buyer Operations
    @WebMethod
    String purchaseShare(@WebParam(name = "buyerID")String buyerID, @WebParam(name = "shareID")String shareID,
                         @WebParam(name = "shareType")String shareType, @WebParam(name = "shareCount")int shareCount);

    @WebMethod
    String getShares(@WebParam(name = "buyerID")String buyerID);

    @WebMethod
    String sellShare(@WebParam(name = "buyerID")String buyerID, @WebParam(name = "shareID")String shareID,
                     @WebParam(name = "shareType")String shareType,
                     @WebParam(name = "shareCount")int shareCount);

    @WebMethod
    String swapShares(@WebParam(name = "buyerID")String buyerID, @WebParam(name = "oldShareID")String oldShareID,
                      @WebParam(name = "oldShareType")String oldShareType, @WebParam(name = "newShareID")String newShareID,
                      @WebParam(name = "newShareType")String newShareType);
}
