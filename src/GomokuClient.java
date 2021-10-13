package gomoku;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GomokuClient extends Application implements EventHandler<WindowEvent>{

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		launch(args);
	}
	
	@Override //override the start method in the Application class
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("GomokuClient.fxml"));//path of fxml file
        primaryStage.setTitle("¤­¤l´Ñ");//frame name(title)
        primaryStage.setScene(new Scene(root, 740, 520));//Place the scene in the stage
        primaryStage.setResizable(false);
        primaryStage.show();//Display the stage
        primaryStage.setOnCloseRequest(this);
    }

    @Override
    public void handle(WindowEvent event) {
        try {
        	Client.getInstance(ClientController.netType).close();
        } catch (Exception e) {
            //ignore this
        }
        System.exit(0);
    }
}