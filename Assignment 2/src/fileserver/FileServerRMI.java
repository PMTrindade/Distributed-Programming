package fileserver;

import java.io.File;
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
import java.util.Iterator;
import java.util.Set;

import contactserver.ContactServer;
import exceptions.InfoNotFoundException;

public class FileServerRMI implements FileServer {

	private String fileServerName;
	private String contactServerURL;
	private boolean isPrimary;
	
	public FileServerRMI(String fileServerName, String contactServerURL) throws RemoteException {
		this.fileServerName = fileServerName;
		this.contactServerURL = contactServerURL;
		this.isPrimary = false;
	}
	
	public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException, UnknownHostException {
		FileServer fs = new FileServerRMI(args[0], args[1]);
		
		System.getProperties().put("java.security.policy", "src/policy.all");
		
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
			System.out.println("Ligacao com o Contact Server falhou.");
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
		if (isPrimary) {
			try {
				try {
					ContactServer cs = (ContactServer) Naming.lookup("//" + contactServerURL + "/ContactServer");
					Set<InetAddress> secondaryServers = cs.listServerAddresses(fileServerName);
					Iterator<InetAddress> it = secondaryServers.iterator();
					while (it.hasNext()) {
						InetAddress addr = it.next();
						try {
							if(!addr.getHostAddress().equals(InetAddress.getLocalHost().getHostAddress())) {
								try {
									FileServer fs = null;
									fs = (FileServer) Naming.lookup("//" + addr.getHostAddress() + "/FileServer");
									if(fs != null)
										fs.mkdir(dir);
								} catch (ConnectException e) {
									System.out.println("Error connecting with " + addr.toString());
								}
								
							}
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
					}
				} catch (MalformedURLException | RemoteException e) {
					e.printStackTrace();
				}
				
			} catch (NotBoundException e) {
				System.out.println("Ligacao com o Contact Server falhou.");
			}

		}
		File f = new File(dir);
		return f.mkdir();
	}
	
	public boolean rmdir(String dir) {
		File f = new File(dir);
		
		if (isPrimary) {
			try {
				try {
					ContactServer cs = (ContactServer) Naming.lookup("//" + contactServerURL + "/ContactServer");
					Set<InetAddress> secondaryServers = cs.listServerAddresses(fileServerName);
					Iterator<InetAddress> it = secondaryServers.iterator();
					while (it.hasNext()) {
						InetAddress addr = it.next();
						try {
							if(!addr.getHostAddress().equals(InetAddress.getLocalHost().getHostAddress())) {
								try {
									FileServer fs = null;
									fs = (FileServer) Naming.lookup("//" + addr.getHostAddress() + "/FileServer");
									if(fs != null)
										fs.rmdir(dir);
								} catch (ConnectException e) {
									System.out.println("Error connecting with " + addr.toString());
								}
								
							}
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
					}
				} catch (MalformedURLException | RemoteException e) {
					e.printStackTrace();
				}
				
			} catch (NotBoundException e) {
				System.out.println("Ligacao com o Contact Server falhou.");
			}

		}
		
	    File[] files = f.listFiles();
	    if(files!=null) {
	        for(File fi: files) {
	            if(f.isDirectory()) {
	                rmdir(fi.getPath());
	            } else {
	                if (!fi.delete())
	                	return false;
	            }
	        }
	    }
	    if (!f.delete()) {
	    	return false;
	    }
	    
	    return true;
	}
	
	public boolean cp(String fromPath, String toPath) throws IOException {
		if (isPrimary) {
			try {
				try {
					ContactServer cs = (ContactServer) Naming.lookup("//" + contactServerURL + "/ContactServer");
					Set<InetAddress> secondaryServers = cs.listServerAddresses(fileServerName);
					Iterator<InetAddress> it = secondaryServers.iterator();
					while (it.hasNext()) {
						InetAddress addr = it.next();
						try {
							if(!addr.getHostAddress().equals(InetAddress.getLocalHost().getHostAddress())) {
								try {
									FileServer fs = null;
									fs = (FileServer) Naming.lookup("//" + addr.getHostAddress() + "/FileServer");
									if(fs != null)
										fs.cp(fromPath, toPath);
								} catch (ConnectException e) {
									System.out.println("Error connecting with " + addr.toString());
								}
								
							}
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
					}
				} catch (MalformedURLException | RemoteException e) {
					e.printStackTrace();
				}
				
			} catch (NotBoundException e) {
				System.out.println("Ligacao com o Contact Server falhou.");
			}

		}
		
		try {
			Path res = Files.copy(new File(fromPath).toPath(), new File(toPath).toPath(), StandardCopyOption.REPLACE_EXISTING);
			return res.equals(new File(toPath).toPath());
		} catch (Exception e) {
			return false;
		}
	}
	
	public boolean rm(String path) {
		if (isPrimary) {
			try {
				try {
					ContactServer cs = (ContactServer) Naming.lookup("//" + contactServerURL + "/ContactServer");
					Set<InetAddress> secondaryServers = cs.listServerAddresses(fileServerName);
					Iterator<InetAddress> it = secondaryServers.iterator();
					while (it.hasNext()) {
						InetAddress addr = it.next();
						try {
							if(!addr.getHostAddress().equals(InetAddress.getLocalHost().getHostAddress())) {
								try {
									FileServer fs = null;
									fs = (FileServer) Naming.lookup("//" + addr.getHostAddress() + "/FileServer");
									if(fs != null)
										fs.rm(path);
								} catch (ConnectException e) {
									System.out.println("Error connecting with " + addr.toString());
								}
								
							}
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
					}
				} catch (MalformedURLException | RemoteException e) {
					e.printStackTrace();
				}
				
			} catch (NotBoundException e) {
				System.out.println("Ligacao com o Contact Server falhou.");
			}

		}
		
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
	
	public boolean upload(byte[] bytes, String path) throws RemoteException {
		if (isPrimary) {
			try {
				try {
					ContactServer cs = (ContactServer) Naming.lookup("//" + contactServerURL + "/ContactServer");
					Set<InetAddress> secondaryServers = cs.listServerAddresses(fileServerName);
					Iterator<InetAddress> it = secondaryServers.iterator();
					while (it.hasNext()) {
						InetAddress addr = it.next();
						try {
							if(!addr.getHostAddress().equals(InetAddress.getLocalHost().getHostAddress())) {
								try {
									FileServer fs = null;
									fs = (FileServer) Naming.lookup("//" + addr.getHostAddress() + "/FileServer");
									if(fs != null)
										fs.upload(bytes, path);
								} catch (ConnectException e) {
									System.out.println("Error connecting with " + addr.toString());
								}
								
							}
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
					}
				} catch (MalformedURLException | RemoteException e) {
					e.printStackTrace();
				}
				
			} catch (NotBoundException e) {
				System.out.println("Ligacao com o Contact Server falhou.");
			}

		}
		
		try {
			File f = new File(path);
			if(!f.exists()) {
				RandomAccessFile raf = new RandomAccessFile(f, "rw");
				raf.write(bytes);
				raf.close();
				return true;
			} else
				throw new InfoNotFoundException("Erro no download : " + path);
		} catch (Exception e) {
			System.err.println("Erro: " + e.getMessage());
			return false;
		}
	}
	@Override
	public byte[] download(String path) throws RemoteException {
		try {
			File f = new File(path);
			if(f.exists()) {
				byte[] b = new byte[(int) f.length()];
				RandomAccessFile raf;
				raf = new RandomAccessFile(f, "r");
				raf.readFully(b);
				raf.close();
				return b;
			} else
				throw new InfoNotFoundException("Erro no upload : " + path);
		} catch (Exception e) {
			System.err.println("Erro: " + e.getMessage());
			return null;
		}
	}
	@Override
	public boolean sendToServer(String fromPath, String toServer, String toPath) throws NotBoundException, IOException { //URLs
		System.out.println(fromPath + " " + toServer + " " + toPath);
		FileServer fs;
		
		File f = new File(fromPath);
		if (f.exists()) {
			fs = (FileServer) Naming.lookup("//" + toServer + "/FileServer");
			byte[] b = new byte[(int) f.length()];
			RandomAccessFile raf;
			raf = new RandomAccessFile(f, "r");
			raf.readFully(b);
			raf.close();
			return fs.upload(b, toPath);
		}
		else return false;
	}
	

	@Override
	public boolean isPrimaryServer() throws RemoteException {
		return isPrimary;
	}

	@Override
	public void setPrimaryServer(boolean status) throws RemoteException {
		isPrimary = status;
	}

}