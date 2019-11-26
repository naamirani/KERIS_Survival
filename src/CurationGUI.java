import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.net.URISyntaxException;
import java.util.*;

public class CurationGUI extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

    ImgView imgView;
    StartView startView;

    Scene prevImgScene;
    Scene imgViewScene;
    Scene startViewScene;

    ImgFileProcessing processor = new ImgFileProcessing();

    File selectedFile;
    String status;

    private static Stage mainStage;

    public static Stage getStage() {
        return mainStage;
    }

    public static String currentScene = "startView";

    @Override
    public void start(Stage primaryStage) {

        // store primary stage into new variable
        mainStage = primaryStage;

        // initialize startView scene
        startView = new StartView(this);
        startViewScene = startView.getScene();

        // initialize imageView scene
        imgView = new ImgView(this);


        startView.getOptionsButton().setOnAction(e-> {
            TextInputDialog dialog = new TextInputDialog("");
            dialog.setHeaderText("Alternative status options (default are Alive, Dead, Censored):");

            status = dialog.showAndWait().orElse("n/a");
        });

        // define action when "Done" button is pressed
        startView.getImageButton().setOnAction(e-> {
            // get input/output paths
            File inPath = new File(startView.getInput());
            File outPath = new File(startView.getOutput());
            if(startView.getInput().isEmpty() || startView.getOutput().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Please fill indicated fields before proceeding.", ButtonType.OK);
                alert.showAndWait();
            }
            else if(!inPath.isDirectory()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Input pathway is invalid.", ButtonType.OK);
                alert.showAndWait();
            }
            else if(!outPath.isDirectory()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Output pathway is invalid.", ButtonType.OK);
                alert.showAndWait();
            }
            // load next scene (image)
            else {
                outPath.mkdirs();

                TextInputDialog dialog = new TextInputDialog("orange");
                dialog.setHeaderText("Color for neuron markers:");
                dialog.getDialogPane().getButtonTypes().remove(ButtonType.CANCEL);

                String result = dialog.showAndWait().orElse("n/a");

                String allWells = "";
                String allTps = "";

                this.currentScene = "imageView";

                if (startView.getWells().isEmpty()) {
                    File input = new File(startView.getInput());
                    String[] directories = input.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File current, String name) {
                            return new File(current, name).isDirectory();
                        }
                    });
                    allWells = Arrays.toString(directories);
                    System.out.println(allWells);
                } else {
                    allWells = startView.getWells();
                }

                if (startView.getTps().isEmpty()) {
                    File input = new File(startView.getInput());
                    File[] list = input.listFiles();
                    String demoDir = startView.getInput() + "/" + list[0].getName();

                    if (list[0].getName().equals(".DS_Store")) {
                        demoDir = startView.getInput() + "/" + list[1].getName();
                    }

                    File demo = new File(demoDir);
                    File[] demoFiles = demo.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.endsWith(".tif");
                        }
                    });

                    Arrays.sort(demoFiles, new Comparator<File>() {
                        public int compare(File f1, File f2) {
                            try {
                                String[] first = f1.getName().split("_");
                                String[] second = f2.getName().split("_");
                                int i1 = Integer.parseInt(first[2].substring(1));
                                int i2 = Integer.parseInt(second[2].substring(1));
                                return i1 - i2;
                            } catch (NumberFormatException e) {
                                throw new AssertionError(e);
                            }
                        }
                    });

                    for (File d : demoFiles) {
                        String[] demoSegments = d.getName().split("_");
                        String demoTp = demoSegments[2];
                        allTps = demoTp;
                    }

                    System.out.println(allTps);
                    allTps = allTps.replaceAll("T", "");
                    allTps = "all," + allTps;
                } else {
                    allTps = startView.getTps();
                }

                List<String> finalTimes = processor.getFiles(startView.getInput(), startView.getOutput(),
                        allWells, startView.getChannels(), allTps);

                try {
                    imgViewScene = imgView.getScene(result, selectedFile, startView.getBlindCheck(),
                            startView.getInput(), startView.getOutput(), startView.getChannels(), startView.getColors(), startView.getCSVButton().getText(), status);

                } catch (FileNotFoundException ex) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "File input path not found.", ButtonType.OK);
                    alert.showAndWait();
                }

                mainStage.setScene(imgViewScene);
                mainStage.setX(primaryScreenBounds.getMaxX()/2);
                mainStage.setY(primaryScreenBounds.getMinY());

                Stage prevWindow = new Stage();

                try {
                    prevImgScene = imgView.getPrevScene(primaryScreenBounds.getWidth()/2,primaryScreenBounds.getHeight(), result);
                } catch (URISyntaxException ex) {
                    ex.printStackTrace();
                }

                prevWindow.setScene(prevImgScene);
                prevWindow.setTitle("Previous timepoint");
                prevWindow.setX(mainStage.getX() - mainStage.getWidth());
                prevWindow.setY(mainStage.getY());
                prevWindow.show();
            }
        });

        // define action when "Clear all fields" button is pressed
        startView.getClearFdsButton().setOnAction(e-> {
            startView.input.clear();
            startView.output.clear();
            startView.well_tokens.clear();
            startView.channel_tokens.clear();
            startView.color_tokens.clear();
            startView.tps.clear();
        });

        startView.getChooseButton().setOnAction(e-> {
            selectedFile = startView.getFileChooser().showOpenDialog(primaryStage);
            startView.getChooseButton().setText(selectedFile.getName());
        });

        startView.getCSVButton().setOnAction(e-> {
            selectedFile = startView.getFileChooser().showOpenDialog(primaryStage);
            startView.getCSVButton().setText(selectedFile.getAbsolutePath());
        });

        mainStage.setTitle("KERIS Survival (Java Curation Tool)");
        mainStage.setScene(startViewScene);
        mainStage.show();
    }
}