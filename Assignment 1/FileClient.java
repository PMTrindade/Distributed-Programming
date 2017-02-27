package trabalho1v2;

import java.io.*;
import java.net.MalformedURLException;
import java.rmi.*;

public class FileClient {

	String contactServerURL;
	
	protected FileClient(String url) {
		this.contactServerURL = url;
	}
	
	/**
	 * Devolve um array com os nomes dos servidores a correr caso o name == null ou o URL dos 
	 * servidores a correr com nome name.
	 * @throws NotBoundException 
	 * @throws RemoteException 
	 * @throws MalformedURLException 
	 * @throws InfoNotFoundException 
	 */
	protected String[] servers(String name) throws MalformedURLException, RemoteException, NotBoundException, InfoNotFoundException {
		System.err.println("exec: servers");
		ContactServer cs = (ContactServer) Naming.lookup("//" + contactServerURL + "/ContactServer");
		if (name == null)
			return cs.listFileServerNames();
		return cs.listServerAddresses(name);
	}
	
	/**
	 * Devolve um array com os ficheiros/directoria na directoria dir no servidor server.
	 * (ou no sistema de ficheiros do cliente caso server == null).
	 * Se isURL for verdadeiro, server representa um URL para o servior (e.g. //127.0.0.1/myServer).
	 * Caso contrario e o nome do servidor. Nesse caso deve listar os ficheiro dum servidor com esse nome.
	 * Devolve null em caso de erro.
	 * NOTA: nao deve lancar excepcao.
	 * @throws NotBoundException 
	 * @throws RemoteException 
	 * @throws MalformedURLException 
	 * @throws InfoNotFoundException 
	 */
	protected String[] ls(String server, boolean isURL, String dir) throws MalformedURLException, RemoteException, NotBoundException, InfoNotFoundException {
		System.err.println("exec: ls " + dir + " no servidor " + server + " - e url : " + isURL);
		ContactServer cs = (ContactServer) Naming.lookup("//" + contactServerURL + "/ContactServer");
		cs.updateAllServers();
		FileServer fs;
		try {
			if (server == null) {
				FileServer f = new FileServerRMI("local", "local");
				return f.ls(dir);
			}
			else if (!isURL) {
				String[] url = servers(server);
				fs = (FileServer) Naming.lookup("//" + url[0] + "/FileServer");
			}
			else {
				fs = (FileServer) Naming.lookup("//" + server + "/FileServer");
			}
			return fs.ls(dir);
		} catch (Exception e) {
			System.err.println("Servidor invalido");
		}
		return null;
	}
	
	/**
	 * Cria a directoria dir no servidor server.
	 * (ou no sistema de ficheiros do cliente caso server == null).
	 * Se isURL for verdadeiro, server representa um URL para o servior (e.g. //127.0.0.1/myServer).
	 * Caso contrario e o nome do servidor. Nesse caso deve listar os ficheiro dum servidor com esse nome.
	 * Devolve false em caso de erro.
	 * NOTA: nao deve lancar excepcao.
	 * @throws NotBoundException 
	 * @throws RemoteException 
	 * @throws MalformedURLException 
	 * @throws InfoNotFoundException 
	 */
	protected boolean mkdir(String server, boolean isURL, String dir) throws MalformedURLException, RemoteException, NotBoundException, InfoNotFoundException {
		System.err.println("exec: mkdir " + dir + " no servidor " + server +" - e url : " + isURL);
		ContactServer cs = (ContactServer) Naming.lookup("//" + contactServerURL + "/ContactServer");
		cs.updateAllServers();
		
		FileServer fs;
		try {
			if (server == null) {
				FileServer f = new FileServerRMI("local", "local");
				return f.mkdir(dir);
			}
			else if (!isURL) {
				String[] url = servers(server);
				fs = (FileServer) Naming.lookup("//" + url[0] + "/FileServer");
			}
			else fs = (FileServer) Naming.lookup("/" + server + "/FileServer");
			return fs.mkdir(dir);
		} catch (Exception e) {
			System.err.println("Servidor invalido");
		}
		return false;
	}
	
