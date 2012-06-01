package edu.rice.rubis.beans.servlets;

import edu.rice.rubis.beans.*;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.rmi.PortableRemoteObject;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.rmi.RemoteException;
import java.util.GregorianCalendar;
import java.util.Enumeration;
import java.net.URLEncoder;

/** In fact, this class is not a servlet itself but it provides
 * output services to servlets to send back HTML files.
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */

public class ServletPrinter
{
  private PrintWriter       out;
  private String            servletName;
  private GregorianCalendar startDate;


  /**
   * Creates a new <code>ServletPrinter</code> instance.
   *
   * @param toWebServer a <code>HttpServletResponse</code> value
   * @param callingServletName a <code>String</code> value
   */
  public ServletPrinter(HttpServletResponse toWebServer, String callingServletName)
  {
    startDate = new GregorianCalendar();
    toWebServer.setContentType("text/html");
    try
    {
      out = toWebServer.getWriter();
    }
    catch (IOException ioe)
    {
      ioe.printStackTrace();
    }
    servletName = callingServletName;
  }

  void printFile (String filename)
  {
    FileReader fis = null;
    try
    {
      fis = new FileReader(filename);
      char[] data = new char[4*1024]; // 4K buffer
      int    bytesRead;
      bytesRead = fis.read(data);
      while (/*(bytesRead = fis.read(data))*/ bytesRead != -1)
      {
        out.write(data, 0, bytesRead);
	bytesRead = fis.read(data);
      }
    }
    catch (Exception e)
    {
      out.println("Unable to read file (exception: "+e+")<br>\n");
    }
    finally
    {
      if (fis != null)
        try
        {
          fis.close();
        }
      catch (Exception ex)
      {
        out.println("Unable to close the file reader (exception: "+ex+")<br>\n"); 
      }
    }
  }

  void printHTMLheader(String title)
  {
    printFile(Config.HTMLFilesPath+"/header.html");
    out.println("<title>"+title+"</title>\n");
  }

  void printHTMLfooter()
  {
    GregorianCalendar endDate = new GregorianCalendar();

    out.println("<br><hr>RUBiS (C) Rice University/INRIA<br><i>Page generated by "+servletName+" in "+TimeManagement.diffTime(startDate, endDate)+"</i><br>\n");
    out.println("</body>\n");
    out.println("</html>\n");	
  }

  void printHTML(String msg)
  {
    out.println(msg);
  }

  void printHTMLHighlighted(String msg)
  {
    out.println("<TABLE width=\"100%\" bgcolor=\"#CCCCFF\">\n");
    out.println("<TR><TD align=\"center\" width=\"100%\"><FONT size=\"4\" color=\"#000000\"><B>"+msg+"</B></FONT></TD></TR>\n");
    out.println("</TABLE><p>\n");
  }

  

  /** Category related printed functions */

  void printCategory(Category category)
  {
    try
    {
      out.println(category.printCategory());
    }
    catch (RemoteException re)
    {
      out.println("Unable to print Category (exception: "+re+")<br>\n");
    }
  }

  /** List all the categories with links to browse items by region */
  void printCategoryByRegion(Category category, int regionId)
  {
    try
    {
      out.println(category.printCategoryByRegion(regionId));
    }
    catch (RemoteException re)
    {
      out.println("Unable to print Category (exception: "+re+")<br>\n");
    }
  }

  /** Lists all the categories and links to the sell item page*/
  void printCategoryToSellItem(Category category, int userId)
  {
    try
    {
      out.println(category.printCategoryToSellItem(userId));
    }
    catch (RemoteException re)
    {
      out.println("Unable to print Category (exception: "+re+")<br>\n");
    }
  }
  

  // Bid related printed functions //
  
  void printBidHistoryHeader()
  {
    out.println("<TABLE border=\"1\" summary=\"List of bids\">\n"+
                "<THEAD>\n"+
                "<TR><TH>User ID<TH>Bid amount<TH>Date of bid\n"+
                "<TBODY>\n");
  }  

  void printBidHistoryFooter()
  {
    out.println("</TABLE>\n");
  } 

