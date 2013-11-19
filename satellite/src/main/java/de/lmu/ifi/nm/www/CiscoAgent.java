package de.lmu.ifi.nm.www;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisco.onep.cdp.CDPEvent;
import com.cisco.onep.cdp.CDPListener;
import com.cisco.onep.core.exception.OnepConnectionException;
import com.cisco.onep.core.exception.OnepException;
import com.cisco.onep.core.exception.OnepIllegalArgumentException;
import com.cisco.onep.core.exception.OnepInvalidSettingsException;
import com.cisco.onep.core.exception.OnepRemoteProcedureException;
import com.cisco.onep.element.NetworkApplication;
import com.cisco.onep.element.NetworkElement;
import com.cisco.onep.element.SessionConfig;
import com.cisco.onep.element.SessionConfig.SessionTransportMode;
import com.cisco.onep.interfaces.InterfaceFilter;
import com.cisco.onep.interfaces.InterfaceStateEvent;
import com.cisco.onep.interfaces.InterfaceStateListener;
import com.cisco.onep.interfaces.InterfaceStatus.InterfaceStateEventType;
import com.cisco.onep.interfaces.NetworkInterface;
import com.cisco.onep.interfaces.NetworkPrefix;
import com.cisco.onep.routing.ARTRouteStateEvent;
import com.cisco.onep.routing.ARTRouteStateListener;
import com.cisco.onep.routing.AppRouteTable;
import com.cisco.onep.routing.L3UnicastNextHop;
import com.cisco.onep.routing.L3UnicastRIBFilter;
import com.cisco.onep.routing.L3UnicastRoute;
import com.cisco.onep.routing.L3UnicastRouteOperation;
import com.cisco.onep.routing.L3UnicastRouteRange;
import com.cisco.onep.routing.L3UnicastScope;
import com.cisco.onep.routing.RIB;
import com.cisco.onep.routing.RIBRouteStateEvent;
import com.cisco.onep.routing.RIBRouteStateListener;
import com.cisco.onep.routing.Route;
import com.cisco.onep.routing.RouteOperation;
import com.cisco.onep.routing.RouteRange;
import com.cisco.onep.routing.Routing;
import com.cisco.onep.routing.L3UnicastScope.AFIType;
import com.cisco.onep.routing.L3UnicastScope.SAFIType;
import com.cisco.onep.routing.RIB.RouteStateListenerFlag;
import com.cisco.onep.routing.RouteOperation.RouteOperationType;
import com.cisco.onep.topology.Edge;
import com.cisco.onep.topology.Edge.EdgeType;
import com.cisco.onep.topology.Graph;
import com.cisco.onep.topology.Topology;
import com.cisco.onep.topology.TopologyEvent;
import com.cisco.onep.topology.TopologyFilter;
import com.cisco.onep.topology.TopologyListener;
import com.cisco.onep.topology.Topology.TopologyType;
import com.cisco.onep.topology.TopologyEvent.TopologyEventType;

