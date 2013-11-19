package de.lmu.ifi.nm.www;

import java.util.HashMap;

import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * This class represents a link in the topology graph. A link has a source and
 * destination node and a subnet address.
 * 
 * @author Dawin Schmidt, dawin.schmidt@lmu.de
 * @author Werner Hoffmann, werner.hoffmann@lmu.de
 * @version 0.3
 * @since October 2, 2013
 */
public class Link extends DefaultWeightedEdge {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private Vertex sourceNode = null;
	@SuppressWarnings("unused")
	private Vertex targetNode = null;
	private String sourceIntPort = null;
	private String sourceIPaddress = null;
	private String targetIntPort = null;
	private String targetIPaddress = null;
	private String subnetAddress = null;
	private int subnetPrefix;
	private boolean isUp = false;;
	private HashMap<String, String> MetricFeedRelation = null;

	/**
	 * @param sourceIntPort
	 *            - The source interface port.
	 * @param sourceIPaddress
	 *            - The source IP-Address.
	 * @param targetIntPort
	 *            - The destination interface port.
	 * @param targetIPaddress
	 *            - The destination IP-Address.
	 * @param subnetAddress
	 *            - The subnet address.
	 * @param sourceNode
	 *            - The source node.
	 * @param targetNode
	 *            - The destination node.
	 */
	public Link(String sourceIntPort, String sourceIPaddress,
			String targetIntPort, String targetIPaddress, String subnetAddress,
			Vertex sourceNode, Vertex targetNode) {
		super();
		this.sourceIntPort = sourceIntPort;
		this.sourceIPaddress = sourceIPaddress;
		this.targetIntPort = targetIntPort;
		this.targetIPaddress = targetIPaddress;
		this.isUp = true;
		this.subnetAddress = subnetAddress;
		this.sourceNode = sourceNode;
		this.targetNode = targetNode;
		this.MetricFeedRelation = new HashMap<String, String>();

		// Get subnet prefix from source/target interface
		if (!sourceNode.getInterfaceHash().get(sourceIntPort).isEmpty()) {
			setSubnetPrefix(sourceNode.getInterfaceHash().get(sourceIntPort)
					.get(0).getPrefixLen());
		} else if (!sourceNode.getInterfaceHash().get(targetIntPort).isEmpty()) {
			setSubnetPrefix(sourceNode.getInterfaceHash().get(targetIntPort)
					.get(0).getPrefixLen());
		} else {
			setSubnetPrefix(0);
		}
	}

