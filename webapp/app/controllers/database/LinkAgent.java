package controllers.database;

import java.util.HashMap;
import java.util.List;

import java.util.Iterator;

import models.Feed;
import models.Link;
import models.Metric;
import play.Logger;
import play.mvc.Controller;

/**
 * @author Oliver Baumann
 * @version 0.0.1
 */
public class LinkAgent extends Controller {
    /**
     * Create a link in the database.
     * 
     * The link is defined by its source and target, along with the subnet. All
     * parameters are unique columns in the database, the PK is an ID-value.
     * 
     * @param sourceInterface
     *            the source interface, restricted to 15 characters
     * @param sourceIP
     *            the source IP
     * @param targetInterface
     *            the target interface, restricted to 15 characters
     * @param targetIP
     *            the target IP
     * @param subnet
     *            the subnet
     * @param metricName the metric-name
     * @param feedName the feed-name
     * @return the link that has been created, else null
     */
    public static Link createLink(final String sourceInterface,
            final String sourceIP, final String targetInterface,
            final String targetIP, final String subnet, final String metricName, final String feedName) {

        Link l = new Link(sourceInterface, sourceIP, targetInterface, targetIP,
                subnet, metricName, feedName);
        l.save();
        return l;
    }

    /**
     * Set up Link-Metric-Feed-relationship.
     * 
     * A link can have metrics assigned to it and feeds providing data for the
     * metrics. Any link can only have one metric of any name.
     * 
     * @param sourceInterface
     *            the source interface, restricted to 15 characters
     * @param sourceIP
     *            the source IP
     * @param targetInterface
     *            the target interface, restricted to 15 characters
     * @param targetIP
     *            the target IP
     * @param subnet
     *            the subnet
     * @param metricName
     *            the metric's name
     * @param feedName
     *            the feed's name
     * @return true on success
     */
    public static boolean insertLinkWithRelationships(
            final String sourceInterface, final String sourceIP,
            final String targetInterface, final String targetIP,
            final String subnet, final String metricName, final String feedName) {

        try {
            Link l = LinkAgent.createLink(sourceInterface, sourceIP,
                    targetInterface, targetIP, subnet, metricName, feedName);

            Logger.debug("insertLinkWithRelationships(): source/target = "
                    + l.getSourceIP() + " / " + l.getTargetIP());
            
            return true;
        } catch (Exception e) {
            Logger.debug(e.getMessage());
            return false;
        }
    }

    public static HashMap<String, String> getMetricFeed(de.lmu.ifi.nm.www.Link l) {
        HashMap<String, String> metricFeedRel = new HashMap<String, String>();

        List<Link> matchingLinks = Link.getFind().where()
                .eq("source_interface", l.getSourceIntPort())
                .eq("source_ip", l.getSourceIPaddress())
                .eq("target_interface", l.getTargetIntPort())
                .eq("target_ip", l.getTargetIPaddress())
                .eq("subnet", l.getSubnetAddress()).findList();
        
        Logger.debug("matchingLinks.size() = " + matchingLinks.size());

        Logger.debug("---- We got this info about the link: ----\n"
                + "source_interface: " + l.getSourceIntPort() + "\n"
                + "source_ip: " + l.getSourceIPaddress() + "\n"
                + "target_interface: " + l.getTargetIntPort() + "\n"
                + "target_ip: " + l.getTargetIPaddress() + "\n" + "subnet: "
                + l.getSubnetAddress() + "\n");

        Iterator<Link> i = matchingLinks.iterator();
        while (i.hasNext()) {
            Link currentLink = i.next();
            Logger.debug("The metric is " + currentLink.getMetric().getName()
                    + " and the feed is " + currentLink.getFeed().getName());
            metricFeedRel.put(currentLink.getMetric().getName(), currentLink
                    .getFeed().getName());
        }

        return metricFeedRel;
    }
}
