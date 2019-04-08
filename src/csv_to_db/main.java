/*This is code written by Frederik Alexander Hounsvad
 * The use of this code in a non commercial and non exam environment is permitted
 */
package csv_to_db;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author Pinnacle F
 */
public class main extends Application {

    FXMLLoader loader;
    Thread[] threads;

    @Override
    public void start(Stage stage) throws Exception {
        loader = new FXMLLoader(getClass().getResource("FXMLDocument.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        
        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest((event) -> {
            saveSetting();
        });
    }

    private void saveSetting() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                ((FXMLDocumentController) loader.getController()).saveSettings();
            }
        });
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
