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

public class ClientController implements NetService.NetStateChange {

    @FXML
    Canvas canvas;
    @FXML
    TextField tfIP;
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
    @FXML
    Button btnUndo;

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
    private Client client;
    private int clientid;
    private int opponentid;

    static final String HEAD_NET = "net";
    static final String HEAD_MSG = "msg";
    static final String HEAD_BROADCAST = "broadcast";
    static final String HEAD_CHESS = "chess";
    static final String HEAD_GAME = "game";    
    static final String HEAD_UNDO = "undo";
    static final String HEAD_WINNER = "winner";
    static final String BODY_OK = "ok";
    static final String BODY_NO = "no";
    static final String BODY_PREPARED = "prepared";
    static final String BODY_BLACKWIN = "blackwin";
    static final String BODY_WHITEWIN = "whitewin";

    private boolean isOtherOK = false;
    private boolean blackwin = true;
    
    private Position lastPostion;

    private enum Chess {
        BLACK, WHITE
    }

    public enum NetType {
    	CLIENT1, CLIENT2
    }

    private enum Direction {
        TOP_LEFT, TOP, TOP_RIGHT, RIGHT, RIGHT_DOWN, DOWN, DOWN_LEFT, LEFT
    }

    static NetType netType;

    private Chess currentChess;
    private Chess[][] game = new Chess[21][21];

    @FXML
    protected void handleCanvasClicked(MouseEvent event) {
        Position p = convertPosition(event.getX() - broadPadding, event.getY() - broadPadding);
        if (p.x < 0 || p.y < 0 || p.x > 20 || p.y > 20 || game[p.x][p.y] != null) {
            return;
        }
        if (netType == NetType.CLIENT1) {
            if (isOtherOK) {
                if (currentChess == Chess.BLACK) {
                    drawChess(currentChess, p);
                    client.sendMessage(buildMessage(HEAD_CHESS, p.toString()));
                    taContent.appendText("[玩家" + clientid + "]走子：" + markX[p.x] + "," + markY[p.y] + "\n");
                    currentChess = Chess.WHITE;
                    game[p.x][p.y] = Chess.BLACK;
                    lastPostion = p;
                    btnUndo.setDisable(false);
                    checkWinner(p.x, p.y);
                } else {
                	taContent.appendText("[系統]請等待對手-玩家"+ opponentid +"走棋！\n");
                }
            } else {
            	taContent.appendText("[系統]對手-玩家" + opponentid + "還沒有準備好！\n");
            }
        } else if (netType == NetType.CLIENT2) {
            if (isOtherOK) {
                if (currentChess == Chess.WHITE) {
                    drawChess(currentChess, p);
                    client.sendMessage(buildMessage(HEAD_CHESS, p.toString()));//send chess position to opponent through "socket"
                    taContent.appendText("[玩家" + clientid + "]走子：" + markX[p.x] + "," + markY[p.y] + "\n");
                    currentChess = Chess.BLACK;
                    game[p.x][p.y] = Chess.WHITE;
                    lastPostion = p;
                    btnUndo.setDisable(false);
                    checkWinner(p.x, p.y);
                } else {
                	taContent.appendText("[系統]請等待對手-玩家" + opponentid + "走棋！\n");
                }
            } else {
                taContent.appendText("[系統]對手-玩家" + opponentid + "還沒有準備好！\n");
            }
        }
    }

    private Position convertPosition(double x, double y) {
        int pX = (int) ((x + gapX / 2) / gapX);
        int pY = (int) ((y + gapY / 2) / gapY);
        return new Position(pX, pY);
    }
    //針對新下的這步棋去判斷是否已形成五子(獲勝)
    private void checkWinner(int x, int y) {
        Chess thisChess = game[x][y];
        if (thisChess == null) {
            return;
        }

        int left2Right = 1 + countChessNum(Direction.LEFT, thisChess, x, y) + countChessNum(Direction.RIGHT, thisChess, x, y);
        System.out.println("--" + left2Right);
        if (left2Right >= 5) {
            win(thisChess);
            return;
        }

        int top2Down = 1 + countChessNum(Direction.TOP, thisChess, x, y) + countChessNum(Direction.DOWN, thisChess, x, y);
        System.out.println("|" + top2Down);
        if (top2Down >= 5) {
            win(thisChess);
            return;
        }

        int topLeft2RightDown = 1 + countChessNum(Direction.TOP_LEFT, thisChess, x, y) + countChessNum(Direction.RIGHT_DOWN, thisChess, x, y);
        System.out.println("\\" + topLeft2RightDown);
        if (topLeft2RightDown >= 5) {
            win(thisChess);
            return;
        }

        int topRight2DownLeft = 1 + countChessNum(Direction.TOP_RIGHT, thisChess, x, y) + countChessNum(Direction.DOWN_LEFT, thisChess, x, y);
        System.out.println("/" + topRight2DownLeft);
        if (topRight2DownLeft >= 5) {
            win(thisChess);
        }
    }

