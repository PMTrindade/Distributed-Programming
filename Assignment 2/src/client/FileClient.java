package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Set;

import contactserver.ContactServer;
import exceptions.InfoNotFoundException;
import fileserver.FileInfo;
import fileserver.FileServer;



public class FileClient {
	
	String contactServerURL;
	ContactServer cs;
	
	private FileClient(String contactServerURL) {
		this.contactServerURL = contactServerURL;
		this.cs = contactServerLookup(contactServerURL);
	}
	
	private ContactServer contactServerLookup(String url) {
		try {
			return (ContactServer) Naming.lookup("//" + url + "/ContactServer");
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			System.out.println("Error. Could not connect to the specified ContactServer.");
			System.exit(1);
		}
		return null;
	}
	
	private void servers(String name) throws RemoteException, MalformedURLException, NotBoundException {
		if (name == null) {
			Set<String> fileServerNames = cs.listFileServerNames();
			for (String s : fileServerNames)
				System.out.println(s);
		}
		else {
			Set<InetAddress> fileServerAddresses = cs.listServerAddresses(name);
			for (InetAddress s: fileServerAddresses)
				System.out.println(s.getHostAddress());
		}
	}
	
	private void ls(String server, String dir) throws RemoteException, MalformedURLException, NotBoundException, UnknownHostException {
		cs.updateAllServers();
		FileServer fs = null;
		try {
			try {
				InetAddress.getByName(server);
				try {
					fs = (FileServer) Naming.lookup("//" + server + "/FileServer");
				} catch (NotBoundException e) {
					System.out.println("Error: No FileServer running on localhost");
				}
			} catch (UnknownHostException e) {
				if(cs.listFileServerNames().contains(server))
					ls(cs.listServerAddresses(server).iterator().next().getHostAddress(), dir);
				else System.out.println("Server '" + server + "' not found.");
				return;
			}
			String[] fileList = fs.ls(dir);
			if (fileList == null) {
				System.out.println("Error listing files on directory '" + dir + "'.");
				return;
			}
			for (String s: fileList)
				System.out.println("  " + s);
		} catch (InfoNotFoundException e) {
			System.out.println("Invalid Server");
		}
	}
	
	private void mkdir(String server, String dir) throws RemoteException, MalformedURLException, NotBoundException, UnknownHostException {
		cs.updateAllServers();
		FileServer fs = null;
		try {
			InetAddress.getByName(server);
			try {
				fs = (FileServer) Naming.lookup("//" + server + "/FileServer");
			} catch (NotBoundException e) {
				System.out.println("Error: No FileServer running on localhost");
			}
		} catch (UnknownHostException e) {
			if(cs.listFileServerNames().contains(server))
				mkdir(cs.listServerAddresses(server).iterator().next().getHostAddress(), dir);
			else System.out.println("Server '" + server + "' not found.");
			return;
		}
		if (fs.mkdir(dir))
			System.out.println("Directory '" + dir + "' sucessfully created.");
		else System.out.println("Error creating directory '" + dir + "'");
	}
	
	private void rmdir(String server, String dir) throws RemoteException, MalformedURLException, NotBoundException, UnknownHostException {
		cs.updateAllServers();
		FileServer fs = null;
		try {
			InetAddress.getByName(server);
			try {
				fs = (FileServer) Naming.lookup("//" + server + "/FileServer");
			} catch (NotBoundException e) {
				System.out.println("Error: No FileServer running on localhost");
			}
		} catch (UnknownHostException e) {
			if(cs.listFileServerNames().contains(server))
				rmdir(cs.listServerAddresses(server).iterator().next().getHostAddress(), dir);
			else System.out.println("Server '" + server + "' not found.");
			return;
		}
		if (fs.rmdir(dir))
			System.out.println("Directory '" + dir + "' sucessfully removed.");
		else System.out.println("Error removing directory '" + dir + "'");
		
	}
	
