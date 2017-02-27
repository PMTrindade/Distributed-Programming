package trabalho1v2;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ContactServer extends Remote {

	public void addFileServer(String name, InetAddress address) throws RemoteException;
	
	public String[] listFileServerNames() throws RemoteException, MalformedURLException, NotBoundException;
	
	public String[] listServerAddresses(String name) throws RemoteException, MalformedURLException, NotBoundException;
	
	public void updateAllServers() throws RemoteException, MalformedURLException, NotBoundException;

}
