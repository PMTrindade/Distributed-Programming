package trabalho1v2;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class ContactServerImpl extends UnicastRemoteObject implements ContactServer {

	private static final long serialVersionUID = 0L;
	private HashMap<String, HashSet<InetAddress>> fileServers;
	
	public ContactServerImpl() throws RemoteException {
		fileServers = new HashMap<String, HashSet<InetAddress>>();
	}
	
	public void addFileServer(String name, InetAddress address) {
		HashSet<InetAddress> addresses;
		
		if (fileServers.containsKey(name)) {
			addresses = fileServers.get(name);
			addresses.add(address);
			fileServers.put(name, addresses);
		} else {
			addresses = new HashSet<InetAddress>();
			addresses.add(address);
			fileServers.put(name, addresses);
		}
		
		System.out.println("Novo FileServer adicionado.");
	}
	
	public String[] listFileServerNames() throws RemoteException, MalformedURLException, NotBoundException {
		updateAllServers();
		String[] names = fileServers.keySet().toArray(new String[fileServers.size()]);
		return names;
	}
	
	public String[] listServerAddresses(String name) throws RemoteException, MalformedURLException, NotBoundException {
		updateAllServers();
		HashSet<InetAddress> addresses = fileServers.get(name);
		String[] result = new String[addresses.size()];
		
		int i = 0;
		for(InetAddress a: addresses) {
			result[i] = a.getHostAddress();
			i++;
		}
		
		return result;
	}
	
	public void updateAllServers() throws RemoteException, MalformedURLException, NotBoundException {
		FileServer fs = null;
		Iterator<String> setIterator = fileServers.keySet().iterator();
		while(setIterator.hasNext()) {
			String key = setIterator.next();
			Iterator<InetAddress> addressIterator = fileServers.get(key).iterator();
			while(addressIterator.hasNext()) {
				InetAddress address = addressIterator.next();
				try {
					fs = (FileServer) Naming.lookup("//" + address.getHostAddress() + "/FileServer");
					if(fs != null)
						System.out.println("Ligação com " + address.toString() + " bem sucedida");
				} catch (ConnectException e) {
					addressIterator.remove();
					System.out.println("Erro de ligação com o servidor " + address.toString());
				}
			}
			if(fileServers.get(key).isEmpty())
				setIterator.remove();
		}
	}
	
	public static void main(String[] args) throws RemoteException, MalformedURLException, UnknownHostException, NotBoundException {
		ContactServer cs = new ContactServerImpl();
		System.getProperties().put("java.security.policy", "src/trabalho1v2/policy.all");
		
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		
		try { // start rmiregistry
			LocateRegistry.createRegistry(1099);
		} catch (RemoteException e) {
			// if not start it
			// do nothing - already started with rmiregistry
		}
		
		Naming.rebind("/ContactServer", cs);
		
		System.out.println("ContactServer bound in registry");
	}
}