	/**
	 * @param PortIn
	 *            - The source interface port.
	 * @param PortOut
	 *            - The destination interface port.
	 */
	public Link(String PortIn, String PortOut) {
		this.sourceIntPort = PortIn;
		this.targetIntPort = PortOut;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jgrapht.graph.DefaultEdge#toString()
	 */
	@Override
	public String toString() {
		return "\n" + "---------- Link Properties ----------" + "\n"
				+ "Source node \t \t : "
				+ getSource()
				+ "\n"
				+ "Source interface \t : "
				+ getSourceIntPort()
				+ "\n"
				+ "Source IP address \t : "
				+ getSourceIPaddress()
				+ "\n"
				+ "Target node \t \t : "
				+ getTarget()
				+ "\n"
				+ "Target interface \t : "
				+ getTargetIntPort()
				+ "\n"
				+ "Target IP address \t : "
				+ getTargetIPaddress()
				+ "\n"
				+ "Weight \t \t \t : "
				+ getWeight()
				+ "\n"
				+ "Subnet address \t \t : "
				+ getSubnetAddress()
				+ "\n"
				+ "Subnet prefix \t \t : "
				+ getSubnetPrefix();
	}

	/**
	 * Get the Feed of the corresponding metric.
	 * 
	 * @param Metric
	 *            - The metric that is passed to the function.
	 * @return The Feed belonging to the metric.
	 */
	public String getFeed(String Metric) {
		return MetricFeedRelation.get(Metric);
	}

	/**
	 * Delete a feed.
	 * 
	 * @param Metric
	 *            - The metric that is passed to the function.
	 * @return True or false whether the feed has been deleted sucessfully or
	 *         not.
	 */
	public boolean deleteFeed(String Metric) {
		boolean check = false;
		if (MetricFeedRelation.containsKey(Metric)) {
			MetricFeedRelation.remove(Metric);
			check = true;
		}
		return check;
	}

	/**
	 * @return The source interface port of a link.
	 */
	public String getSourceIntPort() {
		return sourceIntPort;
	}

	/**
	 * Set the source interface port.
	 * 
	 * @param sourceIntPort
	 *            - The source interface port to be set.
	 */
	public void setSourceIntPort(String sourceIntPort) {
		this.sourceIntPort = sourceIntPort;
	}

	/**
	 * @return The destination interface port of a link.
	 */
	public String getTargetIntPort() {
		return targetIntPort;
	}

	/**
	 * Set the destination interface port.
	 * 
	 * @param targetIntPort
	 *            - The destination interface port to be set.
	 */
	public void setTargetIntPort(String targetIntPort) {
		this.targetIntPort = targetIntPort;
	}

	/**
	 * @return The IP-Address of the source node.
	 */
	public String getSourceIPaddress() {
		return sourceIPaddress;
	}

	/**
	 * Set the IP-Address of the source node.
	 * 
	 * @param sourceIPaddress
	 *            - The source IP-Address to be set.
	 */
	public void setSourceIPaddress(String sourceIPaddress) {
		this.sourceIPaddress = sourceIPaddress;
	}

	/**
	 * @return The IP-Address of the destination node.
	 */
	public String getTargetIPaddress() {
		return targetIPaddress;
	}

	/**
	 * Set the IP-Address of the destination node.
	 * 
	 * @param targetIPaddress
	 *            - The destination IP-Address to be set.
	 */
	public void setTargetIPaddress(String targetIPaddress) {
		this.targetIPaddress = targetIPaddress;
	}

	/**
	 * @return The subnet address of a link.
	 */
	public String getSubnetAddress() {
		return subnetAddress;
	}

	/**
	 * Set the subnet address of a link.
	 * 
	 * @param subnetAddress
	 *            - The subnet address to be set.
	 */
	public void setSubnetAddress(String subnetAddress) {
		this.subnetAddress = subnetAddress;
	}

	/**
	 * Check whether a link is up or nor.
	 * 
	 * @return True = link is up, false = link is down.
	 */
	public boolean isUp() {
		return isUp;
	}

	/**
	 * Set a link up or down.
	 * 
	 * @param isUp
	 *            - True = set link up, false = set link down.
	 */
	public void setUp(boolean isUp) {
		this.isUp = isUp;
	}

	/**
	 * @return The MetricFeedRelation HashMap.
	 */
	public HashMap<String, String> getMetricFeedRelation() {
		return MetricFeedRelation;
	}

	/**
	 * @param Metric The metric value passed to the function.
	 * @return The corresponding feed that belongs to the metric.
	 */
	public String getMetricFeedRelation(String Metric) {
		return MetricFeedRelation.get(Metric);
	}

	/**
	 * Set the MetricFeedRelation HashMap.
	 * 
	 * @param metricFeedRelation
	 *            - The MetricFeedRelation object that is passed to the
	 *            function.
	 */
	public void setMetricFeedRelation(HashMap<String, String> metricFeedRelation) {
		MetricFeedRelation = metricFeedRelation;
	}

	/**
	 * Add a new metric-feed relation to the HashMap.
	 * 
	 * @param metric
	 *            - The metric to be added.
	 * @param feed
	 *            - The feed to be added.
	 */
	public void setMetricFeedRelation(String metric, String feed) {
		MetricFeedRelation.put(metric, feed);
	}

	/**
	 * @return The prefix of a subnet.
	 */
	public int getSubnetPrefix() {
		return subnetPrefix;
	}

	/**
	 * Set the subnet prefix of a link.
	 * 
	 * @param subnetPrefix
	 *            - The subnet prefix to be set.
	 */
	public void setSubnetPrefix(int subnetPrefix) {
		this.subnetPrefix = subnetPrefix;
	}
}