    private void win(Chess thisChess) {
        isOtherOK = false;        
        //client tell server who is winner
        if(thisChess == Chess.BLACK) {
        	blackwin = true;
        	client.sendMessage(buildMessage(HEAD_WINNER, BODY_BLACKWIN));            		
        }
    	else {
    		blackwin = false;
    		client.sendMessage(buildMessage(HEAD_WINNER, BODY_WHITEWIN));
    	}
        Alert alert = new Alert(Alert.AlertType.INFORMATION, thisChess == Chess.BLACK ? "黑棋獲勝！" : "白棋獲勝！", ButtonType.OK);        
        alert.show();
        currentChess = Chess.BLACK;
        alert.setOnCloseRequest(event -> {
            cleanChessBoard();
            client.sendMessage(buildMessage(HEAD_GAME, BODY_OK));//send "game ok" to opponent through "socket"
            game = new Chess[21][21];
            if(blackwin == true) {
            	taContent.setText("[系統]黑棋獲勝！新的一局開始了！\n");
            }
            else {
            	taContent.setText("[系統]白棋獲勝！新的一局開始了！\n");
            }
            taContent.appendText("[系統]您為玩家" + clientid + "\n" );
        });
    }

    private int countChessNum(Direction direction, Chess thisChess, int x, int y) {
        int num = 0;
        switch (direction) {
            case TOP_LEFT:
                if (x - 1 >= 0 && y - 1 >= 0 && thisChess == game[x - 1][y - 1]) {
                    num++;
                    num += countChessNum(direction, thisChess, x - 1, y - 1);
                }
                break;
            case TOP:
                if (y - 1 >= 0 && thisChess == game[x][y - 1]) {
                    num++;
                    num += countChessNum(direction, thisChess, x, y - 1);
                }
                break;
            case TOP_RIGHT:
                if (x + 1 <= 20 && y - 1 >= 0 && thisChess == game[x + 1][y - 1]) {
                    num++;
                    num += countChessNum(direction, thisChess, x + 1, y - 1);
                }
                break;
            case RIGHT:
                if (x + 1 <= 20 && thisChess == game[x + 1][y]) {
                    num++;
                    num += countChessNum(direction, thisChess, x + 1, y);
                }
                break;
            case RIGHT_DOWN:
                if (x + 1 <= 20 && y + 1 <= 20 && thisChess == game[x + 1][y + 1]) {
                    num++;
                    num += countChessNum(direction, thisChess, x + 1, y + 1);
                }
                break;
            case DOWN:
                if (y + 1 <= 20 && thisChess == game[x][y + 1]) {
                    num++;
                    num += countChessNum(direction, thisChess, x, y + 1);
                }
                break;
            case DOWN_LEFT:
                if (x - 1 >= 0 && y + 1 <= 20 && thisChess == game[x - 1][y + 1]) {
                    num++;
                    num += countChessNum(direction, thisChess, x - 1, y + 1);
                }
                break;
            case LEFT:
                if (x - 1 >= 0 && thisChess == game[x - 1][y]) {
                    num++;
                    num += countChessNum(direction, thisChess, x - 1, y);
                }
                break;
        }
        return num;
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
        btnUndo.setDisable(true);
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
    protected void handleAutoConnectServer(ActionEvent event) {
    	client = Client.getInstance(NetType.CLIENT2);
        client.setNetStateChangeListener(this);
        client.connectToServer("127.0.0.1");
        netType = NetType.CLIENT2;
    }

    @FXML
    protected void handleConnectClicked(ActionEvent event) {    	
        if (tfIP.getText().matches("(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)")) {
            client = Client.getInstance(NetType.CLIENT2);
            client.setNetStateChangeListener(this);
            client.connectToServer(tfIP.getText());
            netType = NetType.CLIENT2;
        }
    }

    @FXML
    protected void handleSendClicked(ActionEvent event) {
        if (tfMessage.getText().length() > 0) {
            String message = buildMessage(HEAD_MSG, tfMessage.getText());
            client.sendMessage(message);//send "message that user typed" to opponent through "socket"
        	taContent.appendText("[玩家" + clientid + "]" + tfMessage.getText() + "\n");
        }
        tfMessage.setText("");
    }

    @FXML
    protected void handleUndoClicked(ActionEvent e) {
        btnUndo.setDisable(true);
        String message = buildMessage(HEAD_UNDO, "[玩家" + clientid + "]");            
        client.sendMessage(message);
        taContent.appendText("[玩家" + clientid + "]請求悔棋！\n");
    }

    @Override
    public void onConnect(int id) {
        System.out.println("some one connected");
        server.sendMessage(buildMessage( buildMessage(HEAD_NET, BODY_OK),String.valueOf(id) ), id);
        taContent.appendText("[系統]客戶端已連接！\n");
        tfMessage.setDisable(false);
        btnSend.setDisable(false);
        taContent.appendText("[系統]主機執黑棋，先走\n");
        currentChess = Chess.BLACK;
    }

    @Override
    public void onMessage(String message) {//according to different message that received from opponent through "socket", react differently (on the local side) 
    	System.out.println(message);
        String[] msgArray = message.split(":");

        switch (msgArray[0]) {
            case HEAD_NET:
                if (msgArray[1].equals(BODY_OK)) {
                    taContent.appendText("[系統]已連接到伺服器！\n");
                    tfMessage.setDisable(false);
                    btnSend.setDisable(false);
                    tfIP.setDisable(true);
                    btnStart.setDisable(true);
                    btnConnect.setDisable(true);
                    
                    client.setClientid( Integer.valueOf(msgArray[2]) );
                    opponentid = opponentID(Integer.valueOf(msgArray[2]))+1;
                	clientid = Integer.valueOf(msgArray[2])+1;
                	taContent.appendText("[系統]您為玩家" + clientid + "\n" );
                	//separate client 1 & 2
                	if(Integer.valueOf(msgArray[2]) %2 == 0) {
                		netType = NetType.CLIENT1;
                		currentChess = Chess.BLACK;
                		taContent.appendText("[系統]玩家" + clientid + "執黑棋，請等待對手-玩家" + opponentid + "上線\n" );
                	}
                	else {
                		taContent.appendText("[系統]玩家" + clientid + "執白棋，請等待對手-玩家" + opponentid + "先走\n");
                		client.sendMessage(buildMessage(HEAD_GAME, BODY_PREPARED));
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
                taContent.appendText("[玩家" + opponentid + "]"+ msgContent.toString() + "\n");
                break;
            case HEAD_BROADCAST:
            	StringBuilder broadcastContent = new StringBuilder();
                for (int i = 1; i < msgArray.length; i++) {
                	broadcastContent.append(msgArray[i]);
                    if (i + 1 < msgArray.length) {
                    	broadcastContent.append(':');
                    }
                }
                taContent.appendText("[伺服器廣播]"+ broadcastContent.toString() + "\n");
                break;
            case HEAD_CHESS:
                btnUndo.setDisable(true);
                int x = Integer.parseInt(msgArray[1]);
                int y = Integer.parseInt(msgArray[2]);
                Position p = new Position(x, y);
                lastPostion = p;
                taContent.appendText("[玩家" + opponentid + "]走子：" + markX[x] + "," + markY[y] + "\n");
                if (netType == NetType.CLIENT1) {                    
                    drawChess(Chess.WHITE, p);
                    game[p.x][p.y] = Chess.WHITE;
                    currentChess = Chess.BLACK; 
                } else if (netType == NetType.CLIENT2) {
                	drawChess(Chess.BLACK, p);
                    game[p.x][p.y] = Chess.BLACK;
                    currentChess = Chess.WHITE;
                }
                checkWinner(p.x, p.y);
                break;
            case HEAD_GAME:
                isOtherOK = true;
                if(msgArray[1].equals(BODY_PREPARED)) {
                	client.sendMessage(buildMessage(HEAD_GAME, BODY_OK));
                	taContent.appendText("[系統]對手-玩家" + opponentid + "已上線\n" );
                }
                break;
            case HEAD_UNDO:
                if (msgArray[1].equals(BODY_OK)) {
                	taContent.appendText("[玩家" + opponentid + "]同意悔棋！\n");
                    if(netType==NetType.CLIENT1) {                    	
                        currentChess = Chess.BLACK;
                    }else if(netType==NetType.CLIENT2){
                        currentChess = Chess.WHITE;
                    }
                    removeChess();
                } else if (msgArray[1].equals(BODY_NO)) {
                	taContent.appendText("[玩家" + opponentid + "]拒絕悔棋！\n");
                } else {
                    taContent.appendText(msgArray[1] + "請求悔棋！\n");
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "對方-玩家" + opponentid + "請求悔棋，請選擇！", ButtonType.NO, ButtonType.OK);
                    alert.setOnCloseRequest(event -> {
                        if (alert.getResult().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                            String msg = buildMessage(HEAD_UNDO, BODY_OK);
                            client.sendMessage(msg);//send "undo ok response" to opponent through "socket"
                        	taContent.appendText("[玩家" + clientid + "]同意悔棋！\n");
                            if (netType == NetType.CLIENT1) {                            	
                                currentChess = Chess.WHITE;
                            } else if (netType == NetType.CLIENT2) {
                                currentChess = Chess.BLACK;
                            }
                            removeChess();
                        } else {
                            String msg = buildMessage(HEAD_UNDO, BODY_NO);
                            client.sendMessage(msg);
                        	taContent.appendText("[玩家" + clientid + "]拒絕悔棋！\n");
                        }
                    });
                    alert.show();
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
        tfIP.setDisable(true);
    }

    int opponentID(int myid) {
    	if(myid%2==0) {
    		return myid+1;
    	}
    	else {
    		return myid-1;
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