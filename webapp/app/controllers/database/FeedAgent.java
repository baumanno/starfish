package controllers.database;

import helpers.HttpsAuthenticator;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.URL;
import java.sql.Timestamp;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import models.Feed;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import play.Logger;
import play.mvc.Controller;

/**
 * @author Oliver Baumann
 * 
 * @version 0.3 Correct return types and fully-functional CRUD-methods
 * @version 0.4 Iterable NodeList; external Helpers
 * @version 0.5 Support for HTTPS-authentication
 * @version 0.6 getAll()-method implemented
 */
public class FeedAgent extends Controller {

    /**
     * Parses XPath against XML-data
     * 
     * Takes an input from a url-resource, an XPath and a resultset for the
     * final result.
     * 
     * @param input
     *            the XML-input-stream
     * @param xPathString
     *            the XPath-argument
     * @return the result of the XPath-validation
     */
    private static int eval(final InputStream input, final String xPathString) {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder;

        try {
            builder = factory.newDocumentBuilder();
            Document document;
            try {
                document = builder.parse(input);

                XPathFactory xPathFactory = XPathFactory.newInstance();

                XPath xPath = xPathFactory.newXPath();

                XPathExpression xPathExpression = xPath.compile(xPathString);

                Logger.debug("What is the result? The result is...");
                Double result = (Double) xPathExpression.evaluate(document,
                        XPathConstants.NUMBER);
                Logger.debug("result = " + result);
                Logger.debug("int-result = " + result.intValue());

                return result.intValue();

            } catch (SAXParseException saxpe) {
                Logger.error("There was a parse-error with SAX: "
                        + saxpe.getMessage() + "(Line-number: "
                        + saxpe.getLineNumber() + ")");
            } catch (SAXException saxe) {
                Logger.error("There was an error with SAX."
                        + "Sorry, no more info here...");
                saxe.printStackTrace();
            } catch (IOException ioe) {
                Logger.error("There was an error retrieving the URL."
                        + "Sorry, no more info here...");
                ioe.printStackTrace();
            }

        } catch (ParserConfigurationException pce) {
            Logger.error("There was an error with XPath-parser-config.");
            pce.printStackTrace();
        } catch (XPathExpressionException xee) {
            Logger.error("There was an error with the XPath."
                    + "Please check the argument passed to FeedAgent.eval()");
            xee.printStackTrace();
        }
        
        Logger.error("We are returning 0. This should never happen!");
        return 0;
    }

    /**
     * Retrieves the feed from the url and displays it.
     * 
     * @param url
     *            the feed-url
     * @param user
     *            the user for HTTPS-auth
     * @param pass
     *            the password corresponding to the user
     * @param xPath
     *            the xpath
     * @return Collection<String> of data-items
     * @throws IOException
     *             erroneous url
     */
    public static int getFeedData(final String url, final String user,
            final String pass, final String xPath) throws IOException {

        if (url.startsWith("https")) {
            Logger.debug("Feed is using HTTPS! Setting up Authenticator...");

            Authenticator.setDefault(new HttpsAuthenticator(user, pass));
            Logger.debug("Authenticator was set up");
        }

        URL u = new URL(url);
        Logger.debug("URL created...");
        InputStream in = u.openStream();
        Logger.debug("Inputstream opened");

        Logger.info("Attempting to return eval...");
        
        int theValue = eval(in, xPath); 
        
        if (theValue < 0) {
            return 0;
        } else {
            return theValue;
        }        
    }

    /**
     * Saves feed to database
     * 
     * Name, URL and values retrieved via XPath are saved to the database.
     * 
     * @param name
     *            the name the feed should have; this is the primary-key in the
     *            DB
     * @param url
     *            the URL the feed is at
     * @param user
     *            the user for HTTPS-auth
     * @param pass
     *            the password corresponfing to the user
     * @param xPath
     *            the XPath to find the data at
     * @return true on successful creation
     * 
     */

    public static boolean createFeed(final String name, final String url,
            final String user, final String pass, final String xPath) {
        try {

            /* First, retrieve the data we want to store: */
            int val = getFeedData(url, user, pass, xPath);

            /*
             Finally, create the feed with all the parameters
             createFeed() also saves to DB.
             */
            Feed feed = new Feed(name, url, user, pass, xPath, val);
            
            Logger.debug("Saving model...");
            feed.save();
            Logger.debug("...saved");

            return true;
        } catch (IOException ioe) {
            Logger.error("Error fetching the URL. Please check it is correct.",
                    ioe);

            return false;
        }
    }

    /**
     * Retrieve the feed-data
     * 
     * Fetches the values stored in the database.
     * 
     * @param name
     *            the name of the feed
     * @return a string of values retrieved by the XPath-argument
     */

    public static int readValues(final String name) {

        Feed f = Feed.getFind().where().eq("name", name).findUnique();

        return f.getCachedValue();
    }

    /**
     * Update the feed.
     * 
     * Set new data-items specified by the XPath
     * 
     * @param name
     *            the feed's ID in the database
     * @param xPath
     *            the XPath-argument defining the data
     * @return true on successful update
     */

    public static boolean updateFeed(final String name, final String xPath) {

        /* Query the database to fetch a feed called "name" */
        Feed f = Feed.getFind().where().eq("name", name).findUnique();
        Logger.info("Found feed " + name);

        int newData;
        try {

            /*
             Use the URL that is stored in the database
             The name has to exist, the xPath can be modified
             Important: this fetches the credentials from the DB!
             */

            newData = getFeedData(f.getUrl(), f.getUser(), f.getPass(), xPath);

            Logger.info("updateFeed(): New data is " + newData);
            f.setCachedValue(newData);
            f.setLastRefreshAt(new Timestamp(System.currentTimeMillis()));
            f.update();

            return true;
        } catch (IOException e) {
            Logger.error("IOError: please check the URL!", e);
            return false;
        }
    }

    /**
     * Delete the feed.
     * 
     * Identification is achieved via the 'name'-attribute
     * 
     * @param name
     *            the name as it is stored in the database
     * @return true on successfull delete
     */

    public static boolean deleteFeed(final String name) {
        Feed f = Feed.getFind().where().eq("name", name).findUnique();

        try {
            f.delete();
            return true;
        } catch (Exception e) {
            Logger.error("Error deleting feed. ", e);
            return false;
        }
    }

    /**
     * Return a list of all feeds stored in the DB.
     * 
     * @return a List<Feed>
     */

    public static List<Feed> getAllFeeds() {
        return Feed.getFind().all();
    }
}
