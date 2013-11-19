package controllers.service;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.*;
import models.ApplicationProperties;
import de.lmu.ifi.nm.www.Link;
import de.lmu.ifi.nm.www.Vertex;

import org.dyndns.kwitte.jfunction.FunctionFormatException;
import org.dyndns.kwitte.jfunction.Terms;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import play.Logger;
import controllers.database.LinkAgent;
import controllers.database.MetricAgent;
import controllers.networking.TopologyDiscoverer;

/**
 * Contains the most part of the logic of the program.
 * 
 * @author Werner Hoffmann
 * @version 0.3
 */
public class Logic {
	static TopologyDiscoverer TopologyDiscoverer;
	public static DirectedWeightedMultigraph<Vertex, Link> network;
	public static List<Metric> metrics;
	public static List<Feed> feeds;
	public static String formula;
	public static ApplicationProperties properties;
	public static Trc traceroute = new Trc();

	/**
	 * Does the whole logic part: Managing communication with GUI Managing
	 * network decides what is when to do Calculates Link costs
	 * 
	 */
	public Logic() {
		Logger.debug("Logic Constructor started");
		TopologyDiscoverer = new TopologyDiscoverer();
		network = new DirectedWeightedMultigraph<Vertex, Link>(Link.class);

		metrics = controllers.database.MetricAgent.getAllMetrics();
		feeds = controllers.database.FeedAgent.getAllFeeds();
		properties = new ApplicationProperties();
		properties.loadProgrammProperies();

		IterativeThread t0 = new IterativeThread(
				properties.getMinutesToRefresh());
		 t0.start();
	}

	/**
	 * Contains to start the work Loads programm.properties new Refresh network
	 * calcualtes Linkcosts Updates the Feeds Calculates and sets the routes
	 * 
	 */
	public void dowork() {
		Logger.debug("do work");
		properties.loadProgrammProperies();
		formula = properties.getFormula();
		try {
			network = TopologyDiscoverer.getTopology();
		} catch (RemoteException e) {
			Logger.error("Failed to get information from network", e);
		}

		calculateLinkCosts();
		setRoutes();
	}

	/**
	 * Method to call the network agents to give the graph to me. Updates
	 * instance variable with new graphobject
	 * 
	 */
	public DirectedWeightedMultigraph<Vertex, Link> getTopology() {
		try {
			network = TopologyDiscoverer.getTopology();
			// getting the informations in the db who fit to the links in the
			// graphobject
			if (network != null) {
				Logger.debug(network.toString());
				Set<Vertex> VertexSet = network.vertexSet();
				Set<Link> LinkSet = new HashSet<Link>();
				// create a List with all Links
				for (Vertex Vertex : VertexSet) {
					Set<Link> LinklistTemp = network.incomingEdgesOf(Vertex);
					for (Link Link : LinklistTemp) {
						LinkSet.add(Link);
					}
				}
			} else {
				Logger.error("Network is null");
			}

			return network;
		} catch (Exception e) {
			play.Logger.error("Failed to get information from network", e);
			return null;
		}
	}

	/**
	 * Creates a new Metric
	 * 
	 * @param name
	 *            the name of the new metric
	 * @param unit
	 *            the unit (just as additional information)
	 * @param defaultValue
	 *            the defaultValue is used if there is no feed
	 * @return boolean true: everything is going well; false: mistake
	 */
	public boolean newMetric(String name, String unit, int defaultValue) {
		Logger.debug("Started to create new metric");
		boolean check = true;
		for (int i = 1; i <= metrics.size(); i++) {
			if (name.equals(metrics.get(i).getName())) {
				Logger.error("Metric already existing");
				check = false;
			}
		}
		if (check) {
			check = MetricAgent.createMetric(name, unit, defaultValue);
			metrics.add(Metric.getFind().ref(name));
		}
		return check;
	}

	/**
	 * This method calculates the costs of all links. It first creates a list of
	 * all links. Then it calculates for every Link the costs. It uses the
	 * String from Program.Properties replaces the variables with there real
	 * values and calculates it values. Then it sets the value of the Link to
	 * the calculated costs.
	 * 
	 * Evaluating Formula is done by API from http://kaiwitte.org/ Documentation
	 * under http://kaiwitte.org/pr/jfunction/javadoc/
	 */
	private void calculateLinkCosts() {
		Logger.debug("Started to calculate Link costs");
		if (network != null) {
			Logger.debug("calculating Link Costs");
			Set<Vertex> VertexSet = network.vertexSet();

			Set<Link> LinkSet = new HashSet<Link>();
			// create a List with all Links
			for (Vertex Vertex : VertexSet) {
				Set<Link> LinklistTemp = network.incomingEdgesOf(Vertex);
				for (Link Link : LinklistTemp) {
					LinkSet.add(Link);
				}
			}

			for (Link l : LinkSet) {
				HashMap<String, String> hashmap = new HashMap<String, String>();

				hashmap = LinkAgent.getMetricFeed(l);
				if (hashmap != null) {
					l.setMetricFeedRelation(hashmap);
				}

				int value = 0;
				String Formulaactually = formula;
				String newFormula = "";
				for (Metric m : metrics) {

					String feedname = l.getMetricFeedRelation(m.getName());
					// String defaultLinkValue =
					// Link.getMetricDefaultLinkValueRelation(m.getName());
					// this coloumn is used if we implemente a not default value for
					// a metric without having a feed
					// the user can type in a numerical value he wants for this link (and only this link)
					int a;
					if (feedname != null) {
						a = controllers.database.FeedAgent.readValues(feedname);
					} else {
						a = m.getDefaultValue();
					}
					String s = new Integer(a).toString();
					newFormula = Formulaactually.replace(m.getName(), s);
				}

				try {
					value = (int) Terms.evaluate(newFormula);
				} catch (FunctionFormatException e) {
					Logger.error(
							"Formula in application.properties is not a valid Formula. Maybe there are names for metrics used which don't exist.",
							e);
				}

				Logger.debug("Link in question is:\n " + l.toString());
				Logger.debug("Set edge weight from " + network.getEdgeWeight(l)
						+ " to " + value);

				network.setEdgeWeight(l, value);

			}
		}

		Logger.debug("Done with calculating Link costs");
	}

