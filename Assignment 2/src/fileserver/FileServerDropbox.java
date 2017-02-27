package fileserver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.DropBoxApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import contactserver.ContactServer;
import exceptions.InfoNotFoundException;

public class FileServerDropbox implements FileServer {
	
	private static final String API_KEY = "";
	private static final String API_SECRET = "";
	private static final String SCOPE = "dropbox";
	
	private OAuthService service = new ServiceBuilder().provider(DropBoxApi.class).apiKey(API_KEY).apiSecret(API_SECRET).scope(SCOPE).build();
	private Token accessToken = null;
	
	private String fileServerName;
	private String contactServerURL;
	private boolean isPrimary;
	
	public FileServerDropbox(String fileServerName, String contactServerURL) throws RemoteException {
		this.fileServerName = fileServerName;
		this.contactServerURL = contactServerURL;
		this.isPrimary = false;
	}

	public static void main(String[] args) throws InfoNotFoundException, IOException, ClassNotFoundException {
		
		if (args.length < 2) {
			System.out.println("Use: java FileServerDropbox fileServerName contactServerURL");
			System.exit(1);
		}
		
		FileServerDropbox fs = new FileServerDropbox(args[0], args[1]);
		FileInputStream fis = new FileInputStream("dropboxtoken");
		ObjectInputStream ois = new ObjectInputStream(fis);
		fs.accessToken = (Token) ois.readObject();
		ois.close();
		
		System.getProperties().put("java.security.policy", "src/policy.all");
		
		if(System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		
		try {
			LocateRegistry.createRegistry(1099);
		} catch(RemoteException e) {
		}
		
        FileServer stub = (FileServer) UnicastRemoteObject.exportObject(fs, 0);
		Naming.rebind("/FileServer", stub);
		System.out.println("FileServer bound in registry");
		
		try {
			ContactServer cs = (ContactServer) Naming.lookup("//" + fs.getContactServerURL() + "/ContactServer");
			cs.addFileServer(fs.getFileServerName(), InetAddress.getLocalHost());
		} catch (NotBoundException|ConnectException e) {
			System.out.println("Connection with the Contact Server failed.");
		}
	}

	public String getFileServerName() {
		return this.fileServerName;
	}
	
	public String getContactServerURL() {
		return contactServerURL;
	}

	@Override
	public String[] ls(String dir) throws InfoNotFoundException, RemoteException {
		if (dir.equals("."))
			dir = "";
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.dropbox.com/1/metadata/auto/" + dir);
		service.signRequest(accessToken, request);
		Response response = request.send();
		if (response.getCode() == 404)
			System.out.println("Error: Directory not found");
		else if (response.getCode() != 200)
			throw new RuntimeException("Metadata response code:" + response.getCode());

		JSONParser parser = new JSONParser();
		JSONObject res = null;
		try {
			res = (JSONObject) parser.parse(response.getBody());
		} catch (ParseException e) {
			e.printStackTrace();
		}

		int listCount = 0;
		
		JSONArray items = (JSONArray) res.get("contents");
		String[] fileList = new String[items.size()];
		
		Iterator<?> it = items.iterator();

		while (it.hasNext()) {
			JSONObject file = (JSONObject) it.next();
			fileList[listCount++] = (String) file.get("path");
		}
		return fileList;
	}

	@Override
	public boolean mkdir(String dir) throws RemoteException {
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
		OAuthRequest request = new OAuthRequest(Verb.POST, "https://api.dropbox.com/1/fileops/create_folder");
		request.addQuerystringParameter("root", "auto");
		request.addQuerystringParameter("path", dir);
		service.signRequest(accessToken, request);
		Response response = request.send();
		if (response.getCode() != 200) {
			throw new RuntimeException("mkdir response code:" + response.getCode());
		}
		else System.out.println("Folder + '" + dir + "' created successfully");
		return true;
	}

	@Override
	public boolean rmdir(String dir) throws RemoteException {
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
		OAuthRequest request = new OAuthRequest(Verb.POST, "https://api.dropbox.com/1/fileops/delete");
		request.addQuerystringParameter("root", "auto");
		request.addQuerystringParameter("path", dir);
		service.signRequest(accessToken, request);
		Response response = request.send();
		if (response.getCode() != 200) {
			throw new RuntimeException("rmdir response code:" + response.getCode());
		}
		else System.out.println("'" + dir + "' deleted successfully");
		return true;
	}

	@Override
	public boolean cp(String fromPath, String toPath) throws IOException, RemoteException {
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
		OAuthRequest request = new OAuthRequest(Verb.POST, "https://api.dropbox.com/1/fileops/copy");
		request.addQuerystringParameter("root", "auto");
		request.addQuerystringParameter("from_path", fromPath);
		request.addQuerystringParameter("to_path", toPath);
		service.signRequest(accessToken, request);
		Response response = request.send();
		if (response.getCode() != 200) {
			throw new RuntimeException("cp response code:" + response.getCode());
		}
		else System.out.println("'" + fromPath + "' copied successfully to '" + toPath + "'");
		return true;
	}

	@Override
	public boolean rm(String path) throws RemoteException {
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
		return rm(path);
	}

	@Override
	public FileInfo getAttr(String path) throws RemoteException {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.dropbox.com/1/metadata/auto/" + path);
		service.signRequest(accessToken, request);
		Response response = request.send();
		if (response.getCode() != 200)
			throw new RuntimeException("Metadata response code:" + response.getCode());

		JSONParser parser = new JSONParser();
		JSONObject res = null;
		try {
			res = (JSONObject) parser.parse(response.getBody());
		} catch (ParseException e) {
			e.printStackTrace();
		}

		SimpleDateFormat sdf = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault());

		try {
			return new FileInfo((String) res.get("path"), (Long) res.get("bytes"), ((Date) sdf.parse((String) res.get("modified"))), !((Boolean) res.get("is_dir")));
		} catch (java.text.ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean upload(byte[] bytes, String toPath) throws RemoteException {
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
										fs.upload(bytes, toPath);
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
		
		OAuthRequest request = new OAuthRequest(Verb.PUT, "https://api-content.dropbox.com/1/files_put/dropbox/" + toPath);
		request.addHeader("Content-Length", "" + bytes.length);
		request.addHeader("Content-Type", "application/octet-stream");
		request.addPayload(bytes);
		service.signRequest(accessToken, request);
		
		
		Response response = request.send();
		
		System.out.println(response.getBody());
		
		if (response.getCode() != 200) {
			throw new RuntimeException("download response code:" + response.getCode());
		}
		System.out.println("File '" + toPath + "' sucessfully uploaded.");
		return true;
	}

	@Override
	public byte[] download(String path) throws RemoteException {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api-content.dropbox.com/1/files/dropbox/" + path);
		service.signRequest(accessToken, request);
		
		Response response = request.send();
		
		if (response.getCode() != 200) {
			throw new RuntimeException("upload response code:" + response.getCode());
		}
		
		JSONParser parser = new JSONParser();
		JSONObject res = null;
		try {
			res = (JSONObject) parser.parse(response.getHeader("x-dropbox-metadata"));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		long size =  (Long) res.get("bytes");
		
		InputStream stream = response.getStream();
		
		int bytesRead = 0, len = 0;
		byte[] buf;
		
		buf = new byte[(int)size];
		while (len < size) {
			try {
				bytesRead = stream.read(buf, len, (int)size - len);
			} catch (IOException e) {
				System.out.println("Error downloading file.");
				e.printStackTrace();
			}
			len += bytesRead;
		}
		System.out.println("Downloaded file successfully. Size of download file: " + len + " bytes.");
		return buf;
	}

	@Override
	public boolean sendToServer(String fromPath, String toServer, String toPath) throws RemoteException, NotBoundException, IOException {
		byte[] file = download(fromPath);
		if (file == null) {
			System.out.println("Error sending file.");
			return false;
		}
		FileServer fs;
		fs = (FileServer) Naming.lookup("//" + toServer + "/FileServer");
		return fs.upload(file, toPath);
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