	private void rm(String server, String dir) throws RemoteException, MalformedURLException, NotBoundException, UnknownHostException {
		cs.updateAllServers();
		FileServer fs = null;
		try {
			InetAddress.getByName(server);
			try {
				fs = (FileServer) Naming.lookup("//" + server + "/FileServer");
			} catch (NotBoundException e) {
				System.out.println("Error: No FileServer running on localhost");
			}
		} catch (UnknownHostException e) {
			if(cs.listFileServerNames().contains(server))
				rm(cs.listServerAddresses(server).iterator().next().getHostAddress(), dir);
			else System.out.println("Server '" + server + "' not found.");
			return;
		}
		if (fs.rm(dir))
			System.out.println("File " + dir + " sucessfully removed.");
		else System.out.println("Error removing '" + dir + "'");
		
	}
	
	private void getattr(String server, String dir) throws RemoteException, MalformedURLException, NotBoundException, UnknownHostException {
		cs.updateAllServers();
		FileServer fs = null;
		try {
			InetAddress.getByName(server);
			try {
				fs = (FileServer) Naming.lookup("//" + server + "/FileServer");
			} catch (NotBoundException e) {
				System.out.println("Error: No FileServer running on localhost");
			}
		} catch (UnknownHostException e) {
			if(cs.listFileServerNames().contains(server))
				getattr(cs.listServerAddresses(server).iterator().next().getHostAddress(), dir);
			else System.out.println("Server '" + server + "' not found.");
			return;
		}
		FileInfo info = fs.getAttr(dir);
		if (info != null)
			System.out.println(info);
		else System.out.println("Error getting attributes of '" + dir + "'");
	}
	
	private void cp(String server, String dir, String server2, String dir2) throws NotBoundException, IOException {
		cs.updateAllServers();
		FileServer fs = null;
		try {
			InetAddress.getByName(server);
			try {
				fs = (FileServer) Naming.lookup("//" + server + "/FileServer");
			} catch (NotBoundException e) {
				System.out.println("Error: No FileServer running on localhost");
			}
		} catch (UnknownHostException e) {
			if(cs.listFileServerNames().contains(server))
				cp(cs.listServerAddresses(server).iterator().next().getHostAddress(), dir, server2, dir2);
			else System.out.println("Server '" + server + "' not found.");
			return;
		}
		try {
			InetAddress.getByName(server2);
		} catch (UnknownHostException e) {
			if(cs.listFileServerNames().contains(server2))
				cp(server, dir, cs.listServerAddresses(server2).iterator().next().getHostAddress(), dir);
			else System.out.println("Server '" + server2 + "' not found.");
			return;
		}
		if (server.equals(server2)) {
			if (fs.cp(dir, dir2))
				System.out.println("File '" + dir + "' sucessfully copied to '" + dir2 + "'.");
			else System.out.println("Error copying '" + dir + "' to '" + dir2 + "'");
		}
		else fs.sendToServer(dir, server2, dir2);
	}
	
	private void sync(String localPath, String server, String toPath) throws NotBoundException, IOException {		
		cs.updateAllServers();
		FileServer fs = null;
		try {
			InetAddress.getByName(server);
			try {
				fs = (FileServer) Naming.lookup("//" + server + "/FileServer");
			} catch (NotBoundException e) {
				System.out.println("Error: No FileServer running.");
			}
		} catch (UnknownHostException e) {
			if(cs.listFileServerNames().contains(server))
				sync(cs.listServerAddresses(server).iterator().next().getHostAddress(), server, toPath);
			else System.out.println("Server '" + server + "' not found.");
			return;
		}
		
		if (!fs.mkdir(toPath))
			System.out.println("Found directory '" + toPath + "' on the FileServer. Syncing...");
		syncDir(fs ,localPath, toPath);	
		
	}
	
	private void syncDir(FileServer fs, String localPath, String toPath) throws RemoteException, IOException {
		
	    File[] list = new File(localPath).listFiles();
	    
	    if (list != null && list.length > 0) {
	        for (File f : list) {
	        	if (fs.getAttr(toPath + "/" + f.getName()).modified.equals(new Date(f.lastModified())))
	        		continue;
	        	if (!fs.cp(f.getPath(), toPath + "/" + f.getName()) && fs.getAttr(toPath + "/" + f.getName()).isFile)
	        		System.out.println("Error copying '" + f.getName() + "'");
	        	if (f.isDirectory())
	        		syncDir(fs, localPath + "/" + f.getName(), toPath + "/" + f.getName());
	        }
	    }
	}

