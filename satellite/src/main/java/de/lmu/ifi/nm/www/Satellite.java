package de.lmu.ifi.nm.www;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.networking.ITopologyDiscoverer;
import de.lmu.ifi.nm.www.Link;
import de.lmu.ifi.nm.www.Vertex;


/**
 * The Satellite ...
 * ... manages the LAN of a specific site by the help of its network agents.
 * ... receives events from the TopologyDiscoverer, e.g. 'Discover local network topology'.
 * ... sends events to the TopologyDiscoverer, e.g. 'Link XY of Network Element Z is down' and
 * ... is implemented as RMI-Server -and Client.
 * 
 * Example call chain:
 * 
 *              	  getTopology()              getTopology()    
 * TopologyDiscoverer ---------------> Satellite -------------> CiscoAgent
 *              	  receiveEvent()             sendEvent()  
 *              	  <---------------           <-----------
 * 
 * @author Dawin Schmidt, dawin.schmidt@lmu.de
 * @since October 2, 2013
 */
public class Satellite implements ISatellite {
	private static String satelliteName = null;
	private static String satelliteIP = null;
	private static int satellitePort;

	private ITopologyDiscoverer myTopologyDiscoverer;
	private static String topologyDiscovererIP = null;
	private static int topologyDiscovererPort;

	private static String rootElementUserName = null;
	private static String rootElementPassword = null;
	private static String rootElementIP = null;

	private List<INetworkAgent> myAgents = new ArrayList<INetworkAgent>();
	public static DirectedWeightedMultigraph<Vertex, Link> myGraph = new DirectedWeightedMultigraph<Vertex, Link>(
			Link.class);
	private static Logger myLogger = null;

