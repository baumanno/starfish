package controllers.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.Feed;
import models.Metric;
import models.Trc;
import de.lmu.ifi.nm.www.Link;
import de.lmu.ifi.nm.www.Vertex;

import org.jgrapht.graph.DirectedWeightedMultigraph;

import controllers.database.FeedAgent;
import controllers.database.MetricAgent;

import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.Result;
import play.Logger;
import views.html.*;
import play.api.libs.json.*;

import views.html.dowork.*;
import views.html.recalculate.*;
/**
 * Handles Communication between Webfrontend and Java-Application
 * 
 * @author Werner Hoffmann
 * @version 0.1 Gives information about network graph to Frontend
 * @version 0.2 Gets data from Webfrontend
 * @version 0.3 Code cleaning. All logic parts one layer back in Logic.class New
 *          features
 */
@SuppressWarnings("unused")
public class Communicator extends Controller {
    public static Logic MyLogic;

    public Communicator() {
        play.Logger.debug("Communicator started.");
        MyLogic = new Logic();
    }

    /**
     * 
     * A method for giving the whole Topology back. Used for Fetch Command
     * 
     * @return 200 OK-Response
     */

    public static Result Topology() {
        if (MyLogic == null) {
            new Communicator();
        }
        Logger.debug("GUI wants to know Topology");
        DirectedWeightedMultigraph<Vertex, Link> network = MyLogic
                .getTopology();
        return ok(summaryrecalc.render());
    }

    public static Result getLink() {
        String s = "{";
        int count = 0;

        Set<Vertex> VertexSet = MyLogic.network.vertexSet();

        Set<Link> LinkSet = new HashSet<Link>();
        // create a List with all Links
        for (Vertex Vertex : VertexSet) {
            Set<Link> LinklistTemp = MyLogic.network.incomingEdgesOf(Vertex);
            for (Link Link : LinklistTemp) {
                LinkSet.add(Link);
            }
        }

        for (Link l : LinkSet) {
            Vertex a = MyLogic.network.getEdgeSource(l);
            Vertex b = MyLogic.network.getEdgeTarget(l);
            count++;
            s = s + "\"" + count + "\" : {";
            s = s + "\"uniqueID\": \"" + a.getUniqueIdentifier() + "\"" + ",";
            s = s + "\"latitudeA\": " + a.getLatitude() + ",";
            s = s + "\"longitudeA\": " + a.getLongitude() + ",";
            s = s + "\"latitudeB\": " + b.getLatitude() + ",";
            s = s + "\"longitudeB\": " + b.getLongitude();
            s = s + " }";

            if (count < LinkSet.size()) {
                s = s + ",";
            }

        }

        s = s + "}";
        return ok(s);
    }

    public static Result getTracerouteLink() {
        List<Link> path = MyLogic.calculateTraceroute();

        String s = "{";
        int count = 0;

        if(path != null) {
            for (Link l : path) {

                Vertex a = MyLogic.network.getEdgeSource(l);
                Vertex b = MyLogic.network.getEdgeTarget(l);

                count++;
                s = s + "\"" + count + "\" : {";
                s = s + "\"latitudeA\": " + a.getLatitude() + ",";
                s = s + "\"longitudeA\": " + a.getLongitude() + ",";
                s = s + "\"latitudeB\": " + b.getLatitude() + ",";
                s = s + "\"longitudeB\": " + b.getLongitude();
                s = s + " }";

                if (count < path.size()) {
                    s = s + ",";
                }
            }
        }
            s = s + "}";
            return ok(s);
    }

    public static Result getAllVertexes() {
        Set<Vertex> vertexes = MyLogic.network.vertexSet();
        String s = "{";
        int count = 0;

        for (Vertex a : vertexes) {
            count++;
            s = s + "\"" + count + "\" : {";
            s = s + "\"latitude\": " + a.getLatitude() + ",";
            s = s + "\"longitude\": " + a.getLongitude() + ",";
            s = s + "\"nodeName\": \"" + a.getUniqueIdentifier() + "\"";
            s = s + " }";

            if (count < vertexes.size()) {
                s = s + ",";
            }
        }

        s = s + "}";
        return ok(s);
    }

    public static Result getAllInfos() {

        Set<Vertex> vertexes = MyLogic.network.vertexSet();
        String s = "{";
        int count = 0;

        for (Vertex a : vertexes) {
            count++;
            s = s + "\"" + count + "\" : {";
            s = s + "\"latitude\": " + a.getLatitude() + ",";
            s = s + "\"longitude\": " + a.getLongitude() + ",";
            s = s + "\"hostname\": \"" + a.getHostname() + "\"" + ",";
            s = s
                    + "\"routingTable\": \""
                    + a.getRoutingTable().replace(
                            System.getProperty("line.separator"), "\\n") + "\""
                            + ",";
            s = s + "\"upTime\": " + a.getUpTime() + ",";
            s = s + "\"managementIP\": \"" + a.getManagementIP() + "\"";
            s = s + " }";

            if (count < vertexes.size()) {
                s = s + ",";
            }
        }

        s = s + "}";
        return ok(s);
    }

    public static List<String> MNameDrop() {
        List<String> all = new ArrayList<String>();
        all.add("Data1");
        all.add("Data2");
        all.add("Data3");
        return all;
    }

    public static Result getVertexInfoShort() {
        Set<Vertex> vertexes = MyLogic.network.vertexSet();
        String s = "";
        for (Vertex a : vertexes) {
            s = s + a.getLatitude() + "; " + a.getLongitude();
            return ok(s);
        }

        return ok(s);
    }

    public static Result getVertexInfoAll(String name) {
        Set<Vertex> vertexes = MyLogic.network.vertexSet();
        String s = "";
        for (Vertex a : vertexes) {
            if (a.getUniqueIdentifier().equals(name)) {
                s = s + a.getHostname() + a.getManagementIP()
                        + a.getRoutingTable() + a.getUpTime();
                return ok(s);
            }

        }
        return ok(s);
    }

    public static List<String> getAllFeeds() {
        if (MyLogic == null) {
            new Communicator();
        }
        List<Feed> feeds = Communicator.MyLogic.getAllFeeds();
        int count = 0;
        String s = "{";
        for (Feed f : feeds) {
            s = s + f.getName();
            if (count < feeds.size()) {
                s = s + "; ";
            }
        }
        s = s + "}";

        // for who prefers lists:
        List<String> feedNames = new ArrayList<String>();
        for (Feed f : feeds) {
            feedNames.add(f.getName());
        }
        // change return Type from "Result" to "List<String>"
        // change return statement from ok(s) to feedNames

        return feedNames;
    }

    public static List<String> getAllMetrics() {
        if (MyLogic == null) {
            new Communicator();
        }

        List<Metric> metrics = Communicator.MyLogic.getAllMetrics();
        int count = 0;
        String s = "{";

        for (Metric m : metrics) {
            s = s + m.getName();
            if (count < metrics.size()) {
                s = s + "; ";
            }
            s = s + "}";
        }

        // for who prefers lists:
        List<String> metricNames = new ArrayList<String>();

        for (Metric m : metrics) {
            metricNames.add(m.getName());
        }

        // change return Type from "Result" to "List<String>"
        // change return statement from ok(s) to metricNames
        return metricNames;
    }

    public static Result doWork() {
        if (MyLogic == null) {
            new Communicator();
        }
        Logger.debug("Gui calls doWork()");
        Communicator.MyLogic.dowork();
        return ok(summary.render());
    }

}
