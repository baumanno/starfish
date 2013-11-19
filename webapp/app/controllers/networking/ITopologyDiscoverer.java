package controllers.networking;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Java RMI Interface for the TopologyDiscoverer class.
 * 
 * @author Dawin Schmidt, dawin.schmidt@lmu.de
 * @since October 2, 2013
 */
public interface ITopologyDiscoverer extends Remote {
    /**
     * @param clientIP
     *            - Source IPv4-Address of the connected client.
     * @param clientPort
     *            - Source-Port of the connected client.
     * @param userName
     *            - The client's name.
     * @throws RemoteException
     *             A RemoteException is the common superclass for a number of
     *             communication-related exceptions that may occur during the
     *             execution of a remote method call.
     */
    public void connect(String clientIP, int clientPort, String userName)
			throws RemoteException;

    /** 
     * @param clientIP
     *            - Source IPv4-Address of the connected client.
     * @param clientPort
     *            - Source-Port of the connected client.
     * @param userName
     *            - The name of the client.
     * @throws RemoteException
     *             A RemoteException is the common superclass for a number of
     *             communication-related exceptions that may occur during the
     *             execution of a remote method call.
     */
	public void disconnect(String clientIP, int clientPort, String userName)
			throws RemoteException;
	
    /**
     * @param userName - The name of the client who has sent the message.
     * @param message
     *            - The actual error message.
     * @throws RemoteException
     *             A RemoteException is the common superclass for a number of
     *             communication-related exceptions that may occur during the
     *             execution of a remote method call.
     */
	public void receiveEvent(String userName, String message)
			throws RemoteException;
}