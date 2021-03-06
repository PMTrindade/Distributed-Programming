package trabalho1v2;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.net.MalformedURLException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FileServer extends Remote {

	public String getFileServerName() throws RemoteException;
	
	public String getContactServerURL() throws RemoteException;
	
	public String[] ls(String dir) throws InfoNotFoundException, RemoteException;
	
	public boolean mkdir(String dir) throws RemoteException;
	
	public boolean rmdir(String dir) throws RemoteException;
	
	public boolean cp(String fromPath,String toPath) throws IOException, RemoteException;
	
	public boolean rm(String path) throws RemoteException;
	
	public FileInfo getAttr(String path) throws RemoteException;
	
	public boolean isOnline() throws RemoteException;
	
	public void setOnline(boolean status) throws RemoteException;
	
	public boolean download(byte[] bytes, String toPath) throws RemoteException;
	
	public byte[] upload(String path) throws RemoteException;
	
	public boolean sendToServer(String fromPath, String toServer, String toPath, boolean toIsURL) throws RemoteException, NotBoundException, IOException;

}
