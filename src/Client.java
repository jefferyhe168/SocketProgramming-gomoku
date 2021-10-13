package gomoku;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class Client extends NetService{
	
	private int clientid = -1;
	private Socket socket;//create socket object
	private InputStream is;
    private OutputStream os;
	private BufferedReader br;
    private PrintWriter pw;
    private NetStateChange nsc;
	

    private static Client client;
    
	private String serverName = "127.0.0.1";
	private int serverPort = 1594;
	
	public Client(String name, int port) {//Constructor
		serverName = name;
		serverPort = port;
	}
	
	private Client() {//Private Constructor
		
	}
	
	public static Client getInstance(ClientController.NetType netType) {//just like initialize
		if (client == null) {
            client = new Client();
        }
		return client;
	}	
	
	void connectToServer(String ip) {
        try {
            if(ip.equals("")) {
            	socket = new Socket(serverName, serverPort);
            }
            else {
            	socket = new Socket(ip, serverPort);
            }
            init();
            startRead();
        } catch (IOException e) {
        	System.out.println("Connect to server fail\n");
        	e.printStackTrace();
        }
    }
	
	public void setClientid(int myid) {
		clientid = myid;
	}
	
	void init() throws IOException {
        is = socket.getInputStream();
        os = socket.getOutputStream();
        br = new BufferedReader(new InputStreamReader(is, "utf-8"));
        pw = new PrintWriter(new OutputStreamWriter(os, "utf-8"));
    }
	
	void startRead() {
        ReadThread reader = new ReadThread();
        reader.start();
    }
	
	public void sendMessage(String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                pw.println( buildMessage(message, String.valueOf(clientid)) );
                pw.flush();
            }
        }).start();
    }
	
	public void close() {//¦bGomokuClient.java¥Î¨ì
        try {
            pw.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	public String readMessage() {
        String message = null;
        try {
            message = br.readLine();
        }catch (SocketException se){
            if(nsc!=null){
                nsc.onDisconnect();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return message;
    }
	
	void setNetStateChangeListener(NetStateChange nsc) {
        this.nsc = nsc;
    }
	
	class ReadThread extends Service<String> {

        @Override
        protected void succeeded() {
            super.succeeded();            
            if (getValue() != null && getValue().length() > 0) {
                if (nsc != null) {
                	//System.out.println("ReadThread get value: "+ getValue()); //Testing
                    nsc.onMessage(getValue());
                }
            }
            this.restart();
        }

        @Override
        protected Task<String> createTask() {
            return new Task<String>() {
                protected String call() throws Exception {
                    return readMessage();
                }
            };
        }
    }

}