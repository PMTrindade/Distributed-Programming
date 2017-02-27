package contactserver;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import fileserver.FileServer;


public class ContactServerImpl extends UnicastRemoteObject implements ContactServer{
	
	private static final long serialVersionUID = 1L;
	private Map<String, Set<InetAddress>> fileServers;
	
	public ContactServerImpl() throws RemoteException{
		this.fileServers = new HashMap<String, Set<InetAddress>>();
	}

	public static void main(String[] args) throws RemoteException, MalformedURLException, UnknownHostException {
		ContactServer cs = new ContactServerImpl();
		System.getProperties().put("java.security.policy","src/policy.all");

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			LocateRegistry.createRegistry(1099);
		} catch (RemoteException e) {
		}
		Naming.rebind("/ContactServer", cs);
		
		System.out.println("ContactServer bound in registry");

	}

	public void addFileServer(String name, InetAddress address) throws RemoteException, MalformedURLException, NotBoundException {
		if (!fileServers.containsKey(name)) {
			Set<InetAddress> newSet = new HashSet<InetAddress>();
			newSet.add(address);
			fileServers.put(name, newSet);
		
			try {
				FileServer fs = null;
				fs = (FileServer) Naming.lookup("//" + address.getHostAddress() + "/FileServer");
				if(fs != null)
					fs.setPrimaryServer(true);
			} catch (ConnectException e) {
				System.out.println("Error connecting with " + address.toString());
			}
		}
		else {			
			Set<InetAddress> newSet = fileServers.get(name);
			newSet.add(address);
			fileServers.put(name, newSet);
		}
		
		System.out.println("FileServer '" + name + "' with address '"  + address.getHostAddress() + "' added.");
	}

	public Set<String> listFileServerNames() throws RemoteException, MalformedURLException, NotBoundException {
		return new HashSet<>(fileServers.keySet()); 
	}

	public Set<InetAddress> listServerAddresses(String name) throws RemoteException {
		return fileServers.get(name);
	}

	public void updateAllServers() throws RemoteException, MalformedURLException, NotBoundException {
		FileServer fs = null;
		Iterator<Set<InetAddress>> setIt = fileServers.values().iterator();
		while (setIt.hasNext()) {
			Iterator<InetAddress> inetIt = setIt.next().iterator();
			while (inetIt.hasNext()) {
				InetAddress addr = inetIt.next();
				try {
					fs = (FileServer) Naming.lookup("//" + addr.getHostAddress() + "/FileServer");
					if(fs != null)
						System.out.println("Successfull connection with '" + addr.toString() + "'");
				} catch (ConnectException e) {
					inetIt.remove();
					System.out.println("Error connecting with " + addr.toString());
				}
			}
		}
	}

}