  void printBid(Bid bid)
  {
    try
    {
      out.println("<a href=\""+Config.context+"/servlet/BrowseBid\">"+
                  bid.getUserId()+" "+bid.getItemId()+" "+bid.getBid()+" "+
                  bid.getMaxBid()+" "+bid.getDate()+"</a><br>\n");
    }
    catch (RemoteException re)
    {
      out.println("Unable to print Bid (exception: "+re+")<br>");
    }
  }

  void printBidHistory(Bid bid)
  {
    try
    {
      out.println(bid.printBidHistory());
    }
    catch (RemoteException re)
    {
      out.println("Unable to print Bid (exception: "+re+")<br>\n");
    }
  }


  /** Region related printed functions */

  void printRegion(Region region)
  {
    try
    {
      String name = region.getName();
      out.println("<a href=\""+Config.context+"/servlet/edu.rice.rubis.beans.servlets.BrowseCategories?region="+URLEncoder.encode(name)+"\">"+name+"</a><br>\n");
    }
    catch (RemoteException re)
    {
      out.println("Unable to print Region (exception: "+re+")<br>\n");
    }
  }


  /////////////////////////////////////
  // Item related printing functions //
  /////////////////////////////////////

  void printUserBoughtItemHeader()
  {
    printHTML("<br>");
    printHTMLHighlighted("<p><h3>Items you bouhgt in the past 30 days.</h3>\n");
    out.println("<TABLE border=\"1\" summary=\"List of items\">\n"+
                "<THEAD>\n"+
                "<TR><TH>Designation<TH>Quantity<TH>Price you bought it<TH>Seller"+
                "<TBODY>\n");
  }

  void printUserBoughtItem(Item item, int quantity)
  {
    try
    {
      out.println(item.printUserBoughtItem(quantity));
    }
    catch (RemoteException re)
    {
      out.println("Unable to print Item (exception: "+re+")<br>\n");
    }
  }

  void printUserWonItemHeader()
  {
    printHTML("<br>");
    printHTMLHighlighted("<p><h3>Items you won in the past 30 days.</h3>\n");
    out.println("<TABLE border=\"1\" summary=\"List of items\">\n"+
                "<THEAD>\n"+
                "<TR><TH>Designation<TH>Price you bought it<TH>Seller"+
                "<TBODY>\n");
  }

  void printUserWonItem(Item item)
  {
    try
    {
      out.println(item.printUserWonItem());
    }
    catch (RemoteException re)
    {
      out.println("Unable to print Item (exception: "+re+")<br>\n");
    }
  }

  void printUserBidsHeader()
  {
    printHTMLHighlighted("<p><h3>Items you have bid on.</h3>\n");
    out.println("<TABLE border=\"1\" summary=\"Items You've bid on\">\n"+
                "<THEAD>\n"+
                "<TR><TH>Designation<TH>Initial Price<TH>Current price<TH>Your max bid<TH>Quantity"+
                "<TH>Start Date<TH>End Date<TH>Seller<TH>Put a new bid\n"+
                "<TBODY>\n");
  }


  void printItemUserHasBidOn(Bid bid, Item item, String username, String password)
  {
    try
    {
      out.println(item.printItemUserHasBidOn(bid.getMaxBid())+"&nickname="+URLEncoder.encode(username)+"&password="+URLEncoder.encode(password)+"\"><IMG SRC=\"/EJB_HTML/bid_now.jpg\" height=22 width=90></a>\n");
    }
    catch (RemoteException re)
    {
      out.println("Unable to print Item (exception: "+re+")<br>\n");
    }
  }


  void printSellHeader(String title)
  {
    printHTMLHighlighted("<p><h3>"+title+"</h3>\n");
    out.println("<TABLE border=\"1\" summary=\"List of items\">\n"+
                "<THEAD>\n"+
                "<TR><TH>Designation<TH>Initial Price<TH>Current price<TH>Quantity<TH>ReservePrice<TH>Buy Now"+
                "<TH>Start Date<TH>End Date\n"+
                "<TBODY>\n");
  }

  
  void printSell(Item item)
  {
    try
    {
      out.println(item.printSell());
    }
    catch (RemoteException re)
    {
      out.println("Unable to print Item (exception: "+re+")<br>\n");
    }
  }