	/**
	 * Remove a directoria dir no servidor server.
	 * (ou no sistema de ficheiros do cliente caso server == null).
	 * Se isURL for verdadeiro, server representa um URL para o servior (e.g. //127.0.0.1/myServer).
	 * Caso contrario e o nome do servidor. Nesse caso deve listar os ficheiro dum servidor com esse nome.
	 * Devolve false em caso de erro.
	 * NOTA: nao deve lancar excepcao.
	 * @throws NotBoundException 
	 * @throws RemoteException 
	 * @throws MalformedURLException 
	 * @throws InfoNotFoundException 
	 */
	protected boolean rmdir(String server, boolean isURL, String dir) throws MalformedURLException, RemoteException, NotBoundException, InfoNotFoundException {
		System.err.println("exec: mkdir " + dir + " no servidor " + server + " - e url : " + isURL);
		ContactServer cs = (ContactServer) Naming.lookup("//" + contactServerURL + "/ContactServer");
		cs.updateAllServers();
		
		FileServer fs;
		try {
			if (server == null) {
				FileServer f = new FileServerRMI("local", "local");
				return f.rmdir(dir);
			}
			else if (!isURL) {
				String[] url = servers(server);
				fs = (FileServer) Naming.lookup("//" + url[0] + "/FileServer");
			}
			else fs = (FileServer) Naming.lookup("/" + server + "/FileServer");
			return fs.rmdir(dir);
		} catch (Exception e) {
			System.err.println("Servidor invalido");
		}
		return false;
	}
	
	/**
	 * Remove o ficheiro path no servidor server.
	 * (ou no sistema de ficheiros do cliente caso server == null).
	 * Se isURL for verdadeiro, server representa um URL para o servior (e.g. //127.0.0.1/myServer).
	 * Caso contrario e o nome do servidor. Nesse caso deve listar os ficheiro dum servidor com esse nome.
	 * Devolve false em caso de erro.
	 * NOTA: nao deve lancar excepcao.
	 * @throws NotBoundException 
	 * @throws RemoteException 
	 * @throws MalformedURLException 
	 * @throws InfoNotFoundException 
	 */
	protected boolean rm(String server, boolean isURL, String path) throws MalformedURLException, RemoteException, NotBoundException, InfoNotFoundException {
		System.err.println("exec: rm " + path + " no servidor " + server +" - e url : " + isURL);

		//ContactServer cs = (ContactServer) Naming.lookup("//" + contactServerURL + "/ContactServer");
		//cs.updateAllServers();
		
		//FileServer fs;
		FileServer fs = null;
		try {
			if (server == null) {
				FileServer f = new FileServerRMI("local", "local");
				return f.rm(path);
			}
			else if (!isURL) {
				String[] url = servers(server);
				fs = (FileServer) Naming.lookup("//" + url[0] + "/FileServer");
			}
			else fs = (FileServer) Naming.lookup("/" + server + "/FileServer");
			return fs.rm(path);
		} catch (Exception e) {
			System.err.println("Servidor invalido");
		}
		return false;
	}
	
	/**
	 * Devolve informacao sobre o ficheiro/directoria path no servidor server.
	 * (ou no sistema de ficheiros do cliente caso server == null).
	 * Se isURL for verdadeiro, server representa um URL para o servior (e.g. //127.0.0.1/myServer).
	 * Caso contrario e o nome do servidor. Nesse caso deve listar os ficheiro dum servidor com esse nome.
	 * Devolve false em caso de erro.
	 * NOTA: nao deve lancar excepcao.
	 * @throws NotBoundException 
	 * @throws RemoteException 
	 * @throws MalformedURLException 
	 * @throws InfoNotFoundException 
	 */
	protected FileInfo getAttr(String server, boolean isURL, String path) throws MalformedURLException, RemoteException, NotBoundException, InfoNotFoundException {
		System.err.println("exec: getattr " + path +  " no servidor " + server + " - e url : " + isURL);
		ContactServer cs = (ContactServer) Naming.lookup("//" + contactServerURL + "/ContactServer");
		cs.updateAllServers();
		
		FileServer fs;
		try {
			if (server == null) {
				FileServer f = new FileServerRMI("local", "local");
				return f.getAttr(path);
			}
			else if (!isURL) {
				String[] url = servers(server);
				fs = (FileServer) Naming.lookup("//" + url[0] + "/FileServer");
			}
			else fs = (FileServer) Naming.lookup("/" + server + "/FileServer");
			return fs.getAttr(path);
		} catch (Exception e) {
			System.err.println("Servidor invalido");
		}
		return null;
	}
	
