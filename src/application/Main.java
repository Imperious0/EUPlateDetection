package application;
	
import org.opencv.core.Core;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;

import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class Main extends Application {
	private static Stage pStage;
	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader fxmlLoader = new FXMLLoader();
		    fxmlLoader.setLocation(getClass().getClassLoader().getResource("application/Main.fxml"));

		    Parent root = fxmlLoader.load();
			Scene scene = new Scene(root);
			primaryStage.setScene(scene);
			primaryStage.setTitle("Photo Filter App");
			primaryStage.show();
			
			setPrimaryStage(primaryStage);

	        pStage = primaryStage;
	        
	     // set the proper behavior on closing the application
	     			MainController controller = fxmlLoader.getController();
	     			primaryStage.setOnCloseRequest((EventHandler<WindowEvent>) (new EventHandler<WindowEvent>() {
	     				public void handle(WindowEvent we)
	     				{
	     					controller.setClosed();
	     				}
	     			}));
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Stage getPrimaryStage() {
        return pStage;
    }

    private void setPrimaryStage(Stage pStage) {
    	Main.pStage = pStage;
    }
	
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		launch(args);
	}
}
