package trabalho1v2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;

public class FileServerRMI implements FileServer {

	private String fileServerName;
	private String contactServerURL;
	private boolean isOnline;
	
	public FileServerRMI(String fileServerName, String contactServerURL) throws RemoteException {
		this.fileServerName = fileServerName;
		this.contactServerURL = contactServerURL;
		this.setOnline(true);
	}
	
	public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException, UnknownHostException {
		FileServer fs = new FileServerRMI(args[0], args[1]);
		
		System.getProperties().put("java.security.policy", "src/trabalho1v2/policy.all");
		
		if(System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		
		try { // start rmiregistry
			LocateRegistry.createRegistry(1099);
		} catch(RemoteException e) {
			// if not start it
			// do nothing - already started with rmiregistry
		}
		
        FileServer stub = (FileServer) UnicastRemoteObject.exportObject(fs, 0);
		Naming.rebind("/FileServer", stub);
		System.out.println("FileServer bound in registry");
		
		try {
			ContactServer cs = (ContactServer) Naming.lookup("//" + fs.getContactServerURL() + "/ContactServer");
			cs.addFileServer(fs.getFileServerName(), InetAddress.getLocalHost());
		} catch (NotBoundException|ConnectException e) {
			System.out.println("Ligação com o Contact Server falhou.");
		}
	}
	
	public String getFileServerName() {
		return this.fileServerName;
	}
	
	public String getContactServerURL() {
		return contactServerURL;
	}
	
	public String[] ls(String dir) throws InfoNotFoundException {
		System.out.println("listing...");
		File f = new File(dir);
		if(f.exists())
			return f.list();
		return null;
	}
	
	public boolean mkdir(String dir) {
		File f = new File(dir);
		return f.mkdir();
	}
	
	public boolean rmdir(String dir) {
		File f = new File(dir);
		System.out.println(f.isDirectory());
		if(f.isDirectory())
			return f.delete();
		return false;
	}
	
	public boolean cp(String fromPath, String toPath) throws IOException {
		Path res = Files.copy(new File(fromPath).toPath(), new File(toPath).toPath(), StandardCopyOption.REPLACE_EXISTING);
		return res.equals(new File(toPath).toPath());
	}
	
	public boolean rm(String path) {
		File f = new File(path);
		if(f.isFile())
			return f.delete();
		return false;
	}
	
	public FileInfo getAttr(String path) {
		File f = new File(path);
		FileInfo info = new FileInfo(f.getName(), f.length(), new Date(f.lastModified()), f.isFile());
		return info;
	}
	
	public boolean isOnline() {
		return isOnline;
	}
	
	public void setOnline(boolean status) {
		this.isOnline = status;
	}
	
	public boolean download(byte[] bytes, String path) throws RemoteException {
		try {
			File f = new File(path);
			if(!f.exists()) {
				RandomAccessFile raf = new RandomAccessFile(f, "rw");
				raf.write(bytes);
				return true;
			} else
				throw new InfoNotFoundException("Erro no download : " + path);
		} catch (Exception e) {
			System.err.println("Erro: " + e.getMessage());
			return false;
		}
	}
	
	public byte[] upload(String path) throws RemoteException {
		try {
			File f = new File(path);
			if(f.exists()) {
				byte[] b = new byte[(int) f.length()];
				RandomAccessFile raf;
				raf = new RandomAccessFile(f, "r");
				raf.readFully(b);
				return b;
			} else
				throw new InfoNotFoundException("Erro no upload : " + path);
		} catch (Exception e) {
			System.err.println("Erro: " + e.getMessage());
			return null;
		}
	}
	
	public boolean sendToServer(String fromPath, String toServer, String toPath, boolean toIsURL) throws NotBoundException, IOException { //URLs
		System.out.println(fromPath + " " + toServer + " " + toPath);
		FileServer fs;
		
		File f = new File(fromPath);
		if (f.exists()) {
			if(!toIsURL)
				fs = (FileServer) Naming.lookup("//" + toServer + "/FileServer");
			else
				fs = (FileServer) Naming.lookup("/" + toServer + "/FileServer");
			byte[] b = new byte[(int) f.length()];
			RandomAccessFile raf;
			raf = new RandomAccessFile(f, "r");
			raf.readFully(b);
			return fs.download(b, toPath);
		}
		else return false;
	}
}
