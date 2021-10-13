package gomoku;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static gomoku.NetService.buildMessage;

public class ServerController implements NetService.NetStateChange {

    @FXML
    Canvas canvas;
    @FXML
    TextField tfMessage;
    @FXML
    TextArea taContent;
    @FXML
    Label lbIP;
    @FXML
    Button btnConnect;
    @FXML
    Button btnStart;
    @FXML
    Button btnSend;

    private Color colorChessboard = Color.valueOf("#FBE39B");//color of Chess board
    private Color colorLine = Color.valueOf("#884B09");//color of line on Chess board
    private Color colorMark = Color.valueOf("#FF7F27");//color of line mark text on Chess board
    private GraphicsContext gc;
    private double gapX, gapY;
    private double chessSize;
    private double broadPadding = 20;
    private String[] markX = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U"};
    private String[] markY = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21"};

    private Server server;

    static final String HEAD_NET = "net";
    static final String HEAD_MSG = "msg";
    static final String HEAD_BROADCAST = "broadcast";
    static final String HEAD_CHESS = "chess";
    static final String HEAD_GAME = "game";
    static final String HEAD_UNDO = "undo";
    static final String HEAD_WINNER = "winner";
    static final String BODY_OK = "ok";
    static final String BODY_NO = "no";
    static final String BODY_BLACKWIN = "blackwin";
    static final String BODY_WHITEWIN = "whitewin";

    private Position lastPostion;

    private enum Chess {
        BLACK, WHITE
    }

    public enum NetType {
        SERVER, CLIENT
    }

    static NetType netType;
    
    private Chess[][] game = new Chess[21][21];

    @FXML
    protected void handleCanvasClicked(MouseEvent event) {
    	taContent.appendText("[系統]伺服器為公正第三方，無法下棋！\n");
    }

    private void drawChess(Chess chess, Position p) {
        double x = p.x * gapX + broadPadding;
        double y = p.y * gapY + broadPadding;
        switch (chess) {
            case BLACK:
                gc.setFill(Color.BLACK);
                gc.fillOval(x - chessSize / 2, y - chessSize / 2, chessSize, chessSize);
                break;
            case WHITE:
                gc.setFill(Color.WHITE);
                gc.fillOval(x - chessSize / 2, y - chessSize / 2, chessSize, chessSize);
                break;
        }
    }

    private void removeChess() {
        double x = lastPostion.x * gapX + broadPadding;
        double y = lastPostion.y * gapY + broadPadding;
        gc.setFill(colorChessboard);
        gc.fillOval(x - chessSize / 2, y - chessSize / 2, chessSize, chessSize);

        gc.strokeLine(x - chessSize / 2, y, x + chessSize / 2, y);
        gc.strokeLine(x, y - chessSize / 2, x, y + chessSize / 2);
        game[lastPostion.x][lastPostion.y] = null;
    }

