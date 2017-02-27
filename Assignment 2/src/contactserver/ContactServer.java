package contactserver;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface ContactServer extends Remote {
	
	public void addFileServer(String name, InetAddress address) throws RemoteException, MalformedURLException, NotBoundException;
	
	public Set<String> listFileServerNames() throws RemoteException, MalformedURLException, NotBoundException;
	
	public Set<InetAddress> listServerAddresses(String name) throws RemoteException;
	
	public void updateAllServers() throws RemoteException, MalformedURLException, NotBoundException;
	
}