	/**
	 * This method calculates the costs of all links. It first creates a list of
	 * all links.
	 */
	private void setRoutes() {
		Logger.debug("Started to set Routes");
		if (network != null) {
			Set<Vertex> VertexSet = network.vertexSet();

			for (Vertex start : VertexSet) {
				if (network.edgesOf(start).size() > 1) {
					for (Vertex end : VertexSet) {
						if (start != end) {
							List<Link> path = DijkstraShortestPath
									.findPathBetween(network, start, end);
							if (path.isEmpty()) {
								Logger.info("No Path between " + start
										+ " and " + end);
							} else {
								Link next = path.get(0);
								Vertex nextV = network.getEdgeTarget(next);
								Link lastLink = path.get(path.size() - 1);
								Vertex nextToLast = network
										.getEdgeSource(lastLink);
								Logger.debug("Route calculated. Now setting in network.");
								if (path.size() > 1) {
									try {
										TopologyDiscoverer.setRoute(start,
												nextV, nextToLast, end);
										Logger.debug("Set route from \n"
												+ start + " to  \n" + end
												+ " via \n" + nextV);
									} catch (RemoteException e) {
										Logger.error("Failed to setRoute from "
												+ start + " to " + end
												+ " via " + nextV, e);
									}
								}
							}
						}
					}
				}
			}
		}
		Logger.debug("Done with calculating Link costs");
		
	}

	public boolean newFeed(Feed created) {
		boolean check = false;
		check = controllers.database.FeedAgent.createFeed(created.getName(),
				created.getUrl(), created.getUser(), created.getPass(),
				created.getXPath());
		if (check) {
			feeds.add(created);
		}
		return check;
	}

	public boolean newMetric(Metric created) {
		boolean check = false;
		check = controllers.database.MetricAgent
				.createMetric(created.getName(), created.getUnit(),
						created.getDefaultValue());
		if (check) {
			metrics.add(created);
		}
		return check;
	}

	public boolean addRelLinkMetricFeed(LinkToMetric rel) {
		Set<Vertex> VertexSet = network.vertexSet();
		for (Vertex start : VertexSet) {
			if (start.getHostname().equals(rel.getStart())) {
				for (Vertex end : VertexSet) {
					if (end.getHostname().equals(rel.getEnd())) {
						Set<Link> Links = network.getAllEdges(start, end);

						for (Link l : Links) {
							l.setMetricFeedRelation(rel.getFName(),
									rel.getMName());
							Logger.debug(rel.getStart() + " " + rel.getEnd()
									+ " " + rel.getMName() + " "
									+ rel.getFName() + " ");
							Logger.debug(l.getSourceIntPort()
									+ l.getSourceIPaddress()
									+ l.getTargetIntPort()
									+ l.getTargetIPaddress()
									+ l.getSubnetAddress());

							LinkAgent.insertLinkWithRelationships(
									l.getSourceIntPort(),
									l.getSourceIPaddress(),
									l.getTargetIntPort(),
									l.getTargetIPaddress(),
									l.getSubnetAddress(), rel.getMName(),
									rel.getFName());

							return true;
						}

					}
				}
			}
		}
		Logger.error("Was not able to create: " + rel.getStart() + " "
				+ rel.getEnd() + " " + rel.getMName() + " " + rel.getFName());
		return false;

	}

	public void traceroute(Trc trc) {
		traceroute.from = trc.from;
		traceroute.to = trc.to;

	}

	public List<Link> calculateTraceroute() {
		String from = traceroute.from;
		String to = traceroute.to;
		Set<Vertex> VertexSet = network.vertexSet();
		for (Vertex start : VertexSet) {
			if (start.getHostname().equals(from)) {
				for (Vertex end : VertexSet) {
					if (end.getHostname().equals(to)) {
						List<Link> path = DijkstraShortestPath.findPathBetween(
								network, start, end);

						return path;
					}
				}
			}

		}

		return null;

	}

	public List<Metric> getAllMetrics() {
		metrics = controllers.database.MetricAgent.getAllMetrics();
		return metrics;
	}

	public List<Feed> getAllFeeds() {
		feeds = controllers.database.FeedAgent.getAllFeeds();
		return feeds;
	}

	public ApplicationProperties getApplicationProperties() {
		return properties;
	}

}
