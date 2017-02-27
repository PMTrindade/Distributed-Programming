package trabalho1v2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Date;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;

@WebService
public class FileServerWS implements FileServer {


	private String fileServerName;
	private String contactServerURL;
	private boolean isOnline;
	
	public FileServerWS(String fileServerName, String contactServerURL) throws RemoteException {
		this.fileServerName = fileServerName;
		this.contactServerURL = contactServerURL;
		this.setOnline(true);
	}
	
	@WebMethod
	public String getFileServerName() {
		return this.fileServerName;
	}
	
	@WebMethod
	public String getContactServerURL() {
		return contactServerURL;
	}

	@WebMethod
	public String[] ls(String dir) throws InfoNotFoundException {
		System.out.println("listing...");
		File f = new File(dir);
		if( f.exists())
			return f.list();
		return null;
	}

	@WebMethod
	public boolean mkdir(String dir) {
		File f = new File(dir);
		return f.mkdir();
	}

	@WebMethod
	public boolean rmdir(String dir) {
		File f = new File(dir);
		System.out.println(f.isDirectory());
		if (f.isDirectory())
			return f.delete();		
		return false;
	}

	@WebMethod
	public boolean cp(String fromPath, String toPath) throws IOException {
		Path res = Files.copy(new File(fromPath).toPath(), new File(toPath).toPath(), StandardCopyOption.REPLACE_EXISTING);
		return res.equals(new File(toPath).toPath());
	}

	@WebMethod
	public boolean rm(String path) {
		File f = new File(path);
		if (f.isFile())
			return f.delete();
		return false;
	}

	@WebMethod
	public FileInfo getAttr(String path) {
		File f = new File(path);
		FileInfo info = new FileInfo(f.getName(), f.length(), new Date(f.lastModified()), f.isFile());
		return info;
	}

	@WebMethod
	public boolean isOnline() {
		return isOnline;
	}

	@WebMethod
	public void setOnline(boolean status) {
		this.isOnline = status;
	}

	@WebMethod
	public boolean download(byte[] bytes, String path) throws RemoteException {
		try {
			File f = new File(path);
			if (!f.exists()) {
				RandomAccessFile raf = new RandomAccessFile(f, "rw");
				raf.write(bytes);
				return true;
			} else
				throw new InfoNotFoundException("File not found :" + path);
		} catch (Exception e) {
			System.err.println("Erro: " + e.getMessage());
			return false;
		}
	}

	@WebMethod
	public byte[] upload(String path) throws RemoteException {
		try {
			File f = new File(path);
			if (f.exists()) {
				byte[] b = new byte[(int) f.length()];
				RandomAccessFile raf;
				raf = new RandomAccessFile(f, "r");
				raf.readFully(b);
				return b;
			} else
				throw new InfoNotFoundException("File not found :" + path);
		} catch (Exception e) {
			System.err.println("Erro: " + e.getMessage());
			return null;
		}
	}

	@WebMethod
	public boolean sendToServer(String fromPath, String toServer, String toPath, boolean toIsURL)
			throws RemoteException, MalformedURLException, NotBoundException,
			FileNotFoundException, IOException {
		// TODO Auto-generated method stub
		return false;
	}
	
	public static void main(String[] args) throws RemoteException {
		
		FileServer fs = new FileServerWS(args[0], args[1]);
		
		try {
			Endpoint.publish("http://localhost:8080/FileServer", fs);
			System.out.println( "FileServerWS started");
			ContactServer cs = (ContactServer) Naming.lookup("//" + fs.getContactServerURL() + "/ContactServer");
			cs.addFileServer(fs.getFileServerName(), InetAddress.getLocalHost());
		} catch( Throwable th) {
			th.printStackTrace();
		}

	}
}
