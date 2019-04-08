/*This is code written by Frederik Alexander Hounsvad
 * The use of this code in a non commercial and non exam environment is permitted
 */
package csv_to_db;

import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.stream.Stream;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 *
 * @author Pinnacle F
 */
public class FXMLDocumentController implements Initializable {

    private Connection conn = null;

    private String dbURL;
    private String dbUser;
    private String dbPassword;
    private String tableName;
    private String lastFilePath = null;

    private File file = null;

    @FXML
    private TableView table;

    private ObservableList<ObservableList> rows;
    private List<List> rowsRaw;
    private List<String> columns = null;
    private List<String> dataTypes;
    @FXML
    private AnchorPane connectionDetails;
    @FXML
    private JFXTextField url;
    @FXML
    private JFXTextField username;
    @FXML
    private JFXPasswordField password;
    @FXML
    private AnchorPane tableDetails;
    @FXML
    private JFXTextField tableNameField;
    @FXML
    private JFXTextField columnNamesField;
    @FXML
    private JFXTextField columnTypesField;
    @FXML
    private Label label;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            DriverManager.registerDriver(new org.postgresql.Driver());
            Class.forName("org.postgresql.Driver");
        } catch (Exception e) {
            label.setText(e.toString());
            System.out.println(e);
        }
        table.addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
            if (KeyCode.ESCAPE == event.getCode()) {
                ((Stage) table.getScene().getWindow()).close();
            }
        });
        try {
            File f = new File(System.getenv("APPDATA").concat("\\csvToDB\\settings.txt"));
            Scanner sc = new Scanner(f);
            while (sc.hasNextLine()) {
                String[] tokens = sc.nextLine().split("!:!");
                if (tokens.length > 1) {
                    switch (tokens[0]) {
                        case "URL":
                            this.url.setText(tokens[1]);
                            this.dbURL = tokens[1];
                            System.out.println("Initial URL: " + tokens[1]);
                            break;
                        case "Username":
                            this.username.setText(tokens[1]);
                            this.dbUser = tokens[1];
                            System.out.println("Initial Username: " + tokens[1]);
                            break;
                        case "Password":
                            this.password.setText(tokens[1]);
                            this.dbPassword = tokens[1];
                            System.out.println("Initial Password: " + tokens[1]);
                            break;
                        case "File path":
                            this.file = new File(tokens[1]);
                            System.out.println("Initial File path: " + tokens[1]);
                            lastFilePath = tokens[1];
                            break;
                        default:
                            System.out.println(tokens[1]);
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            System.out.println("file not found");
            ex.printStackTrace();
        }

    }

    @FXML
    private void open(ActionEvent event) {
        FileChooser fc = new FileChooser();
        if (file != null) {
            fc.setInitialDirectory(file.getParentFile());
        }
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Comma seperated values (*.CSV)", "*.csv"));
        file = fc.showOpenDialog(table.getScene().getWindow());
        try {
            if (file == null) {
                throw new NullPointerException("File is null");
            }
            try (Scanner lineScanner = new Scanner(file)) {
                columns = new ArrayList<>();
                rowsRaw = new ArrayList<>();
                dataTypes = new ArrayList<>();
                int lineCounter = 0;
                while (lineScanner.hasNextLine()) {
                    System.out.println("Line");
                    String[] tokens = lineScanner.nextLine().split(",");
                    switch (lineCounter) {
                        case 0:
                            System.out.println("Added table name");
                            tableName = tokens[0];
                            break;
                        case 1:
                            System.out.println("Added data type");
                            Stream.of(tokens).forEach((t) -> dataTypes.add(t));
                            break;
                        case 2:
                            System.out.println("Added column name");
                            for (String s : tokens) {
                                columns.add(s);
                            }
                            break;
                        default:
                            System.out.println("added row");
                            List<String> tempRow = new ArrayList<>();
                            Stream.of(tokens).forEach((t) -> tempRow.add(t));
                            rowsRaw.add(tempRow);
                    }
                    lineCounter++;
                }
                showData();
            } catch (FileNotFoundException ex) {
                System.exit(-1);
            }
        } catch (NullPointerException e) {
            if (e.getMessage().equals("File is null") && lastFilePath != null) {

                file = new File(lastFilePath);
            }
        }
    }

    private void showData() {
        tableNameField.setText(tableName);
        columnNamesField.setText(columns.toString().replaceAll("[^A-Za-z0-9_,]", ""));
        columnTypesField.setText(dataTypes.toString().replaceAll("[^A-Za-z0-9_,]", ""));
        System.out.println("Show data");
        rows = FXCollections.observableArrayList();
        table.getColumns().clear();
        for (int i = 0; i < columns.size(); i++) {
            System.out.println("Adding column: " + i);
            TableColumn col = new TableColumn(columns.get(i));
            final int j = i;
            col.setCellValueFactory(new Callback<CellDataFeatures<ObservableList, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(CellDataFeatures<ObservableList, String> param) {
                    return new SimpleStringProperty(param.getValue().get(j).toString());
                }
            });
            table.getColumns().addAll(col);
        }
        for (List<String> ls : rowsRaw) {
            System.out.println("Add row");
            ObservableList<String> row = FXCollections.observableArrayList();
            for (String s : ls) {
                row.add(s);
            }
            rows.add(row);
        }
        table.setItems(rows);
    }

    @FXML
    private void paste(ActionEvent event) {
        try {
            String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            Scanner lineScanner = new Scanner(data);
            columns = new ArrayList<>();
            rowsRaw = new ArrayList<>();
            dataTypes = new ArrayList<>();
            int lineCounter = 0;
            while (lineScanner.hasNextLine()) {
                System.out.println("Line");
                String[] tokens = lineScanner.nextLine().split(",");
                switch (lineCounter) {
                    case 0:
                        System.out.println("Added table name");
                        tableName = tokens[0];
                        break;
                    case 1:
                        System.out.println("Added data type");
                        Stream.of(tokens).forEach((t) -> dataTypes.add(t));
                        break;
                    case 2:
                        System.out.println("added column name");
                        for (String s : tokens) {
                            columns.add(s);
                        }
                        break;
                    default:
                        System.out.println("added row");
                        List<String> tempRow = new ArrayList<>();
                        Stream.of(tokens).forEach((t) -> tempRow.add(t));
                        rowsRaw.add(tempRow);
                }
                lineCounter++;
            }
            showData();
        } catch (IOException ex) {
            System.out.println(ex);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @FXML
    private void insert(ActionEvent event) {
        try {
            conn = DriverManager.getConnection(dbURL, dbUser, dbPassword);
            Statement stmt = conn.createStatement();
            stmt.executeQuery(buildQuery(file));
        } catch (org.postgresql.util.PSQLException e) {
            System.out.println(e);
            label.setText(e.toString());
        } catch (Exception e) {
            label.setText(e.toString());
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                label.setText(e.toString());
                System.out.println(e);
            }
        }
    }

    private String buildQuery(File file) {
        if (file == null) {
            System.exit(-1);
        }
        StringBuilder result = new StringBuilder();
        for (List<String> r : rowsRaw) {
            //List<String> r = rowsRaw.get(0);
            result.append(String.format("INSERT INTO \u005c\u0022public\u005c\u0022.\u005c\u0022%s\u005c\u0022 VALUES (", tableName.toLowerCase()));
            result.deleteCharAt(result.lastIndexOf("\ufeff"));
            for (int i = 0; i < r.size(); i++) {
                result.append("'" + valueToString(r.get(i), i) + "'").append(", ");
            }
            result.delete(result.length() - 2, result.length());
            result.append(");");
        }
        result.deleteCharAt(result.length() - 1);

        System.out.println(result.toString());
        return result.toString();
    }

    private String valueToString(String value, int index) {
        if (dataTypes.get(index).toLowerCase().contains("char")
                || dataTypes.get(index).toLowerCase().contains("String")) {
            return String.format("'%s'", value);
        } else if (dataTypes.get(index).toLowerCase().contains("int")
                || dataTypes.get(index).toLowerCase().contains("float")
                || dataTypes.get(index).toLowerCase().contains("decimal")
                || dataTypes.get(index).toLowerCase().contains("numeric")
                || dataTypes.get(index).toLowerCase().contains("money")
                || dataTypes.get(index).toLowerCase().contains("real")
                || dataTypes.get(index).toLowerCase().contains("bool")) {
            return value;
        } else {
            return value;
        }

    }

    private void checkEscape(KeyEvent event) {
        System.out.println(event.getCode());
        if (event.getCode() == KeyCode.ESCAPE) {
            System.exit(0);
        }
    }

    @FXML
    private void clear(ActionEvent event) {
        table.setItems(FXCollections.observableArrayList());
        table.getColumns().clear();
        rows = null;
        rowsRaw = null;
    }

    @FXML
    private void clickConnectionDetails(ActionEvent event) {
        connectionDetails.visibleProperty().set(true);
    }

    @FXML
    private void clickTableDetails(ActionEvent event) {
        tableDetails.visibleProperty().set(true);
    }

    private void updateFields() {
        dbURL = url.getText();
        dbUser = username.getText();
        dbPassword = password.getText();
        tableName = tableNameField.getText();
        if (columns != null) {
            columns.clear();
            columns.addAll(Arrays.asList(columnNamesField.getText().split(",")));
            showData();
        }
    }

    @FXML
    private void closePupup(MouseEvent event) {
        tableDetails.visibleProperty().set(false);
        connectionDetails.visibleProperty().set(false);
        updateFields();
    }

    public boolean saveSettings() {
        FileWriter fw = null;
        try {
            File f = new File(System.getenv("APPDATA").concat("\\csvToDB\\settings.txt"));
            f.getParentFile().mkdirs();
            fw = new FileWriter(f);
            StringBuilder sb = new StringBuilder();
            sb.append("URL!:!").append(url.getText()).append("\n");
            sb.append("Username!:!").append(username.getText()).append("\n");
            sb.append("Password!:!").append(password.getText()).append("\n");
            sb.append("File path!:!");
            if (file != null) {
                sb.append(file.getPath());
            } else {
                sb.append(lastFilePath);
            }

            fw.write(sb.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                fw.close();
            } catch (IOException ex) {
                //ex.printStackTrace();
            }
        }
        return false;
    }

}
