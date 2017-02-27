package fileserver;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import exceptions.InfoNotFoundException;

public interface FileServer extends Remote {

	public String getFileServerName() throws RemoteException;
	
	public String getContactServerURL() throws RemoteException;
	
	public boolean isPrimaryServer() throws RemoteException;
	
	public void setPrimaryServer(boolean status) throws RemoteException;
	
	public String[] ls(String dir) throws InfoNotFoundException, RemoteException;
	
	public boolean mkdir(String dir) throws RemoteException;
	
	public boolean rmdir(String dir) throws RemoteException;
	
	public boolean cp(String fromPath,String toPath) throws IOException, RemoteException;
	
	public boolean rm(String path) throws RemoteException;
	
	public FileInfo getAttr(String path) throws RemoteException;
	
	public boolean upload(byte[] bytes, String toPath) throws RemoteException;
	
	public byte[] download(String path) throws RemoteException;
	
	public boolean sendToServer(String fromPath, String toServer, String toPath) throws RemoteException, NotBoundException, IOException;

}