	private void commandInterpreter() throws IOException, NotBoundException {
		BufferedReader reader = new BufferedReader( new InputStreamReader( System.in));
		String line;
		String[] cmd;
		String server = null;
		String dir = null;
		
		System.out.println("Welcome to the FileClient! Insert a command.");
		
		while (true) {
			System.out.print("> ");
			line = reader.readLine();
			
			if (line == null)
				break;
			cmd = line.split(" ");
			
			if (cmd[0].toLowerCase().matches("ls|mkdir|rmdir|rm|getattr|cp|sync")) {
				if (cmd.length < 2) {
					System.out.println("Error: Misssing or no arguments specified.");
					continue;
				}
				if (!cmd[1].matches(".+@.+")) {
					server = "localhost";
					dir = cmd[1];
				}
				else {
					server = cmd[1].split("@")[0];
					dir = cmd[1].split("@")[1];
				}
			}
			
			switch (cmd[0].toLowerCase()) {
			case "servers":
				if(cmd.length == 1)
					servers(null);
				else {
					String name = cmd[1];
					servers(name);
				}
				break;
			
			case "ls":
				ls(server,dir);
				break;
				
			case "mkdir":
				mkdir(server, dir);
				break;
				
			case "rmdir":
				rmdir(server, dir);
				break;
				
			case "rm":
				rm(server, dir);
				break;
				
			case "getattr":
				getattr(server, dir);
				break;
				
			case "cp":
				if (cmd.length < 3) {
					System.out.println("Error: Misssing or no arguments specified.");
					continue;
				}
				String server2, dir2;
				
				if (!cmd[2].matches(".+@.+")) {
					server2 = "localhost";
					dir2 = cmd[2];
				}
				else {
					server2 = cmd[2].split("@")[0];
					dir2 = cmd[2].split("@")[1];
				}
				
				cp(server, dir, server2, dir2);
				break;
				
			case "sync":
				if (cmd.length < 3) {
					System.out.println("Error: Misssing or no arguments specified.");
					continue;
				}
				
				if (!cmd[2].matches(".+@.+")) {
					server2 = "localhost";
					dir2 = cmd[2];
				}
				else {
					server2 = cmd[2].split("@")[0];
					dir2 = cmd[2].split("@")[1];
				}
				
				sync(dir, server2, dir2);
				break;
				
			case "help":
				System.out.println(" servers - lists the names of the file servers known by the system.");
				System.out.println(" servers [NAME] - lists the adresses of the servers with the given name.");
				System.out.println(" ls [SERVER@DIR] - lists the files in the directory DIR of a given server. SERVER can be an address or a name.");
				System.out.println(" mkdir [SERVER@DIR] - creates the directory DIR on a given server. SERVER can be an address or a name.");
				System.out.println(" rmdir [SERVER@DIR] - removes the directory DIR on a given server. SERVER can be an address or a name.");
				System.out.println(" cp [SERVER1@DIR1] [SERVER2,DIR2] - copies DIR1 located in SERVER1 to DIR2 located on SERVER2. SERVER1 and SERVER2 can be addresses or names.");
				System.out.println(" rm [SERVER@PATH_TO_FILE] - removes the file PATH_TO_FILE located in SERVER. SERVER can be an address or a name.");
				System.out.println(" getattr [SERVER@PATH_TO_FILE] - Returns information about the specified file PATH_TO_FILE located in SERVER. SERVER can be an address or a name.");
				System.out.println(" sync [DIR] [SERVER@PATH_TO_FILE] - Syncs a local folder with a remote folder.");
				break;
				
			default:
				System.out.println("Invalid command. Try 'help' for the list of available commands.");
				break;
			}
		}
		
	}

	public static void main(String[] args) throws NotBoundException {
		if( args.length != 1) {
			System.out.println("Use: java FileClient contactServerURL");
			return;
		}
		try {
			new FileClient(args[0]).commandInterpreter();
		} catch (IOException e) {
			System.err.println("Error:" + e.getMessage());
			e.printStackTrace();
		}
	}

}