	/**
	 * Copia ficheiro de fromPath no servidor fromServer para o ficheiro 
	 * toPath no servidor toServer.
	 * (caso fromServer/toServer == local, corresponde ao sistema de ficheiros do cliente).
	 * Devolve false em caso de erro.
	 * NOTA: nao deve lancar excepcao.
	 * @throws NotBoundException 
	 * @throws IOException 
	 * @throws InfoNotFoundException 
	 */
	protected boolean cp(String fromServer, boolean fromIsURL, String fromPath, String toServer, boolean toIsURL, String toPath) throws NotBoundException, IOException, InfoNotFoundException {
		System.err.println("exec: cp " + fromPath + " no servidor " + fromServer + " - e url : " + fromIsURL + " para " + toPath + " no servidor " + toServer +" - e url : " + toIsURL);
		ContactServer cs = (ContactServer) Naming.lookup("//" + contactServerURL + "/ContactServer");
		cs.updateAllServers();
		
		FileServer fs;
		if (fromServer == null && toServer == null) {
			FileServer f = new FileServerRMI("local", "local");
			return f.cp(fromPath, toPath);
		}
		else if (fromServer == null) {
			if (!toIsURL) {
				String[] url = servers(toServer);
				fs = (FileServer) Naming.lookup("//" + url[0] + "/FileServer");
			}
			else {
				fs = (FileServer) Naming.lookup("/" + toServer + "/FileServer");
			}
			return uploadFile(fs, fromPath, toPath);
		}
		else if (toServer == null) {
			if (!fromIsURL) {
				String[] url = servers(fromServer);
				fs = (FileServer) Naming.lookup("//" + url[0] + "/FileServer");
			}
			else {
				fs = (FileServer) Naming.lookup("/" + fromServer + "/FileServer");
			}
			return downloadFile(fs, fromPath, toPath);
		}
		else if (fromServer.equals(toServer)) {
			if (!fromIsURL) {
				String[] url = servers(fromServer);
				fs = (FileServer) Naming.lookup("//" + url[0] + "/FileServer");
			}
			else fs = (FileServer) Naming.lookup("/" + fromServer + "/FileServer");
			return fs.cp(fromPath, toPath);
		}
		else {
			if (!fromIsURL) {
				String[] url = servers(fromServer);
				fs = (FileServer) Naming.lookup("//" + url[0] + "/FileServer");
			}
			else fs = (FileServer) Naming.lookup("/" + fromServer + "/FileServer");
			if (!toIsURL) {
				toServer = servers(toServer)[0]; //Update
			}
			return fs.sendToServer(fromPath, toServer, toPath, toIsURL);
		}
	}
	
	private boolean uploadFile(FileServer fs, String fromPath, String toPath) {
		try {
			File f = new File(fromPath);
			if (f.exists()) {
				byte[] bytes = new byte[(int) f.length()];
				RandomAccessFile r = new RandomAccessFile(f, "r");
				r.readFully(bytes);
				return fs.download(bytes, toPath);
			} else
				throw new InfoNotFoundException("File not found :" + fromPath);
		} catch (Exception e) {
			System.err.println("Erro: " + e.getMessage());
			return false;
		}
	}
	
	private boolean downloadFile(FileServer fs, String fromPath, String toPath) throws RemoteException {
		byte[] bytes = fs.upload(fromPath);
		try {
			File f = new File(toPath);
			if (!f.exists()) {
				RandomAccessFile raf = new RandomAccessFile(f, "rw");
				raf.write(bytes);
				return true;
			} else
				throw new InfoNotFoundException("File already exists: " + toPath);
		} catch (Exception e) {
			System.err.println("Erro: " + e.getMessage());
			return false;
		}
	}
	
