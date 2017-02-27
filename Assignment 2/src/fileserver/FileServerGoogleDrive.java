package fileserver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TimeZone;
import java.util.Date;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;

import contactserver.ContactServer;
import exceptions.InfoNotFoundException;

@SuppressWarnings("unused")
public class FileServerGoogleDrive implements FileServer {

	private static final String PROTECTED_RESOURCE_URL = "https://www.googleapis.com/";
	private static final String apiKey = "";
	private static final String apiSecret = "";
	private static final String scope = "https://www.googleapis.com/auth/drive";
	private static final String callback = "urn:ietf:wg:oauth:2.0:oob";
	private OAuthService service = new ServiceBuilder()
			.provider(Google2Api.class).apiKey(apiKey).scope(scope)
			.apiSecret(apiSecret).callback(callback).build();
	private Token accessToken = null;
	private String fileServerName;
	private String contactServerURL;
	private boolean isPrimary;

	public FileServerGoogleDrive(String name, String contactServerURL) {
		this.fileServerName = name;
		this.contactServerURL = contactServerURL;
		this.isPrimary = false;
	}

	public static void main(String[] args) throws IOException,
			ClassNotFoundException, InfoNotFoundException {
		
		if (args.length < 2) {
			System.out.println("Use: java FileServerGoogleDrive fileServerName contactServerURL");
			System.exit(1);
		}

		FileServerGoogleDrive fs = new FileServerGoogleDrive(args[0], args[1]);

//		Scanner in = new Scanner(System.in);
//		String authorizationUrl = fs.service.getAuthorizationUrl(null);
//		System.out.println("Got the Authorization URL!");
//		System.out.println("Now go and authorize Scribe here:");
//		System.out.println(authorizationUrl);
//		System.out.println("And paste the authorization code here");
//		System.out.print(">>");
//		Verifier verifier = new Verifier(in.nextLine());
//		System.out.println();
//		
//		// Trade the Request Token and Verfier for the Access Token
//		System.out.println("Trading the Request Token for an Access Token...");
//		Token accessToken = fs.service.getAccessToken(null, verifier);
//		System.out.println("Got the Access Token!");		
//		FileOutputStream fos = new FileOutputStream("googledrivetoken");
//		ObjectOutputStream oos = new ObjectOutputStream(fos);
//		
//		oos.writeObject(accessToken);
//		oos.close();

		FileInputStream fis = new FileInputStream("googledrivetoken");
		ObjectInputStream ois = new ObjectInputStream(fis);
		fs.accessToken = (Token) ois.readObject();
		ois.close();

		
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
			System.out.println("Connection with the Contact Server failed.");
		}
	}

	private JSONObject requestResource(String requestURL) {
		System.out.println(requestURL);

		OAuthRequest request = new OAuthRequest(Verb.GET, requestURL);

		service.signRequest(accessToken, request);
		Response response = request.send();
		JSONParser parser = new JSONParser();
		JSONObject res = null;

		try {
			res = (JSONObject) parser.parse(response.getBody());
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return res;
	}

	private String folderExists(String dir) {

		String folderID = null;

		JSONObject res = requestResource(PROTECTED_RESOURCE_URL
				+ "drive/v2/about");

		String rootfolderid = (String) res.get("rootFolderId");
		
		if (dir.equals("."))
			return rootfolderid;

		System.out.println("Root folder ID: " + rootfolderid);

		res = requestResource(PROTECTED_RESOURCE_URL + "drive/v2/files?q='"
				+ rootfolderid + "'+in+parents");

		int listCount = 0;

		JSONArray items = (JSONArray) res.get("items");
		String[] fileList = new String[items.size()];
		Iterator<?> it = items.iterator();

		while (it.hasNext()) {
			JSONObject file = (JSONObject) it.next();
			fileList[listCount++] = (String) file.get("title");
		}


		String[] path = dir.split("/");

		for (String s : path) { // optimizar
			if (Arrays.asList(fileList).contains(s)) {
				it = items.iterator();
				while (it.hasNext()) {
					JSONObject folder = (JSONObject) it.next();
					if (folder.get("title").equals(s)
							&& folder.get("mimeType").equals(
									"application/vnd.google-apps.folder")) {
						folderID = (String) folder.get("id");
						res = requestResource(PROTECTED_RESOURCE_URL
								+ "drive/v2/files?q='" + folderID
								+ "'+in+parents");
						listCount = 0;

						items = (JSONArray) res.get("items");
						fileList = new String[items.size()];
						it = items.iterator();

						while (it.hasNext()) {
							JSONObject file = (JSONObject) it.next();
							fileList[listCount++] = (String) file.get("title");
						}
						break;
					}
				}
			} else
				return null;
		}
		return folderID;
	}

	public String getFileServerName() throws RemoteException {
		return fileServerName;
	}

	public String getContactServerURL() throws RemoteException {
		return contactServerURL;
	}

	public String[] ls(String dir) throws InfoNotFoundException, RemoteException {

		String folderID = folderExists(dir);
		if (folderID == null)
			return null;

		JSONObject res = requestResource(PROTECTED_RESOURCE_URL
				+ "drive/v2/files?q='" + folderID + "'+in+parents");

		int listCount = 0;

		JSONArray items = (JSONArray) res.get("items");
		String[] fileList = new String[items.size()];
		Iterator<?> it = items.iterator();

		while (it.hasNext()) {
			JSONObject file = (JSONObject) it.next();
			fileList[listCount++] = (String) file.get("title");
		}
		return fileList;
	}

	@SuppressWarnings("unchecked")
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
		
		if (dir.split("/").length > 1) {
			int lastSlashIndex = dir.lastIndexOf("/");

			String folderID = folderExists(dir.substring(0, lastSlashIndex));

			String[] path = dir.split("/");

			try {
				if (folderID != null
						&& !Arrays.asList(ls(dir.substring(0, lastSlashIndex)))
								.contains(path[path.length - 1])) {
					OAuthRequest request = new OAuthRequest(Verb.POST,
							"https://www.googleapis.com/drive/v2/files");

					JSONObject res = new JSONObject();
					res.put("title", path[path.length - 1]);
					res.put("mimeType", "application/vnd.google-apps.folder");

					JSONObject parent = new JSONObject();
					parent.put("kind", "drive#parentReference");
					parent.put("id", folderID);

					res.put("parents", Arrays.asList(parent));

					request.addPayload(res.toString());
					request.addHeader("Content-Length", "" + res.size());
					request.addHeader("Content-Type", "application/json");

					service.signRequest(accessToken, request);

					Response response = request.send();

					System.out.println(response.getBody());
				}

			} catch (InfoNotFoundException e) {
					e.printStackTrace();
			}

			return true;
		}
		else {
			String folderID = folderExists(dir);
			if (folderID == null) {
				OAuthRequest request = new OAuthRequest(Verb.POST,
						"https://www.googleapis.com/drive/v2/files");

				JSONObject res = new JSONObject();
				res.put("title", dir);
				res.put("mimeType", "application/vnd.google-apps.folder");

				request.addPayload(res.toString());
				request.addHeader("Content-Length", "" + res.size());
				request.addHeader("Content-Type", "application/json");

				service.signRequest(accessToken, request);

				Response response = request.send();

				System.out.println(response.getBody());
			}
			return true;
		}


	}

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

		String folderID = folderExists(dir);

		if (folderID != null) {
			OAuthRequest request = new OAuthRequest(Verb.DELETE,
					PROTECTED_RESOURCE_URL + "drive/v2/files/" + folderID);
			service.signRequest(accessToken, request);
			Response response = request.send();

			System.out.println(response.getBody());
			return true;
		}

		return false;
	}

	@SuppressWarnings("unchecked")
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
		
		String fromFolder;
		String toFolder;
		String filename;
		String toFilename;
		
		if (fromPath.split("/").length > 1) {
			int lastSlashIndex = fromPath.lastIndexOf("/");
			fromFolder = fromPath.substring(0, lastSlashIndex);
			filename = fromPath.substring(lastSlashIndex + 1,
					fromPath.length());
		}
		else {
			fromFolder = ".";
			filename = fromPath;
		}
		
		if(toPath.split("/").length > 1) {
			int lastSlashIndex = toPath.lastIndexOf("/");
			toFolder = toPath.substring(0, lastSlashIndex);
			toFilename = toPath.substring(lastSlashIndex+1, toPath.length());
		}
		else {
			toFolder = ".";
			toFilename = toPath;
		}

		String fromFolderID = folderExists(fromFolder);
		String toFolderID = folderExists(toFolder);

		JSONObject res = requestResource(PROTECTED_RESOURCE_URL
				+ "drive/v2/files?q='" + fromFolderID + "'+in+parents");

		JSONArray items = (JSONArray) res.get("items");
		Iterator<?> it = items.iterator();

		JSONObject sourcefile = null;

		while (it.hasNext()) {
			sourcefile = (JSONObject) it.next();
			if (!sourcefile.get("title").equals(filename))
				sourcefile = null;
			else
				break;
		}

		if (folderExists(fromFolder) == null || folderExists(toFolder) == null
				|| sourcefile == null)
			return false;

		OAuthRequest request = new OAuthRequest(Verb.POST,
				"https://www.googleapis.com/drive/v2/files/" + sourcefile.get("id") + "/copy");

		JSONObject copy = sourcefile;
		JSONArray newparentsArray = new JSONArray();
		JSONObject newparent = new JSONObject();
		
		newparent.put("kind", "drive#parentReference");
		newparent.put("id", toFolderID);
		
		newparentsArray.add(newparent);
		
		copy.put("parents", newparentsArray);
		
		copy.put("title", toFilename);

		request.addPayload(copy.toString());
		request.addHeader("Content-Length", "" + copy.size());
		request.addHeader("Content-Type", "application/json");

		service.signRequest(accessToken, request);
		
		Response response = request.send();
		
		System.out.println(response.getBody());
		return false;
	}

	public boolean rm (String path) throws RemoteException {
		
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
		
		if (path.split("/").length > 1) {
			int lastSlashIndex = path.lastIndexOf("/");
			
			String filename = path.substring(lastSlashIndex+1, path.length());
			
			String folderID = folderExists(path.substring(0, lastSlashIndex));
			
			JSONObject res = requestResource(PROTECTED_RESOURCE_URL
					+ "drive/v2/files?q='" + folderID + "'+in+parents");

			JSONArray items = (JSONArray) res.get("items");
			Iterator<?> it = items.iterator();

			JSONObject file = null;

			while (it.hasNext()) {
				file = (JSONObject) it.next();
				if (!file.get("title").equals(filename))
					file = null;
				else
					break;
			}

			if (folderID != null && file != null) {
				OAuthRequest request = new OAuthRequest(Verb.DELETE,
						PROTECTED_RESOURCE_URL + "drive/v2/files/" + file.get("id"));
				service.signRequest(accessToken, request);
				Response response = request.send();

				System.out.println(response.getBody());
				return true;
			}
			return false;
		}
		else {
			String filename = path;
			
			String folderID = folderExists(".");
			
			JSONObject res = requestResource(PROTECTED_RESOURCE_URL
					+ "drive/v2/files?q='" + folderID + "'+in+parents");

			JSONArray items = (JSONArray) res.get("items");
			Iterator<?> it = items.iterator();

			JSONObject file = null;

			while (it.hasNext()) {
				file = (JSONObject) it.next();
				if (!file.get("title").equals(filename))
					file = null;
				else
					break;
			}

			if (folderID != null && file != null) {
				OAuthRequest request = new OAuthRequest(Verb.DELETE,
						PROTECTED_RESOURCE_URL + "drive/v2/files/" + file.get("id"));
				service.signRequest(accessToken, request);
				Response response = request.send();

				System.out.println(response.getBody());
				return true;
			}
			return false;
		}
		
	}

	public FileInfo getAttr(String path) throws RemoteException {
		if (path.split("/").length > 1) {
			int lastSlashIndex = path.lastIndexOf("/");
			String folderID = folderExists(path.substring(0, lastSlashIndex));
			if (folderID == null)
				return null;

			JSONObject res = requestResource(PROTECTED_RESOURCE_URL
					+ "drive/v2/files?q='" + folderID + "'+in+parents");

			JSONArray items = (JSONArray) res.get("items");
			Iterator<?> it = items.iterator();

			while (it.hasNext()) {
				JSONObject file = (JSONObject) it.next();
				if (((String) file.get("title")).equals(path.substring(
						lastSlashIndex + 1, path.length()))) {
					SimpleDateFormat sdf = new SimpleDateFormat(
							"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
					sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
					try {
						return new FileInfo(
								(String) file.get("title"),
								Long.parseLong((String) file.get("fileSize")),
								(Date) sdf.parse((String) file.get("modifiedDate")),
								!((String) file.get("mimeType"))
										.equals("application/vnd.google-apps.folder"));
					} catch (java.text.ParseException e) {
						return null;
					}
				}
			}
			return null;
		}
		else {
			String folderID = folderExists(".");
			if (folderID == null)
				return null;

			JSONObject res = requestResource(PROTECTED_RESOURCE_URL
					+ "drive/v2/files?q='" + folderID + "'+in+parents");

			JSONArray items = (JSONArray) res.get("items");
			Iterator<?> it = items.iterator();

			while (it.hasNext()) {
				JSONObject file = (JSONObject) it.next();
				if (((String) file.get("title")).equals(path)) {
					SimpleDateFormat sdf = new SimpleDateFormat(
							"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
					sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
					try {
						return new FileInfo(
								(String) file.get("title"),
								Long.parseLong((String) file.get("fileSize")),
								(Date) sdf.parse((String) file.get("modifiedDate")),
								!((String) file.get("mimeType"))
										.equals("application/vnd.google-apps.folder"));
					} catch (java.text.ParseException e) {
						return null;
					}
				}
			}
			return null;
		}
	}

	@SuppressWarnings("unchecked")
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
		
		String[] path = toPath.split("/");
		
		if (path.length != 1){
			
			int lastSlashIndex = toPath.lastIndexOf("/");
			
			System.out.println(toPath.substring(0, lastSlashIndex));
			
			String folderID = folderExists(toPath.substring(0, lastSlashIndex));
			
			if (folderID == null)
				return false;
			
			OAuthRequest request = new OAuthRequest(Verb.POST,
					"https://www.googleapis.com/drive/v2/files");
			
			request.addHeader("Authorization", "Bearer " + accessToken.getToken());
					
			JSONObject file = new JSONObject();

			file.put("title", path[path.length - 1]);
			file.put("mimeType", "mime/type");

			JSONObject parent = new JSONObject();
			parent.put("kind", "drive#parentReference");
			parent.put("id", folderID);

			file.put("parents", Arrays.asList(parent));
			
			request.addPayload(file.toString());
			request.addHeader("Content-Length", "" + file.size());
			request.addHeader("Content-Type", "application/json");
			
			Response response = request.send();
			
			JSONParser parser = new JSONParser();
			JSONObject res = null;

			try {
				res = (JSONObject) parser.parse(response.getBody());
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			request = new OAuthRequest(Verb.PUT,
					PROTECTED_RESOURCE_URL + "upload/drive/v2/files/" + res.get("id") + "?uploadType=media");
			request.addHeader("Authorization", "Bearer " + accessToken.getToken());
			request.addHeader("Content-Type", "mime/type");
			
			request.addPayload(bytes);
			
			response = request.send();
			
			System.out.println(response.getBody());
			
			return true;
		}
		else {
			OAuthRequest request = new OAuthRequest(Verb.POST,
					"https://www.googleapis.com/drive/v2/files");
			
			request.addHeader("Authorization", "Bearer " + accessToken.getToken());
					
			JSONObject file = new JSONObject();

			file.put("title", path[path.length - 1]);
			file.put("mimeType", "mime/type");
			
			request.addPayload(file.toString());
			request.addHeader("Content-Length", "" + file.size());
			request.addHeader("Content-Type", "application/json");
			
			Response response = request.send();
			
			JSONParser parser = new JSONParser();
			JSONObject res = null;

			try {
				res = (JSONObject) parser.parse(response.getBody());
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			request = new OAuthRequest(Verb.PUT,
					PROTECTED_RESOURCE_URL + "upload/drive/v2/files/" + res.get("id") + "?uploadType=media");
			request.addHeader("Authorization", "Bearer " + accessToken.getToken());
			request.addHeader("Content-Type", "mime/type");
			
			request.addPayload(bytes);
			
			response = request.send();
			
			System.out.println(response.getBody());
			
			return true;
		}
	
	}

	public byte[] download(String path) throws RemoteException {
		
		String filename;
		String folderID;
		
		if (path.split("/").length > 1) {
			int lastSlashIndex = path.lastIndexOf("/");
			filename = path.substring(lastSlashIndex+1, path.length());
			folderID = folderExists(path.substring(0, lastSlashIndex));
		}
		else {
			filename = path;
			folderID = folderExists(".");
		}
		
		JSONObject res = requestResource(PROTECTED_RESOURCE_URL
				+ "drive/v2/files?q='" + folderID + "'+in+parents");

		JSONArray items = (JSONArray) res.get("items");
		Iterator<?> it = items.iterator();

		JSONObject file = null;

		while (it.hasNext()) {
			file = (JSONObject) it.next();
			if (!file.get("title").equals(filename))
				file = null;
			else
				break;
		}
		
		if (file == null || folderID == null) 
			return null;
		
		OAuthRequest request = new OAuthRequest(Verb.GET,
				PROTECTED_RESOURCE_URL + "drive/v2/files/" + file.get("id") + "?alt=media");
		
		request.addHeader("Authorization", "Bearer " + accessToken.getToken());
		
		Response response = request.send();

		long size =  Long.parseLong((String) file.get("fileSize"));
		
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
