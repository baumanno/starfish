package de.lmu.ifi.nm.www;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.jgrapht.graph.DirectedWeightedMultigraph;

/**
 * Java RMI Interface for the Satellite class.
 * 
 * @author Dawin Schmidt, dawin.schmidt@lmu.de
 * @since October 2, 2013
 */
public interface ISatellite extends Remote {
	/**
	 * Get the site's network topology by asking all available Network Agents
	 * to discover the LAN. The TopologyDiscoverer will call this method while
	 * discovering the global network topology.
	 * 
	 * @return The topology graph object of the site's LAN.
	 * @throws RemoteException
	 *             A RemoteException is the common superclass for a number of
	 *             communication-related exceptions that may occur during the
	 *             execution of a remote method call.
	 */
	public DirectedWeightedMultigraph<?, ?> getTopology()
			throws RemoteException;

	/**
	 * Set a static route on a network element. The TopologyDiscoverer can call
	 * this method.
	 * 
	 * @param source
	 *            - The source network element where to configure the route.
	 * @param next
	 *            - The neighbor network element for getting the next-hop
	 *            IP-Address.
	 * @param nextToLast
	 *            - The second to last network element for calculating the
	 *            destination network.
	 * @param destination
	 *            - The destination network element for calculating the
	 *            destination network.
	 * @return True or false whether a route has been placed successfully or
	 *         not.
	 * @throws RemoteException
	 *             A RemoteException is the common superclass for a number of
	 *             communication-related exceptions that may occur during the
	 *             execution of a remote method call.
	 */
	public boolean setRoute(Vertex source, Vertex next, Vertex nextToLast,
			Vertex destination) throws RemoteException;

	/**
	 * The TopologyDiscoverer will use this remote method to check whether or
	 * not a Satellite is still reachable.
	 * 
	 * @return True or False.
	 * @throws RemoteException
	 *             A RemoteException is the common superclass for a number of
	 *             communication-related exceptions that may occur during the
	 *             execution of a remote method call.
	 */
	public boolean isAlive() throws RemoteException;
}