	protected void doit() throws IOException, NotBoundException, InfoNotFoundException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Bem vindo ao FileClient. Insira um comando");
		for( ; ; ) {
			System.out.print("> ");
			String line = reader.readLine();
			if(line == null)
				break;
			String[] cmd = line.split(" ");
			if(cmd[0].equalsIgnoreCase("servers")) {
				String[] s = servers(cmd.length == 1 ? null : cmd[1]);
				if(s == null || s.length == 0)
					System.out.println("error");
				else {
					//System.out.println("Found " + s.length + " addresses" + ":");
					for(int i = 0; i < s.length; i++)
						System.out.println(s[i]);
				}
			} else if(cmd[0].equalsIgnoreCase("ls")) {
				String[] dirserver = cmd[1].split("@");
				String server = dirserver.length == 1 ? null : dirserver[0];
				boolean isURL = dirserver.length == 1 ? false : dirserver[0].indexOf('/') >= 0;
				String dir = dirserver.length == 1 ? dirserver[0] : dirserver[1];
				
				String[] res = ls(server, isURL, dir);
				if(res != null) {
					System.out.println("Numero de objectos: " + res.length);
					for(int i = 0; i < res.length; i++)
						System.out.println( res[i]);
				} else
					System.out.println("error");
			} else if(cmd[0].equalsIgnoreCase("mkdir")) {
				String[] dirserver = cmd[1].split("@");
				String server = dirserver.length == 1 ? null : dirserver[0];
				boolean isURL = dirserver.length == 1 ? false : dirserver[0].indexOf('/') >= 0;
				String dir = dirserver.length == 1 ? dirserver[0] : dirserver[1];
				
				boolean b = mkdir(server, isURL, dir);
				if(b)
					System.out.println("success");
				else
					System.out.println("error");
			} else if(cmd[0].equalsIgnoreCase("rmdir")) {
				String[] dirserver = cmd[1].split("@");
				String server = dirserver.length == 1 ? null : dirserver[0];
				boolean isURL = dirserver.length == 1 ? false : dirserver[0].indexOf('/') >= 0;
				String dir = dirserver.length == 1 ? dirserver[0] : dirserver[1];
				
				boolean b = rmdir(server, isURL, dir);
				if(b)
					System.out.println("success");
				else
					System.out.println("error");
			} else if(cmd[0].equalsIgnoreCase("rm")) {
				String[] dirserver = cmd[1].split("@");
				String server = dirserver.length == 1 ? null : dirserver[0];
				boolean isURL = dirserver.length == 1 ? false : dirserver[0].indexOf('/') >= 0;
				String path = dirserver.length == 1 ? dirserver[0] : dirserver[1];
				
				boolean b = rm(server, isURL, path);
				if(b)
					System.out.println("success");
				else
					System.out.println("error");
			} else if(cmd[0].equalsIgnoreCase("getattr")) {
				String[] dirserver = cmd[1].split("@");
				String server = dirserver.length == 1 ? null : dirserver[0];
				boolean isURL = dirserver.length == 1 ? false : dirserver[0].indexOf('/') >= 0;
				String path = dirserver.length == 1 ? dirserver[0] : dirserver[1];
				
				FileInfo info = getAttr(server, isURL, path);
				if(info != null) {
					System.out.println(info);
					System.out.println("success");
				} else
					System.out.println("error");
			} else if(cmd[0].equalsIgnoreCase("cp")) {
				String[] dirserver1 = cmd[1].split("@");
				String server1 = dirserver1.length == 1 ? null : dirserver1[0];
				boolean isURL1 = dirserver1.length == 1 ? false : dirserver1[0].indexOf('/') >= 0;
				String path1 = dirserver1.length == 1 ? dirserver1[0] : dirserver1[1];
				
				String[] dirserver2 = cmd[2].split("@");
				String server2 = dirserver2.length == 1 ? null : dirserver2[0];
				boolean isURL2 = dirserver2.length == 1 ? false : dirserver2[0].indexOf('/') >= 0;
				String path2 = dirserver2.length == 1 ? dirserver2[0] : dirserver2[1];
				
				boolean b = cp(server1, isURL1, path1, server2, isURL2, path2);
				if(b)
					System.out.println("success");
				else
					System.out.println("error");
			} else if(cmd[0].equalsIgnoreCase("help")) {
				System.out.println("servers - lista nomes de servidores a executar");
				System.out.println("servers nome - lista URL dos servidores com nome nome");
				System.out.println("ls server@dir - lista ficheiros/directorias presentes na directoria dir (. e .. tem o significado habitual), caso existam ficheiros com o mesmo nome devem ser apresentados como nome@server");
				System.out.println("mkdir server@dir - cria a directoria dir no servidor server");
				System.out.println("rmdir server@udir - remove a directoria dir no servidor server");
				System.out.println("cp path1 path2 - copia o ficheiro path1 para path2; quando path representa um ficheiro num servidor deve ter a forma server:path, quando representa um ficheiro local deve ter a forma path");
				System.out.println("rm path - remove o ficheiro path");
				System.out.println("getattr path - apresenta informacao sobre o ficheiro/directoria path, incluindo: nome, boolean indicando se e ficheiro, data da criacao, data da ultima modificacao");
			} else if( cmd[0].equalsIgnoreCase("exit"))
				break;
		}
	}
	
	public static void main(String[] args) throws NotBoundException, InfoNotFoundException {
		if(args.length != 1) {
			System.out.println("Use: java trab1.FileClient URL");
			return;
		}
		try {
			new FileClient(args[0]).doit();
		} catch (IOException e) {
			System.err.println("Error:" + e.getMessage());
			e.printStackTrace();
		}
	}
}