/**
 * The CiscoAgent is the implementation of a network agent and is instantiated by the Satellite.
 * 
 * It uses Cisco's onePK-SDK to ...
 * ... query necessary information of network elements
 * ... set static routes on network elements and
 * ... handle network events.
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
public class CiscoAgent implements INetworkAgent {
	private Satellite mySatellite = null;
	private RIBRouteListener myRIBRouteListener = null;
	private ARTRouteListener myARTRouteListener = null;
	// private InterfaceStateListener myInterfaceStateListener = null;
	@SuppressWarnings("unused")
	private CDPListenerImplementer myCDPListener = null;
	// private TopologyListenerImplementer myTopologyListener = null;
	private String seedAddress = null;
	private String userName = null;
	private String password = null;
	private HashMap<String, List<de.lmu.ifi.nm.www.NetworkPrefix>> interfaceHash = null;
	private HashMap<String, String> nodesHash = null;
	private NetworkElement myNetworkElement = null;
	private SessionConfig mySessionConf = null;
	private NetworkApplication myNetworkApplication = null;
	private double latitude;
	private double longitude;
	private Graph myCdpGraph = null;
	private List<List<InetAddress>> tailNodeConnectorList = null;;
	private Logger myLogger = null;

	/**
	 * Load the Logger, set onPK application name and create a CDPListener
	 * object.
	 * 
	 * @param mySatellite
	 *            - The Satellite object which instantiated CiscoAgent
	 * @param applicationName
	 *            - The name of the onePK application.
	 * @param seedAddress
	 *            - The IP-Address of the root network element. Used to start
	 *            network discovery.
	 * @param userName
	 *            - The username to authorize with the network element. Needs to
	 *            be configured on the network device.
	 * @param password
	 *            - The password to authorize with the network element. Needs to
	 *            be configured on the network device.
	 */
	public CiscoAgent(Satellite mySatellite, String applicationName,
			String seedAddress, String userName, String password) {
		this.mySatellite = mySatellite;
		this.seedAddress = seedAddress;
		this.userName = userName;
		this.password = password;
		myLogger = LoggerFactory.getLogger(this.getClass());

		try {
			myNetworkApplication = NetworkApplication.getInstance();
			myNetworkApplication.setName(applicationName);
		} catch (OnepInvalidSettingsException e) {
			getLogger().error("\n\n--- Exception Text ---\n" + e.getMessage());
			getLogger().error(
					"\n\n--- Localized Exception Text ---\n"
							+ e.getLocalizedMessage());
			System.exit(1);
		}

		mySessionConf = new SessionConfig(SessionTransportMode.SOCKET);
		myCDPListener = new CDPListenerImplementer();
		nodesHash = new HashMap<String, String>();
	}

	/**
	 * TopologyListener implemented as inner class.
	 */
	private class TopologyListenerImplementer implements TopologyListener {

		/**
		 * Handle TopologyEvents. Call mySatellite.sendEvent() when an event has
		 * occurred.
		 * 
		 * @param event
		 *            The TopologyEvent.
		 * @param clientData
		 *            Data passed in when the listener was registered.
		 * 
		 * @see com.cisco.onep.topology.TopologyListener#handleEvent(com.cisco.onep
		 *      .topology.TopologyEvent, java.lang.Object)
		 */
		public synchronized void handleEvent(TopologyEvent event,
				Object clientData) {
			getLogger().info("Received TopologyEvent.");

			if (event.getType().contains(TopologyEventType.EDGES_ADD)) {
				getLogger().info("Some edges have been added.");
				try {
					mySatellite.sendEvent(mySatellite.getSatelliteName(),
							"Some edges have been added.");
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}

			if (event.getType().contains(TopologyEventType.EDGES_DELETE)) {
				getLogger().info("Some edges have been deleted.");
				try {
					mySatellite.sendEvent(mySatellite.getSatelliteName(),
							"Some edges have been deleted.");
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}

			getLogger().info(
					"The number of edges in the new topology is "
							+ event.getEdgeList().size());
		}
	}

	/**
	 ** InterfaceStateListener implemented as inner class.
	 **/
	private class InterfaceStateListenerImplementer implements
			InterfaceStateListener {

		/**
		 * Handle InterfaceStateEvent. Call mySatellite.sendEvent() when an
		 * event has occurred.
		 * 
		 * @param event
		 *            The InterfaceStateEvent.
		 * @param clientData
		 *            Data passed in when the listener was registered.
		 * 
		 * @see com.cisco.onep.interfaces.InterfaceStateListener#handleEvent(com.cisco.onep.interfaces.InterfaceStateEvent,
		 *      java.lang.Object)
		 */
		public synchronized void handleEvent(InterfaceStateEvent event,
				Object clientData) {
			getLogger().info("InterfaceStateEvent received:");
			getLogger().info("Interface: " + event.getInterface());
			getLogger().info("State: " + event.getState());
			getLogger().info("State event type: " + event.getStateEventType());

			try {
				mySatellite.sendEvent(
						mySatellite.getSatelliteName(),
						"Interface "
								+ event.getInterface().getName()
								+ " of "
								+ event.getNetworkElement().getProperty()
										.getSysName() + " was set to "
								+ event.getState() + ".");
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (OnepRemoteProcedureException e) {
				e.printStackTrace();
			} catch (OnepConnectionException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 ** CDPListener implemented as inner class.
	 **/
	private class CDPListenerImplementer implements CDPListener {
		/**
		 * Handle the CDPEvents. Add addresses to HashMap 'nodesHash' to
		 * progress network discovery.
		 * 
		 * @param cdpEvent
		 *            The CDPEvent from the network element.
		 * @param clientData
		 *            An Object that may be passed in during event registration.
		 * @see com.cisco.onep.cdp.CDPListener#handleEvent(com.cisco.onep.cdp.CDPEvent,
		 *      java.lang.Object)
		 */
		public synchronized void handleEvent(CDPEvent cdpEvent,
				Object clientData) {
			List<InetAddress> addressList = cdpEvent.getAddresses();
			for (InetAddress address : addressList) {
				String neighborAddress = address.getHostAddress();
				addElementAddress(neighborAddress);
			}
		}
	}

	/**
	 ** RIBRouteStateListener implemented as inner class.
	 **/
	private class RIBRouteListener implements RIBRouteStateListener {
		/**
		 * Handle RIBRouteStateEvent.
		 * 
		 * @param event
		 *            The RIBRouteStateEvent from the network element.
		 * @param clientData
		 *            An Object that may be passed in during event registration.
		 * @see com.cisco.onep.routing.RIBRouteStateListener#handleEvent(com.cisco.onep.routing.RIBRouteStateEvent,
		 *      java.lang.Object)
		 */
		public synchronized void handleEvent(RIBRouteStateEvent event,
				Object clientData) {
			try {
				getLogger().info(
						"Received RIBRouteStateEvent on "
								+ event.getNetworkElement().getProperty()
										.getSysName());
			} catch (OnepRemoteProcedureException e) {
				e.printStackTrace();
			} catch (OnepConnectionException e) {
				e.printStackTrace();
			}

			getLogger().info("Scope: " + event.getScope());
			getLogger().info("Route: " + event.getRoute());
		}
	}

	/**
	 ** ARTRouteStateListener implemented as inner class.
	 **/
	private class ARTRouteListener implements ARTRouteStateListener {
		/**
		 * Handle ARTRouteStateEvent.
		 * 
		 * @param event
		 *            The RIBRouteStateEvent from the network element.
		 * @param clientData
		 *            An Object that may be passed in during event registration.
		 * @see com.cisco.onep.routing.ARTRouteStateListener#handleEvent(com.cisco.onep.routing.ARTRouteStateEvent,
		 *      java.lang.Object)
		 */
		public synchronized void handleEvent(ARTRouteStateEvent event,
				Object clientData) {
			getLogger().info("ARTRouteStateEvent received:");
			getLogger().info("Scope: " + event.getScope());
			getLogger().info("Route: " + event.getRoute());
		}
	}

	/**
	 * Register InterfaceStateListener on a network element.
	 * 
	 * @param networkElement
	 *            - The network element object where to register the listener.
	 * @throws OnepException
	 *             OnepException represents a general exception to be thrown by
	 *             ONE-P API when an unrecoverable error is encountered.
	 */
	private void registerInterfaceStateListener(NetworkElement networkElement)
			throws OnepException {
		getLogger().info("Adding InterfaceState listener.");
		networkElement.addInterfaceStateListener(
				new InterfaceStateListenerImplementer(), new InterfaceFilter(),
				InterfaceStateEventType.ONEP_IF_STATE_EVENT_ANY, null);
	}

	/**
	 * Add an TopologyListener to the topology object passed in.
	 * 
	 * @param topology
	 *            - The topology object to which the ExampleTopologyListener is
	 *            added.
	 * @throws OnepException
	 *             OnepException represents a general exception to be thrown by
	 *             ONE-P API when an unrecoverable error is encountered.
	 */
	public void registerTopologyListener(Topology topology)
			throws OnepException {
		// The Event Filter object allows the application to listen to specific
		// topology change events corresponding to
		// a topology object. When a topology change event matches with the
		// specified filter criteria the application is
		// notified through the registered callback.
		List<TopologyEventType> eventType = new ArrayList<TopologyEventType>();
		eventType.add(TopologyEventType.EDGES_ADD);
		eventType.add(TopologyEventType.EDGES_DELETE);
		TopologyFilter filter = new TopologyFilter(eventType);
		topology.addTopologyListener(new TopologyListenerImplementer(), filter,
				null);
		getLogger().info("Adding topology listener.");
	}

	/**
	 * Adds an Application Routing Table Route Listener.
	 * 
	 * @param networkElement
	 *            - Network element where to add the listener.
	 * @throws OnepException
	 *             OnepException represents a general exception to be thrown by
	 *             ONE-P API when an unrecoverable error is encountered.
	 */
	private void registerARTRouteListener(NetworkElement networkElement)
			throws OnepException {
		AppRouteTable appRouteTable = getAppRouteTable();

		L3UnicastScope aL3UnicastScope = new L3UnicastScope("", AFIType.IPV4,
				SAFIType.UNICAST, "base");

		myARTRouteListener = new ARTRouteListener();
		getLogger().info("Adding ART listener.");
		appRouteTable.addRouteStateListener(myARTRouteListener,
				aL3UnicastScope, null);
	}

	/**
	 * Adds a RIB Route listener. When events arrive, listener.handleEvent()
	 * will be invoked.
	 * 
	 * @param networkElement
	 *            - Network element where to add the listener.
	 * @throws OnepException
	 *             OnepException represents a general exception to be thrown by
	 *             ONE-P API when an unrecoverable error is encountered.
	 */
	private void registerRIBRouteListener(NetworkElement networkElement)
			throws OnepException {
		RIB rib = getRIB();
		L3UnicastScope aL3UnicastScope = new L3UnicastScope("", AFIType.IPV4,
				SAFIType.UNICAST, "base");
		L3UnicastRIBFilter filter = new L3UnicastRIBFilter();

		myRIBRouteListener = new RIBRouteListener();
		getLogger().info("Adding RIB listener.");
		rib.addRouteStateListener(myRIBRouteListener, aL3UnicastScope, filter,
				RouteStateListenerFlag.TRIGER_INITIAL_WALK, null);
	}

	/**
	 * Get the topology graph object of a network element via CDP. New
	 * discovered nodes are added to the global Satellite.myGraph object.
	 */
	private void getTopologyGraphviaCDP() {
		boolean isInVertexSet = false;
		tailNodeConnectorList = new ArrayList<List<InetAddress>>();

		if (!isInVertexSet) {
			// Add vertex to JGraphT graph object
			Satellite.myGraph.addVertex(initVertex());

			try {
				// Get graph object from network element
				Topology myTopology = new Topology(myNetworkElement,
						TopologyType.CDP);

				// registerTopologyListener(myTopology);

				// Concatenate graphs
				if (myCdpGraph == null) {
					myCdpGraph = myTopology.getGraph();
				} else {
					myCdpGraph.concatenate(myTopology.getGraph());
				}

				// Get TailNodeConnectorList to retrieve neighbor addresses
				for (Edge edge : myTopology.getGraph().getEdgeList(
						EdgeType.DIRECTED)) {
					tailNodeConnectorList.add(edge.getTailNodeConnector()
							.getAddressList());
				}
			} catch (OnepConnectionException e) {
				e.printStackTrace();
			} catch (OnepRemoteProcedureException e) {
				e.printStackTrace();
			} catch (OnepIllegalArgumentException e) {
				e.printStackTrace();
			}

			// Add addresses from TailNodeConnectorList to nodesHash
			for (List<InetAddress> connectorList : tailNodeConnectorList) {
				for (InetAddress address : connectorList) {
					String neighborAddress = address.getHostAddress();
					addElementAddress(neighborAddress);
				}
			}
		}
	}

	/**
	 * Given the address of a network element, register this as a CDP listener,
	 * and call registerSyslogListener.
	 * 
	 * @param elementAddress
	 *            - Address of the network element to listen to.
	 */
	private void listenToAddress(String elementAddress) {
		try {
			if (myNetworkElement != null
					&& InetAddress.getAllByName(elementAddress).equals(
							myNetworkElement.getAddress())) {
				getLogger().info(
						"Already listen to network element with address "
								+ elementAddress + ".");
			} else {
				getLogger().info(
						"Connecting to address " + elementAddress + ".");
				myNetworkElement = getMyNetworkApplication().getNetworkElement(
						InetAddress.getByName(elementAddress));

				myNetworkElement.connect(getUserName(), getPassword(),
						mySessionConf);
			}

			discoverAddresses(myNetworkElement);

			registerARTRouteListener(myNetworkElement);
			registerRIBRouteListener(myNetworkElement);
			registerInterfaceStateListener(myNetworkElement);

			getTopologyGraphviaCDP();
		} catch (Exception e) {
			getLogger().error(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Discover all of the addresses for the given network element Interfaces
	 * and sub-Interfaces, and record them. This will allow us to avoid
	 * processing the same network element when it is discovered later via CDP
	 * from one of its neighbors.
	 * 
	 * @param networkElement
	 *            - The network element where to discover the addresses.
	 * @throws OnepException
	 *             OnepException represents a general exception to be thrown by
	 *             ONE-P API when an unrecoverable error is encountered.
	 */
	private void discoverAddresses(NetworkElement networkElement)
			throws OnepException {
		List<NetworkInterface> interfaceList = networkElement
				.getInterfaceList(new InterfaceFilter());
		interfaceHash = new HashMap<String, List<de.lmu.ifi.nm.www.NetworkPrefix>>();

		for (NetworkInterface networkInterface : interfaceList) {
			List<InetAddress> addressList = networkInterface.getAddressList();
			for (InetAddress inetAddress : addressList) {
				nodesHash.put(inetAddress.getHostAddress(), networkElement
						.getAddress().toString());
			}

			HashMap<String, NetworkInterface> subInterfaceList = networkInterface
					.getSubInterfaceList();
			for (String subInterfaceName : subInterfaceList.keySet()) {
				NetworkInterface subNetworkInterface = subInterfaceList
						.get(subInterfaceName);
				List<InetAddress> subInterfaceAddressList = subNetworkInterface
						.getAddressList();
				for (InetAddress subInterfaceAddress : subInterfaceAddressList) {
					nodesHash.put(subInterfaceAddress.getHostAddress(),
							networkElement.getAddress().toString());
				}
			}
		}
	}

	/**
	 * Similarly to discoverAddresses, an address discovered in
	 * handleEvent(CDPEvent cdpEvent, Object clientData) is recorded to avoid
	 * further processing, and listenToAddress(String elementAddress) is called
	 * for the address.
	 * 
	 * @param elementAddress
	 *            - The IP-Address of the network element.
	 */
	private void addElementAddress(String elementAddress) {
		if (!nodesHash.containsKey(elementAddress)) {
			nodesHash.put(elementAddress, elementAddress);
			listenToAddress(elementAddress);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see satellite.INetworkAgent#setRoute(java.lang.String, java.lang.String,
	 * int, java.lang.String, java.lang.String)
	 */
	public boolean setRoute(String sourceAddress, String destNetwork,
			int destNetworkPrefix, String nextHop, String sourceInt) {
		try {
			if (myNetworkElement != null
					&& InetAddress.getAllByName(sourceAddress).equals(
							myNetworkElement.getAddress())) {
				getLogger().info(
						"Already listen to network element with address "
								+ sourceAddress + ".");
			} else {
				getLogger().debug(
						"Getting network element with address " + sourceAddress
								+ ".");
				myNetworkElement = getMyNetworkApplication().getNetworkElement(
						InetAddress.getByName(sourceAddress));
				if (!myNetworkElement.isConnected()) {
					myNetworkElement.connect(getUserName(), getPassword());
				}
			}

			// Before adding the route sleep for 5 seconds. Routes haven'n been
			// placed without the prior sleep.
			Thread.sleep(5000);

			addRoutes(getAppRouteTable(), destNetwork, destNetworkPrefix,
					nextHop, sourceInt);
		} catch (Exception e) {
			getLogger().error(e.getLocalizedMessage(), e);
			return false;
		}

		return true;
	}

	/**
	 * Add custom application routes to the network element.
	 * 
	 * @param appRouteTable
	 *            - Application route table to be updated.
	 * @throws OnepConnectionException
	 *             As determined by the presentation layer semantics.
	 * @throws OnepIllegalArgumentException
	 *             As determined by the presentation layer semantics.
	 * @throws OnepRemoteProcedureException
	 *             As determined by the presentation layer semantics.
	 * @throws UnknownHostException
	 *             As determined by the presentation layer semantics.
	 */
	private void addRoutes(AppRouteTable appRouteTable,
			String destinationNetwork, int destNetworkPrefix, String nextHop,
			String sourceInt) throws OnepConnectionException,
			OnepIllegalArgumentException, OnepRemoteProcedureException,
			UnknownHostException {
		// Specify scope
		L3UnicastScope aL3UnicastScope = new L3UnicastScope("", AFIType.IPV4,
				SAFIType.UNICAST, "base");

		// Create a new route and change its administrative distance
		// to make it more trusted. This operation will have the same effect
		// as the adding/replacing static route using the following IOS
		// config command:
		//
		// ip route 10.1.1.0 255.255.255.0 10.15.1.7
		NetworkPrefix destNetwork = new NetworkPrefix(
				InetAddress.getByName(destinationNetwork), destNetworkPrefix);

		L3UnicastNextHop aL3UnicastNextHop = new L3UnicastNextHop(
				getNetworkElement().getInterfaceByName(sourceInt),
				InetAddress.getByName(nextHop));

		Set<L3UnicastNextHop> aL3UnicastNextHopList = new HashSet<L3UnicastNextHop>();
		aL3UnicastNextHopList.add(aL3UnicastNextHop);
		L3UnicastRoute aRoute = new L3UnicastRoute(destNetwork,
				aL3UnicastNextHopList);
		aRoute.setAdminDistance(1);

		// Now update the application route table with this route
		RouteOperation routeOperation = new L3UnicastRouteOperation(
				RouteOperationType.REPLACE, aRoute);
		List<RouteOperation> routeOperationList = new ArrayList<RouteOperation>();
		routeOperationList.add(routeOperation);
		appRouteTable.updateRoutes(aL3UnicastScope, routeOperationList);
	}

	/**
	 * Get the Application Route Table generated by this application.
	 * 
	 * @return AppRouteTable for this application.
	 * @throws OnepConnectionException
	 *             As determined by the presentation layer semantics.
	 */
	private AppRouteTable getAppRouteTable() throws OnepConnectionException {
		Routing routing = Routing.getInstance(getNetworkElement());
		AppRouteTable appRouteTable = routing.getAppRouteTable();

		return appRouteTable;
	}

	/**
	 * Get the RIB table for the connected network element.
	 * 
	 * @return RIB table for the connected network element.
	 * @throws OnepConnectionException
	 *             As determined by the presentation layer semantics.
	 */
	private RIB getRIB() throws OnepConnectionException {
		Routing routing = Routing.getInstance(getNetworkElement());
		RIB rib = routing.getRib();

		return rib;
	}

	/**
	 * Disconnect from the network element.
	 * 
	 * @return True of the disconnect succeeded without an exception, and false
	 *         if there was an exception.
	 */
	public boolean disconnect() {
		try {
			myNetworkElement.disconnect();
		} catch (Exception e) {
			getLogger().error("Failed to disconnect from Network Element");
			return false;
		}
		return true;
	}

	/**
	 * @return Serial number of the network element.
	 */
	public String getIdentifier() {
		return myNetworkElement.getProperty().getSerialNo();
	}

	/**
	 * @return User name of the network element.
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @return Password of the network element.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Get all available interfaces of the network element.
	 * 
	 * @return A HashMap where the key is the interface name and the value a
	 *         list of NetworkPrefix objects. A NetworkPrefix object consists of
	 *         an address and a prefix, e.g. 10.1.0.0/24.
	 */
	public HashMap<String, List<de.lmu.ifi.nm.www.NetworkPrefix>> getInterfaceHash() {
		List<de.lmu.ifi.nm.www.NetworkPrefix> interfacePrefixList;
		interfaceHash = new HashMap<String, List<de.lmu.ifi.nm.www.NetworkPrefix>>();

		try {
			List<NetworkInterface> interfaceList = getNetworkElement()
					.getInterfaceList(
							new InterfaceFilter(null,
									NetworkInterface.Type.ONEP_IF_TYPE_ANY));

			for (NetworkInterface networkInterface : interfaceList) {
				interfacePrefixList = new ArrayList<de.lmu.ifi.nm.www.NetworkPrefix>();

				for (NetworkPrefix networkPrefix : networkInterface
						.getPrefixList()) {
					de.lmu.ifi.nm.www.NetworkPrefix prefix = new de.lmu.ifi.nm.www.NetworkPrefix(
							networkPrefix.getAddress(),
							networkPrefix.getPrefixLength());
					interfacePrefixList.add(prefix);
				}

				interfaceHash.put(networkInterface.getName(),
						interfacePrefixList);
			}
		} catch (OnepException e) {
			getLogger().error("\n\n--- Exception Text ---\n" + e.getMessage());
			getLogger().error(
					"\n\n--- Localized Exception Text ---\n"
							+ e.getLocalizedMessage());
		}

		return interfaceHash;
	}

	/**
	 * Get the routing information base of the network element.
	 * 
	 * @return The routing table as a String object.
	 */
	public String getRoutingTable() {
		String RibString = "";

		try {
			// Create a Routing object for the network element
			Routing routing = Routing.getInstance(getNetworkElement());

			// Specify scope, filter and range
			L3UnicastScope aL3UnicastScope = new L3UnicastScope("",
					AFIType.IPV4, SAFIType.UNICAST, "base");

			NetworkPrefix networkPrefix = new NetworkPrefix(
					InetAddress.getByName("192.168.0.0"), 32);
			L3UnicastRIBFilter filter = new L3UnicastRIBFilter();

			// Get the instance of RIB information
			RIB rib = routing.getRib();

			L3UnicastRouteRange range = new L3UnicastRouteRange(networkPrefix,
					RouteRange.RangeType.EQUAL_OR_LARGER, 10);

			// Get all routes from RIB
			List<Route> routeList = rib.getRouteList(aL3UnicastScope, filter,
					range);

			// Print the route in the list if it is a layer 3 unicast route
			for (Route route : routeList) {
				if (route instanceof L3UnicastRoute) {
					L3UnicastRoute l3uRoute = (L3UnicastRoute) route;
					RibString = RibString + l3uRoute + "\n";
				}
			}
		} catch (OnepConnectionException e) {
			getLogger().error("\n\n--- Exception Text ---\n" + e.getMessage());
			getLogger().error(
					"\n\n--- Localized Exception Text ---\n"
							+ e.getLocalizedMessage());
		} catch (OnepIllegalArgumentException e) {
			getLogger().error("\n\n--- Exception Text ---\n" + e.getMessage());
			getLogger().error(
					"\n\n--- Localized Exception Text ---\n"
							+ e.getLocalizedMessage());
		} catch (OnepRemoteProcedureException e) {
		} catch (UnknownHostException e) {
			getLogger().error("\n\n--- Exception Text ---\n" + e.getMessage());
			getLogger().error(
					"\n\n--- Localized Exception Text ---\n"
							+ e.getLocalizedMessage());
		}

		return RibString;
	}

	/**
	 * @return The uptime of the network element.
	 */
	public long getUpTime() {
		try {
			return getNetworkElement().getProperty().getSysUpTime();
		} catch (OnepRemoteProcedureException e) {
			getLogger().error("\n\n--- Exception Text ---\n" + e.getMessage());
			getLogger().error(
					"\n\n--- Localized Exception Text ---\n"
							+ e.getLocalizedMessage());
			return 0;
		} catch (OnepConnectionException e) {
			getLogger().error("\n\n--- Exception Text ---\n" + e.getMessage());
			getLogger().error(
					"\n\n--- Localized Exception Text ---\n"
							+ e.getLocalizedMessage());
			return 0;
		}
	}

	/**
	 * @return The host name of the network element.
	 */
	public String getHostname() {
		try {
			return getNetworkElement().getProperty().getSysName();
		} catch (OnepRemoteProcedureException e) {
			getLogger().error("\n\n--- Exception Text ---\n" + e.getMessage());
			getLogger().error(
					"\n\n--- Localized Exception Text ---\n"
							+ e.getLocalizedMessage());
			return "";
		} catch (OnepConnectionException e) {
			getLogger().error("\n\n--- Exception Text ---\n" + e.getMessage());
			getLogger().error(
					"\n\n--- Localized Exception Text ---\n"
							+ e.getLocalizedMessage());
			return "";
		}
	}

	/**
	 * @return The management IP-Address of the network element.
	 */
	public String getManagementIPaddress() {
		return getNetworkElement().getAddress().toString().substring(1);
	}

	/**
	 * @return The network element that the NetworkApplication is connected to.
	 */
	public NetworkElement getNetworkElement() {
		return myNetworkElement;
	}

	/**
	 * Initialize a new vertex object that will be further added to the
	 * Satellite.myGraph JGraphT object.
	 * 
	 * @return A Vertex object.
	 */
	private Vertex initVertex() {
		if (getHostname().equals("Zuerich")) {
			this.latitude = 47.36865;
			this.longitude = 8.53918;
		}

		if (getHostname().equals("Berlin")) {
			this.latitude = 52.471;
			this.longitude = 13.344612;
		}

		if (getHostname().equals("Muenchen")) {
			this.latitude = 48.175168;
			this.longitude = 11.564484;
		}

		if (getHostname().equals("Koeln")) {
			this.latitude = 50.936977;
			this.longitude = 6.961555;
		}

		if (getHostname().equals("Hamburg")) {
			this.latitude = 53.564715;
			this.longitude = 9.959793;
		}

		Vertex v = new Vertex(getIdentifier(), getUserName(), getPassword(),
				getInterfaceHash(), getRoutingTable(), getUpTime(),
				getHostname(), getManagementIPaddress(), getLatitude(),
				getLongitude());

		getLogger().info(
				"\n" + "Added new vertex to JGraphT graph: " + v.toString());

		return v;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see satellite.INetworkAgent#toJGraphT()
	 */
	public void toJGraphT() {
		Vertex headNode = null;
		Vertex tailNode = null;
		Link myLink = null;
		String subnetAddress = null;
		String headInt = null;
		String tailInt = null;
		String headIP = null;
		String tailIP = null;

		for (Edge edge : myCdpGraph.getEdgeList(EdgeType.DIRECTED)) {
			for (Vertex vertex : Satellite.myGraph.vertexSet()) {
				if (vertex.getHostname().equals(edge.getHeadNode().getName())) {
					headNode = vertex;
					headInt = edge.getHeadNodeConnector().getName();
					headIP = edge.getHeadNodeConnector().getAddressList()
							.get(0).toString().substring(1);
				}

				if (vertex.getHostname().equals(edge.getTailNode().getName())) {
					tailNode = vertex;
					tailInt = edge.getTailNodeConnector().getName();
					tailIP = edge.getTailNodeConnector().getAddressList()
							.get(0).toString().substring(1);
				}
			}

			// Calculate subnet address from source and destination IP address
			subnetAddress = Iandr.and(headIP, tailIP);

			myLink = new Link(headInt, headIP, tailInt, tailIP, subnetAddress,
					headNode, tailNode);

			if (!Satellite.myGraph.containsEdge(headNode, tailNode)) {
				Satellite.myGraph.addEdge(headNode, tailNode, myLink);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see satellite.INetworkAgent#getTopology()
	 */
	public void getTopology() {
		listenToAddress(seedAddress);
		toJGraphT();
	}

	/**
	 * @return The logger.
	 */
	public Logger getLogger() {
		return myLogger;
	}

	/**
	 * Obtain all interfaces on a network element.
	 * 
	 * @return List of NetworkInterface instances.
	 */
	public List<NetworkInterface> getAllInterfaces() {
		List<NetworkInterface> interfaceList = null;
		try {
			NetworkElement networkElement = getNetworkElement();
			interfaceList = networkElement
					.getInterfaceList(new InterfaceFilter());
		} catch (Exception e) {
			getLogger().error(e.getLocalizedMessage(), e);
		}
		return interfaceList;
	}

	/**
	 * @return The latitude GPS coordinate of the network element.
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * @return The longitude GPS coordinate of the network element.
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * @return The NetworkApplication that is connected to the network element.
	 */
	public NetworkApplication getMyNetworkApplication() {
		return myNetworkApplication;
	}
}