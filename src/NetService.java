package gomoku;

public class NetService {

	static String buildMessage(String head, String body) {
        return head + ':' + body;
    }

    interface NetStateChange {
        void onServerOK();

        void onConnect(int id);
        
        void onMessage(String message);

        void onDisconnect();
    }
}