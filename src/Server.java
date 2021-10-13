package gomoku;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class Server extends NetService{
	
	private int clientlimit = 10;//set the upper limit of the number of clients
	
	private int clientcounter = 0;
	private Socket[] socket= new Socket[clientlimit];//create socket object
	private InputStream[] is = new InputStream[clientlimit];
	private OutputStream[] os = new OutputStream[clientlimit];
	private BufferedReader[] br = new BufferedReader[clientlimit];
	private PrintWriter[] pw = new PrintWriter[clientlimit];
	private NetStateChange nsc;	
	private ServerSocket serverSocket;//create server socket object
	
	private static Server server;
	  
	private String serverName = "127.0.0.1";
	private int serverPort = 1594;
	  
	public Server(String name, int port) {//Constructor
		serverName = name;
		serverPort = port;
	}
	
	private Server() {//Private Constructor
	  	
	}
	
	public static Server getInstance(ServerController.NetType netType) {//just like initialize
	    if (server == null) {
			server = new Server();
        }
	    return server;
	}
	
	public void setLocalIP() {
		try {
			serverName = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			System.out.println("setLocalIP fail\n");
			e.printStackTrace();
		}
	}
	
	public void startServer() {
		new ServerThread().start();
	}
	
	void init(int id) throws IOException {
		is[id] = socket[id].getInputStream();
		os[id] = socket[id].getOutputStream();
    	br[id] = new BufferedReader(new InputStreamReader(is[id], "utf-8"));
    	pw[id] = new PrintWriter(new OutputStreamWriter(os[id], "utf-8"));
	}
	
	//send message through "socket"
	public void sendMessage(String message, int targetid) {
    
		new Thread(new Runnable() {
    	
        @Override
        public void run() {        	
        	if(targetid<0) {//broadcast
        		for(int i=0;i<clientcounter;i++) {
        			pw[i].println(message);
            		pw[i].flush();
            		//flush()方法：沖走。意思是把緩衝區的內容強制的寫出。因為作業系統的某些機制，為了防止一直不停地磁碟讀寫，所以有了延遲寫入的概念。
        		}
        	}
        	else {//send message to specific client
        		pw[targetid].println(message);
        		pw[targetid].flush();
        	}
        }
    	}).start();
	}
	
	public void close() {//在GomokuServer.java用到
		try {
			for(int i=0;i<clientcounter;i++) {
				pw[i].close();
				socket[i].close();
	      	}
			serverSocket.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void setNetStateChangeListener(NetStateChange nsc) {
		this.nsc = nsc;
	}
	
	class ServerThread extends Thread {
		
		@Override
		public void run() {
			while (true) {
				try {
					InetSocketAddress serverSocketAddress = new InetSocketAddress(serverName, serverPort);//set server address
					serverSocket = new ServerSocket();
					serverSocket.bind(serverSocketAddress);//Binds the ServerSocket to a specific address
					System.out.println(serverSocket.getInetAddress());
					if (nsc != null) {
						nsc.onServerOK();
					}
					try {
						while(clientcounter<clientlimit) {
							socket[clientcounter] = serverSocket.accept();//Accept new client's connection                	
							Thread thread = new Thread(new ClientHandlingTask(clientcounter));				
							thread.start();//Create a thread to execute ClientHandlingTask(socket) to serve the client	
							clientcounter++;
						}
					} catch (IOException e) {
						System.out.print("serverSocket accept() stop\n");
						//e.printStackTrace();
					} finally {
						System.out.print("serverSocket accept() over\n");
					}
					break;
				} catch (IOException e) {
					System.out.print("Server failure\n");
					e.printStackTrace();
					try {
						serverSocket.close();
	                } catch (IOException ex) {
	                     //ignore this
	                }
				}finally {
					try {
						serverSocket.close();
					} catch (IOException e) {
						//e.printStackTrace();
					}
					System.out.println("Server shutdown.");
				}
			}
		}
	}
	
	void startRead(int id) {
		ReadThread reader = new ReadThread(id);
		reader.start();
	}
	
	class ReadThread extends Service<String> {

		private int clientid = -1;
		
		public ReadThread(int id) {//Constructor
			clientid = id;
		}
		
		@Override
		protected void succeeded() {
			super.succeeded();          
			if (getValue() != null && getValue().length() > 0) {//received message from client through "socket"
				if (nsc != null) {
					//System.out.println("ReadThread get value: "+ getValue());//Testing
					nsc.onMessage(getValue());//call function to react according to different message received
				}
			}
			this.restart();
		}

		@Override
		protected Task<String> createTask() {
			return new Task<String>() {
				protected String call() throws Exception {
					return readMessage(clientid);
				}
			};
		}
	}

	public String readMessage(int id) {
		String message = null;
		try {
			message = br[id].readLine();
		}catch (SocketException se){
			if(nsc!=null){
				nsc.onDisconnect();
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
		return message;
	}
	
	//設計好的類別若已經繼承了其他父類別，而無法再繼承 Thread 類別時，則可以「實作 Runnable 介面」間接達成執行緒的設計
	private class ClientHandlingTask implements Runnable {
		
		private int clientid = -1;
		
		public ClientHandlingTask(int id) {//Constructor
			clientid = id;
		}

		@Override
		public void run() {			
			//get the IP address and port number of the remote client
			try {
				init(clientid);
				if (nsc != null) {
	                nsc.onConnect(clientid);//will send "net ok" message to client
	                nsc.onMessage(buildMessage(ServerController.HEAD_NET, "some one connected!:" + clientid));
	            }
	            startRead(clientid);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				//System.out.println("client" + clientid + " thread was created\n");
			}   
		}	
	}
	
	int opponentid(String msg) {
		String[] msgArray = msg.split(":");
		int temp = Integer.valueOf(msgArray[msgArray.length-1]);
	    if(temp%2==0) {
	    	return temp+1;
	   	}
	   	else {
	   		return temp-1;
	   	}
	}
	
}