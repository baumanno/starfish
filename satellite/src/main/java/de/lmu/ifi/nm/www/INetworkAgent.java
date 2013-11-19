package de.lmu.ifi.nm.www;

/**
 * This interface defines a network agent. A network agent discovers the
 * network topology, sets static routes on network devices and converts a
 * proprietary graph object to an open one (e.g. JGraphT).
 * 
 * @author Dawin Schmidt, dawin.schmidt@lmu.de
 * @since October 2, 2013
 */
public interface INetworkAgent {
	/**
	 * Discover the network topology. Will be called by the Satellite.
	 */
	public void getTopology();
	
	/**
	 * Set a static route on a network element.
	 * 
	 * @param source
	 *            - The source IP-Address of the network element where to
	 *            configure the route.
	 * @param destNet
	 *            - The destination network address.
	 * @param destSubnetMask
	 *            - The destination subnet mask or prefix.
	 * @param nextHop
	 *            - The IP-Address of the next hop.
	 * @param sourceInt
	 *            - The source interface of the network element where to
	 *            configure the route.
	 * @return True or false whether a route has been placed successfully or
	 *         not.
	 */
	public boolean setRoute(String source, String destNet, int destSubnetMask,
			String nextHop, String sourceInt);

	/**
	 * Convert a proprietary topology graph object to a JGraphT graph object.
	 */
	public void toJGraphT();
}