  void printItemHeader()
  {
    out.println("<TABLE border=\"1\" summary=\"List of items\">\n"+
                "<THEAD>\n"+
                "<TR><TH>Designation<TH>Price<TH>Bids<TH>End Date<TH>Bid Now\n"+
                "<TBODY>\n");
  }

  void printItem(Item item)
  {
    try
    {
      out.println(item.printItem());
    }
    catch (RemoteException re)
    {
      out.println("Unable to print Item (exception: "+re+")<br>\n");
    }
  }

  void printItemFooter(String URLprevious, String URLafter)
  {
    out.println("</TABLE>\n");
    out.println("<p><CENTER>\n"+URLprevious+"\n&nbsp&nbsp&nbsp"+URLafter+"\n</CENTER>\n");
  }

  void printItemFooter()
  {
    out.println("</TABLE>\n");
  }

  /**
   * Print the full description of an item and the bidding option if userId>0.
   *
   * @param item an <code>Item</code> value
   * @param userId an authenticated user id
   * @param initialContext a <code>Context</code> value
   */
  void printItemDescription(Item item, int userId, Context initialContext)
  {
    try
    {
      Integer itemId   = item.getId();
      float   buyNow   = 0;
      int     nbOfBids = item.getNbOfBids();
      int     qty      = item.getQuantity();
      String  firstBid;
      Query   query;
      float   maxBid   = item.getMaxBid();

      if (maxBid == 0)
      {
        firstBid = "none";
        maxBid = item.getInitialPrice();
        buyNow = item.getBuyNow();
      }
      else
      {
        if (qty > 1)
        {
          BidHome bHome = null;
          try 
          { // Connecting to Query Home thru JNDI
            QueryHome qHome;
            qHome = (QueryHome)PortableRemoteObject.narrow(initialContext.lookup("QueryHome"), QueryHome.class);
            query = qHome.create();
            // Connecting to Bid Home thru JNDI
            bHome = (BidHome)PortableRemoteObject.narrow(initialContext.lookup("BidHome"), BidHome.class);
            /* Get the qty max first bids and parse bids in this order
               until qty is reached. The bid that reaches qty is the
               current minimum bid. */
            Enumeration list = query.getItemQtyMaxBid(qty, itemId).elements();
            Bid         bid;
            int         numberOfItems = 0;
            while (list.hasMoreElements()) 
            {
              bid = bHome.findByPrimaryKey((BidPK)list.nextElement());
              numberOfItems += bid.getQuantity();
              if (numberOfItems >= qty)
              {
                maxBid = bid.getBid();
                break;
              }
            }
          } 
          catch (Exception e)
          {
            printHTML("Problem while computing current bid: "+e+"<br>");
            return ;
          }
        }

        Float foo = new Float(maxBid);
        firstBid = foo.toString();
      }

      String itemName = item.getName();
      if (userId>0)
      {
        printHTMLheader("RUBiS: Bidding\n");
        printHTMLHighlighted("You are ready to bid on: "+itemName);
      }
      else
      {
        printHTMLheader("RUBiS: Viewing "+itemName+"\n");
        printHTMLHighlighted(itemName);
      }
      out.println("<TABLE>\n"+
                  "<TR><TD>Currently<TD><b><BIG>"+maxBid+"</BIG></b>\n");
      // Check if the reservePrice has been met (if any)
      float   reservePrice = item.getReservePrice();
      Integer sellerId = item.getSellerId();
      if (reservePrice > 0)
      { // Has the reserve price been met ?
        if (maxBid >= reservePrice)
          out.println("(The reserve price has been met)\n");
        else
          out.println("(The reserve price has NOT been met)\n");
      }
      out.println("<TR><TD>Quantity<TD><b><BIG>"+qty+"</BIG></b>\n"+
                  "<TR><TD>First bid<TD><b><BIG>"+firstBid+"</BIG></b>\n"+
                  "<TR><TD># of bids<TD><b><BIG>"+nbOfBids+"</BIG></b> (<a href=\""+Config.context+"/servlet/edu.rice.rubis.beans.servlets.ViewBidHistory?itemId="+itemId+"\">bid history</a>)\n"+
                  "<TR><TD>Seller<TD><a href=\""+Config.context+"/servlet/edu.rice.rubis.beans.servlets.ViewUserInfo?userId="+sellerId+"\">"+item.getSellerNickname()+"</a> (<a href=\"/servlet/edu.rice.rubis.beans.servlets.PutCommentAuth?to="+sellerId+"&itemId="+itemId+"\">Leave a comment on this user</a>)\n"+
                  "<TR><TD>Started<TD>"+item.getStartDate()+"\n"+
                  "<TR><TD>Ends<TD>"+item.getEndDate()+"\n"+
                  "</TABLE>");
      // Can the user buy this item now ?
      if (buyNow > 0)
        out.println("<p><a href=\""+Config.context+"/servlet/edu.rice.rubis.beans.servlets.BuyNowAuth?itemId="+itemId+"\">"+
                    "<IMG SRC=\""+Config.context+"/buy_it_now.jpg\" height=22 width=150></a>"+
                    "  <BIG><b>You can buy this item right now for only $"+buyNow+"</b></BIG><br><p>\n");

      if (userId<=0)
      {
        out.println("<a href=\""+Config.context+"/servlet/edu.rice.rubis.beans.servlets.PutBidAuth?itemId="+itemId+"\"><IMG SRC=\""+Config.context+"/bid_now.jpg\" height=22 width=90> on this item</a>\n");
      }

      printHTMLHighlighted("Item description");
      out.println(item.getDescription());
      out.println("<br><p>\n");

      if (userId>0)
      {
        printHTMLHighlighted("Bidding");
        float minBid = maxBid+1;
        printHTML("<form action=\""+Config.context+"/servlet/edu.rice.rubis.beans.servlets.StoreBid\" method=POST>\n"+
                  "<input type=hidden name=minBid value="+minBid+">\n"+
                  "<input type=hidden name=userId value="+userId+">\n"+
                  "<input type=hidden name=itemId value="+itemId+">\n"+
                  "<input type=hidden name=maxQty value="+qty+">\n"+
                  "<center><table>\n"+
                  "<tr><td>Your bid (minimum bid is "+minBid+"):</td>\n"+
                  "<td><input type=text size=10 name=bid></td></tr>\n"+
                  "<tr><td>Your maximum bid:</td>\n"+
                  "<td><input type=text size=10 name=maxBid></td></tr>\n");
        if (qty > 1)
          printHTML("<tr><td>Quantity:</td>\n"+
                    "<td><input type=text size=5 name=qty></td></tr>\n");
        else
          printHTML("<input type=hidden name=qty value=1>\n");
        printHTML("</table><p><input type=submit value=\"Bid now!\"></center><p>\n");
      }
    }
    catch (RemoteException re)
    {
      out.println("Unable to print Item description (exception: "+re+")<br>\n");
    }
  }

  /**
   * Print the full description of an item and the buy now option
   *
   * @param item an <code>Item</code> value
   * @param userId an authenticated user id
   */
  void printItemDescriptionToBuyNow(Item item, int userId)
  {
    try
    {
      Integer itemId   = item.getId();
      float   buyNow   = item.getBuyNow();
      int     qty      = item.getQuantity();
      String itemName  = item.getName();
      Integer sellerId = item.getSellerId();

      printHTMLheader("RUBiS: Buy Now");
      printHTMLHighlighted("You are ready to buy this item: "+itemName);
      out.println(item.printItemDescriptionToBuyNow(userId));
    }
    catch (RemoteException re)
    {
      out.println("Unable to print Item description (exception: "+re+")<br>\n");
    }
  }

  /** Comment related printed functions */

  void printCommentHeader()
  {
    out.println("<DL>\n");
  }

  void printComment(String userName, Comment comment)
  {
    try
    {
      out.println(comment.printComment(userName));
    }
    catch (RemoteException re)
    {
      out.println("Unable to print Comment (exception: "+re+")<br>\n");
    }
  }

  void printCommentFooter()
  {
    out.println("</DL>\n");
  }

}