	/**
	 * Setup RMI-Server and connect to the TopologyDiscoverer via Java RMI.
	 * 
	 * @param satelliteName
	 *            - The Satellite's name.
	 * @param satelliteIP
	 *            - The local IP-Address to setup the RMI-Server.
	 * @param satellitePort
	 *            - The local port number to setup the RMI-Server.
	 * @param myTopologyDiscovererIP
	 *            - Destination IP-Address to connect to TopoDis via Java RMI.
	 * @param myTopologyDiscovererPort
	 *            - Destination port number to connect to TopoDis via RMI.
	 */
	public Satellite(String satelliteName, String satelliteIP,
			int satellitePort, String myTopologyDiscovererIP,
			int myTopologyDiscovererPort) {
		Satellite.satelliteName = satelliteName;

		try {
			/*
			 * Workaround for 'unexpected hostname and/or port number'
			 * exception. See
			 * http://docs.oracle.com/javase/1.5.0/docs/guide/rmi/
			 * faq.html#domain
			 */
			System.setProperty("java.rmi.server.hostname", satelliteIP);

			// Log to console
			RemoteServer.setLog(System.out);

			// LocateRegistry.createRegistry(satellitePort);

			// Graceful RMI registry creation/reuse
			RMIRegistry myRMIRegistry = new RMIRegistry(satellitePort, 1300,
					1400);
			myRMIRegistry.selectGracefully();

			ISatellite stub = (ISatellite) UnicastRemoteObject.exportObject(
					this, 0);
			Registry registry = LocateRegistry.getRegistry(myRMIRegistry
					.getPort());
			registry.rebind(satelliteName, stub);

			// Get myTopologyDiscoverer remote service
			myTopologyDiscoverer = (ITopologyDiscoverer) Naming.lookup("//"
					+ myTopologyDiscovererIP + ":" + myTopologyDiscovererPort
					+ "/TopologyDiscoverer");

			// Call remote method connect()
			myTopologyDiscoverer.connect(satelliteIP, satellitePort,
					satelliteName);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Call remote receiveEvent() method to send an event.
	 * 
	 * @param satelliteName
	 *            - The Satellite's name who sends the message.
	 * @param message
	 *            - The actual message, e.g. 'Link XY on device Z is down'.
	 * @throws RemoteException
	 *             A RemoteException is the common superclass for a number of
	 *             communication-related exceptions that may occur during the
	 *             execution of a remote method call.
	 */
	public void sendEvent(String satelliteName, String message)
			throws RemoteException {
		getServer().receiveEvent(satelliteName, message);
	}

	/**
	 * Call remote disconnect() method to remove Satellite from the
	 * TopologyDiscoverer's ArrayList.
	 * 
	 * @throws RemoteException
	 *             A RemoteException is the common superclass for a number of
	 *             communication-related exceptions that may occur during the
	 *             execution of a remote method call.
	 */
	public void disconnect() throws RemoteException {
		getServer().disconnect(satelliteIP, satellitePort, getSatelliteName());
		getLogger().debug(
				"Removing Satellite from TopologyDiscoverer queue ...");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see satellite.ISatellite#setRoute(satellite.Vertex, satellite.Vertex,
	 * satellite.Vertex, satellite.Vertex)
	 */
	public boolean setRoute(Vertex source, Vertex next, Vertex secondToLast,
			Vertex destination) throws RemoteException {
		String destNetwork = "";
		String sourceAddress = source.getManagementIP();

		for (Vertex v : myGraph.vertexSet()) {
			if (v.getHostname().equals(source.getHostname())) {
				source = v;
				// getLogger().info("\n source: " + source.hashCode() + " and "
				// + v.hashCode());
			}
			if (v.getHostname().equals(next.getHostname())) {
				next = v;
				// getLogger().info("\n next: " + next.hashCode() + " and " +
				// v.hashCode());
			}
			if (v.getHostname().equals(secondToLast.getHostname())) {
				secondToLast = v;
				// getLogger().info("\n secondToLast: " +
				// secondToLast.hashCode() +
				// " and " + v.hashCode());
			}
			if (v.getHostname().equals(destination.getHostname())) {
				destination = v;
				// getLogger().info("\n destination: " + destination.hashCode()
				// + " and " + v.hashCode());
			}
		}

		Link secondToLastToDestination = myGraph.getEdge(secondToLast,
				destination);

		if (secondToLastToDestination != null) {
			destNetwork = secondToLastToDestination.getSubnetAddress();
		} else {
			getLogger().error("Could not determine subnet address!");
			return false;
		}

		int destPrefixLength = secondToLastToDestination.getSubnetPrefix();

		if (destPrefixLength == 0) {
			getLogger().error("Could not determine destination prefix length!");
			return false;
		}

		Link sourceToNextHop = myGraph.getEdge(source, next);
		String nextHopAddress = sourceToNextHop.getTargetIPaddress();

		getLogger().debug(
				"\n Node where to configure route: " + sourceAddress + "\n"
						+ " Command: " + "ip route " + destNetwork + " "
						+ destPrefixLength + " " + nextHopAddress);

		Link myLink = myGraph.getEdge(source, next);

		for (INetworkAgent agent : myAgents) {
			return agent.setRoute(sourceAddress, destNetwork, destPrefixLength,
					nextHopAddress, myLink.getSourceIntPort());
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see satellite.ISatellite#isAlive()
	 */
	public boolean isAlive() throws RemoteException {
		return true;
	}

	/**
	 * @return The remote TopologyDiscoverer service object.
	 */
	public ITopologyDiscoverer getServer() {
		return myTopologyDiscoverer;
	}

	/**
	 * Adds an Network Agent to the ArrayList 'myAgents'.
	 * 
	 * @param agent
	 *            - The Network Agent that needs to be added to the ArrayList
	 *            'myAgents'.
	 */
	public void addNetworkAgent(INetworkAgent agent) {
		myAgents.add(agent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see satellite.ISatellite#getTopology()
	 */
	public DirectedWeightedMultigraph<Vertex, Link> getTopology()
			throws RemoteException {
		for (INetworkAgent agent : myAgents) {
			agent.getTopology();
		}

		return myGraph;
	}

	/**
	 * @return The logger object.
	 */
	private static Logger getLogger() {
		return myLogger;
	}

	/**
	 * Parse the command line options. If the required arguments are not
	 * provided then this method will call System.exit(1). The required
	 * arguments are: 
	 * '--satname' for Satellite name;
	 * '--satip' for Satellite local IP-Address;
	 * '--satport' for Satellite port number;
	 * '--topoip' for TopologyDiscoverer IP-Address;
	 * '--topoport' for TopologyDiscoverer port number;
	 * "--addr" for root element address; 
	 * "--user" for root element username and 
	 * "--pass" for root element password.
	 * 
	 * @param args
	 *            The args string passed into the method.
	 **/
	public static void parseCommandLine(String[] args) {
		boolean usage = false;

		if (args.length == 0) {
			usage = true;
		} else {
			for (int i = 0; i + 1 < args.length; i += 2) {
				if ((args[i].equals("-sn")) || (args[i].equals("--satname"))) {
					satelliteName = args[i + 1];
				} else if ((args[i].equals("-si"))
						|| (args[i].equals("--satip"))) {
					satelliteIP = args[i + 1];
				} else if ((args[i].equals("-sp"))
						|| (args[i].equals("--satport"))) {
					satellitePort = Integer.parseInt(args[i + 1]);
				} else if ((args[i].equals("-ti"))
						|| (args[i].equals("--topoip"))) {
					topologyDiscovererIP = args[i + 1];
				} else if ((args[i].equals("-tp"))
						|| (args[i].equals("--topoport"))) {
					topologyDiscovererPort = Integer.parseInt(args[i + 1]);
				} else if ((args[i].equals("-a")) || (args[i].equals("--addr"))) {
					rootElementIP = args[i + 1];
				} else if ((args[i].equals("-u")) || (args[i].equals("--user"))) {
					rootElementUserName = args[i + 1];
				} else if ((args[i].equals("-p")) || (args[i].equals("--pass"))) {
					rootElementPassword = args[i + 1];
				} else {
					usage = true;
				}
			}
		}

		if ((usage) || (satelliteName == null) || (satelliteIP == null)
				|| topologyDiscovererIP == null || rootElementIP == null
				|| rootElementUserName == null || rootElementPassword == null) {
			getLogger()
					.info("Usage: -sn <Satellite name> -si <Satellite IP addr> -sp <Satellite port number> -ti <TopologyDiscoverer IP addr> -tp <TopologyDiscoverer port number> -a <root element addr>"
							+ " -u <root element username> -p <root element password> \n");
			System.exit(1);
		}
	}

	/**
	 * Read the connection properties - Satellite name, Satellite IP-Address,
	 * Satellite port number, TopologyDiscoverer IP-Address, TopologyDiscoverer
	 * port number, root element IP-Address, root element username and password
	 * - from a file named 'connection.properties' that is a resource on the
	 * classpath.
	 * 
	 * @return True if the properties are read successfully.
	 */
	public static boolean readProperties() {
		try {
			Properties properties = new Properties();
			
			InputStream inputStream = Satellite.class.getClassLoader()
					.getResourceAsStream("connection.properties");

			properties.load(inputStream);

			satelliteName = properties.getProperty("satName", "Berlin");
			satelliteIP = properties.getProperty("satIP", "localhost");
			satellitePort = Integer.parseInt(properties.getProperty("satPort",
					"1101"));
			topologyDiscovererIP = properties.getProperty("topoDisIP",
					"10.153.7.208");
			topologyDiscovererPort = Integer.parseInt(properties.getProperty(
					"topoDisPort", "1100"));
			rootElementIP = properties.getProperty("rootElement", "10.1.1.4");
			rootElementUserName = properties.getProperty("userName", "user1");
			rootElementPassword = properties.getProperty("password", "pass1");
		} catch (IOException e) {
			getLogger().error(e.getLocalizedMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * @return The Satellite's name.
	 */
	public String getSatelliteName() {
		return satelliteName;
	}

	/**
	 * Create objects, add CiscoAgent to ArrayList 'myAgents' and setup the
	 * shutdown hook.
	 * 
	 * @param args
	 *            The args string passed into the main method.
	 */
	public static void main(String[] args) {
		myLogger = LoggerFactory.getLogger(Satellite.class.getClass());
		
		if (!readProperties()) {
			parseCommandLine(args);
		}

		// Create Satellite object
		final Satellite mySatellite = new Satellite("Berlin", satelliteIP,
				satellitePort, topologyDiscovererIP, topologyDiscovererPort);

		// Create CiscoAgent object
		final CiscoAgent myFirstCiscoAgent = new CiscoAgent(mySatellite,
				"my1stCiscoAgent", rootElementIP, rootElementUserName,
				rootElementPassword);
		
		// Add 'myFirstCiscoAgent' to the ArrayList 'myAgents'
		mySatellite.addNetworkAgent(myFirstCiscoAgent);

		// Shutdown hook for removing Satellite from the
		// TopologyDiscoverer's list
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				try {
					if (!mySatellite.equals(null)) {
						mySatellite.disconnect();
						getLogger()
								.debug("Removing Satellite from TopologyDiscoverer's ArrayList. \n Exiting ...");
					}
				} catch (RemoteException e) {
					getLogger()
							.error("Failed to remove Satellite from TopologyDiscoverer's ArrayList!");
					getLogger().error(e.getMessage());
				}
			}
		}));
	}
}