    public void initialize() {
        gc = canvas.getGraphicsContext2D();
        gapX = (canvas.getWidth() - broadPadding * 2) / 20;
        gapY = (canvas.getWidth() - broadPadding * 2) / 20;
        System.out.println();
        chessSize = gapX * 0.8;
        cleanChessBoard();
        try {
            lbIP.setText("本機IP：" + InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private void cleanChessBoard() {
        gc.setFill(colorChessboard);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setStroke(colorLine);
        for (int i = 0; i <= 20; i++) {
            gc.strokeLine(i * gapX + broadPadding, broadPadding, i * gapX + broadPadding, canvas.getHeight() - broadPadding);
            gc.strokeLine(broadPadding, i * gapY + broadPadding, canvas.getWidth() - broadPadding, i * gapY + broadPadding);
        }

        gc.setFill(colorMark);
        gc.setFont(Font.font(broadPadding / 2));
        for (int i = 0; i <= 20; i++) {
            gc.fillText(markX[i], i * gapX + broadPadding - 5, broadPadding - 5);
            gc.fillText(markX[i], i * gapX + broadPadding - 5, canvas.getHeight() - 5);
            gc.fillText(markY[i], 5, gapY * i + broadPadding + 5);
            gc.fillText(markY[i], canvas.getWidth() - broadPadding + 5, gapY * i + broadPadding + 5);
        }
    }

    @FXML
    protected void handleStartServer(ActionEvent event) {
        server = Server.getInstance(NetType.SERVER);
        server.startServer();
        server.setNetStateChangeListener(this);
        netType = NetType.SERVER;
    }

    @FXML
    protected void handleBindLocalClicked(ActionEvent event) {
    	server = Server.getInstance(NetType.SERVER);
    	server.setLocalIP();
        server.startServer();
        server.setNetStateChangeListener(this);
        netType = NetType.SERVER;
    }

    @FXML
    protected void handleBroadcastClicked(ActionEvent event) {
        if (tfMessage.getText().length() > 0) {
            String broadcast = buildMessage(HEAD_BROADCAST, tfMessage.getText());
            server.sendMessage(broadcast, -1);
            taContent.appendText("[伺服器廣播]" + tfMessage.getText() + "\n");
        }
        tfMessage.setText("");
    }
    
    @Override
    public void onConnect(int id) {
        System.out.println("some one connected");        
        server.sendMessage(buildMessage( buildMessage(HEAD_NET, BODY_OK),String.valueOf(id) ), id);//second buildMessage is used for setting client id
        taContent.appendText("[系統]玩家" + (id+1) + "已連接到伺服器！\n");
        tfMessage.setDisable(false);
        btnSend.setDisable(false);
    }

    @Override
    public void onMessage(String message) {//according to different message that received from opponent through "socket", react differently (on the local side)    	
    	System.out.println(message);
        String[] msgArray = message.split(":");        
        int sourceid = Integer.valueOf( msgArray[msgArray.length-1] );
        int targetid = opponentid( msgArray[msgArray.length-1] );
        if ( !msgArray[0].equals(HEAD_NET) ) {//forward message to the other client
        	server.sendMessage(message, targetid );
        }
        switch (msgArray[0]) {
        	//for server
        	case HEAD_WINNER:
        		if (msgArray[1].equals(BODY_BLACKWIN) && sourceid%2 == 0) {
        			cleanChessBoard();
                    game = new Chess[21][21];
                    taContent.appendText("[系統]玩家"+ (((targetid+sourceid)/2)+1) +"執黑棋獲勝！新的一局開始了！\n");
        		}
        		else if (msgArray[1].equals(BODY_WHITEWIN) && sourceid%2 == 0){
        			cleanChessBoard();
                    game = new Chess[21][21];             
                    taContent.appendText("[系統]玩家"+ (((targetid+sourceid)/2)+2) +"執白棋獲勝！新的一局開始了！\n");
        		}
        		break;
        	case HEAD_NET:
                if (msgArray[1].equals(BODY_OK)) {
                	taContent.appendText("[系統]玩家" + (sourceid+1) + "已連接到伺服器！\n");
                    tfMessage.setDisable(false);
                    btnSend.setDisable(false);
                    btnStart.setDisable(true);
                    btnConnect.setDisable(true);                    
                    if(sourceid%2==0) {
                    	taContent.appendText("[系統]玩家" + (sourceid+1) + "執黑棋\n");
                    }
                    else {
                    	taContent.appendText("[系統]玩家" + (sourceid+1) + "執白棋，將等待玩家" + (targetid+1) + "先走\n");
                    }
                }
                break;
            case HEAD_MSG:
                StringBuilder msgContent = new StringBuilder();
                for (int i = 1; i < msgArray.length-1; i++) {
                    msgContent.append(msgArray[i]);
                    if (i + 1 < msgArray.length-1) {
                        msgContent.append(':');
                    }
                }
                taContent.appendText("[玩家" + (sourceid+1) + "]" + msgContent.toString() + "\n");
                break;
            case HEAD_CHESS:
                int x = Integer.parseInt(msgArray[1]);
                int y = Integer.parseInt(msgArray[2]);
                Position p = new Position(x, y);
                lastPostion = p;
                taContent.appendText("[玩家" + (sourceid+1) + "]走子：" + markX[x] + "," + markY[y] + "\n");
                if(sourceid==0) {
                	drawChess(Chess.BLACK, p);
                }
                else if(sourceid==1){
                    drawChess(Chess.WHITE, p);
                }                	
                game[p.x][p.y] = Chess.WHITE;
                break;
            case HEAD_GAME:
            	//ignore
                break;
            case HEAD_UNDO:
                if (msgArray[1].equals(BODY_OK)) {
                    taContent.appendText("[玩家" + (sourceid+1) + "]同意悔棋！\n");
                    removeChess();
                } else if (msgArray[1].equals(BODY_NO)) {
                    taContent.appendText("[玩家" + (sourceid+1) + "]拒絕悔棋！\n");
                } else {
                    taContent.appendText(msgArray[1] + "請求悔棋！\n");
                }
                break;
        }
    }

    @Override
    public void onDisconnect() {
        Alert alert = new Alert(Alert.AlertType.ERROR, "連接已中斷！", ButtonType.OK);
        alert.setOnCloseRequest(event -> System.exit(0));
        alert.show();
    }

    @Override
    public void onServerOK() {
        System.out.println("server OK");
        taContent.appendText("[系統]伺服器ok！\n");
        btnStart.setDisable(true);
        btnConnect.setDisable(true);
    }

    int opponentid(String sourceid) {   	
    	int temp = Integer.valueOf(sourceid);
    	if(temp%2==0) {
    		//System.out.println("forward message[" + msg + "] from client[" + temp + "]");
    		return temp+1;
    	}
    	else {
    		//System.out.println("forward message[" + msg + "] from client[" + temp + "]");
    		return temp-1;
    	}
    }
    
    private class Position {
        int x;
        int y;

        Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return x + ":" + y;
        }
    }
}