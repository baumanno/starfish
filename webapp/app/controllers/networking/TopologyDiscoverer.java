package controllers.networking;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jgrapht.graph.DirectedWeightedMultigraph;

import de.lmu.ifi.nm.www.Link;
import de.lmu.ifi.nm.www.Vertex;
import de.lmu.ifi.nm.www.ISatellite;

import play.Logger;

/**
 * The TopologyDiscoverer offers functions for
 * controller.service.Communicator.java and acts as an interface between
 * Communicator and Satellite.
 *
 * It's implemented both as an RMI-Server as well as an RMI-Client. An example
 * call chain could look like this:
 *
 *              getTopology()                       getTopology()
 * Communicator ---------------> TopologyDiscoverer -------------> Satellite
 *              MyLogic.dowork()                    receiveEvent()
 *              <---------------                    <-------------
 *
 * The TopologyDiscoverer can handle multiple Satellites.
 *
 * @author Dawin Schmidt, dawin.schmidt@lmu.de
 * @since October 2, 2013
 */
public class TopologyDiscoverer implements ITopologyDiscoverer {

    private static final String TOPODIS_IP = "localhost";
    private static final int TOPODIS_PORT = 1100;

    // Graph object for storing the network topology
    private DirectedWeightedMultigraph<Vertex, Link> myGraph = null;

    // List for storing Satellites
    private List<ISatellite> satellites = new ArrayList<ISatellite>();

    public TopologyDiscoverer() {
        myGraph = new DirectedWeightedMultigraph<Vertex, Link>(Link.class);

        try {
            /*
             * Workaround for 'unexpected hostname and/or port number'
             * exception. See
             * http://docs.oracle.com/javase/1.5.0/docs/guide/rmi/
             * faq.html#domain
             */
            System.setProperty("java.rmi.server.hostname", TOPODIS_IP);

            // Log to console
            RemoteServer.setLog(System.out);

            // LocateRegistry.createRegistry(TOPODIS_PORT);

            // Graceful RMI registry creation/reuse
            RMIRegistry myRMIRegistry = new RMIRegistry(TOPODIS_PORT, 1200,
                    1300);
            myRMIRegistry.selectGracefully();

            ITopologyDiscoverer stub = (ITopologyDiscoverer) UnicastRemoteObject
                    .exportObject(this, 0);
            Registry registry = LocateRegistry.getRegistry(myRMIRegistry
                    .getPort());
            registry.rebind("TopologyDiscoverer", stub);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Satellites initially connect to the TopologyDiscoverer by calling this RMI
     * function. It adds a new connected satellite to the ArrayList
     * 'satellites'.
     *
     * @param satelliteIP
     *            - Source IPv4-Address of the connected Satellite.
     * @param satPort
     *            - Source-Port of the connected Satellite.
     * @param satName
     *            - The name of the Satellite.
     * @throws RemoteException
     *             A RemoteException is the common superclass for a number of
     *             communication-related exceptions that may occur during the
     *             execution of a remote method call.
     */
    public void connect(String satelliteIP, int satPort, String satName)
            throws RemoteException {
        try {
            ISatellite satellite = (ISatellite) Naming.lookup("//"
                    + satelliteIP + ":" + satPort + "/" + satName);
            satellites.add(satellite);
            Logger.debug("Satellite " + "'" + satName + "'" + " connected.");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Once a Satellite is shutting down it calls this RMI method. It
     * removes the Satellite from ArrayList 'satellites'.
     *
     * @param satelliteIP
     *            - Source IPv4-Address of the connected Satellite.
     * @param satPort
     *            - Source-Port of the connected Satellite.
     * @param satName
     *            - The name of the Satellite.
     * @throws RemoteException
     *             A RemoteException is the common superclass for a number of
     *             communication-related exceptions that may occur during the
     *             execution of a remote method call.
     */
    public void disconnect(String satelliteIP, int satPort, String satName)
            throws RemoteException {
        try {
            ISatellite satellite = (ISatellite) Naming.lookup("//"
                    + satelliteIP + ":" + satPort + "/" + satName);
            satellites.remove(satellite);
            Logger.debug("Removed satellite " + "'" + satName + "'"
                    + " from ArrayList.");
            Logger.debug("Remaining satellites: " + satellites.toString() + ".");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Satellites call this RMI method when an error has occurred on the
     * network.
     *
     * @param satName - The name of the Satellite who has sent the message.
     * @param message
     *            - The actual error message.
     * @throws RemoteException
     *             A RemoteException is the common superclass for a number of
     *             communication-related exceptions that may occur during the
     *             execution of a remote method call.
     */
    public void receiveEvent(String satName, String message)
            throws RemoteException {
        Logger.info("\n Received event from " + satName + ": \n" + message);
    }

    /**
     * Gets the global network topology by calling the remote procedure
     * getTopology() for every Satellite stored in the list.
     *
     * @return The global topology of the network.
     * @throws RemoteException
     *             A RemoteException is the common superclass for a number of
     *             communication-related exceptions that may occur during the
     *             execution of a remote method call.
     */
    @SuppressWarnings("unchecked")
    public DirectedWeightedMultigraph<Vertex, Link> getTopology()
            throws RemoteException {
        Logger.info(satellites.toString());
        for (ISatellite sat : satellites) {
            myGraph = (DirectedWeightedMultigraph<Vertex, Link>) sat
                    .getTopology();
            return myGraph;
        }
        return null;
    }

    /**
     * Sets a static route on a network element.
     *
     * @param source
     *            - The Source network element where to configure the route.
     * @param next
     *            - The neighbor network element for getting the next-hop
     *            IP-Address.
     * @param secondToLast
     *            - The second to last network element for calculating the
     *            destination network.
     * @param destination
     *            - The destination network element for calculating the
     *            destination network.
     * @return True or false whether a route has been placed successfully or not.
     * @throws RemoteException
     *             A RemoteException is the common superclass for a number of
     *             communication-related exceptions that may occur during the
     *             execution of a remote method call.
     */
    public boolean setRoute(Vertex source, Vertex next, Vertex secondToLast,
            Vertex destination) throws RemoteException {
        for (ISatellite sat : satellites) {
            return sat.setRoute(source, next, secondToLast, destination);
        }
        return false;
    }

    /**
     * Returns the topology graph object.
     *
     * @return The graph object where the network topology is stored.
     */
    public DirectedWeightedMultigraph<Vertex, Link> getMyGraph() {
        return myGraph;
    }

    /**
     * Calls the remote procedure 'isAlive()' for each Satellite. If an exception
     * occurs remove the Satellite from the ArrayList.
     */
    public void checkSatellites() {
        for (Iterator<ISatellite> iter = satellites.iterator(); iter.hasNext();) {
            ISatellite satellite = iter.next();
            try {
                if (satellite.isAlive()) {
                    continue;
                }
            } catch (RemoteException e) {
                Logger.error("Could not call remote method isAlive()!");
                Logger.error("Removing Satellite from ArrayList!");
                iter.remove();
            }
        }
        Logger.debug("New ArrayList of satellites: " + "\n"
                + satellites.toString());
    }
}
