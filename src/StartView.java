import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Screen;

import java.io.File;
import java.net.URISyntaxException;


public class StartView {

    GridPane mainMenuArea;

    Label greetingL = new Label("Welcome to KERIS Survival!");
    Label inputL = new Label("What is your input path?");
    Label wellsL = new Label("What are the wells?*");
    Label channelsL = new Label("What are the channel tokens?");
    Label colorsL = new Label("What is the list of pseudocolors?");
    Label tpsL = new Label("What are the timepoints?*");
    Label outputL = new Label("What is your output path?");
    Label explainL = new Label("*Leave these fields blank to include all");

    CheckBox blindCheck = new CheckBox("Blinded?");

    final TextField input = new TextField();
    final TextField well_tokens = new TextField();
    final TextField channel_tokens = new TextField();
    final TextField color_tokens = new TextField();
    final TextField tps = new TextField();
    final TextField output = new TextField();

    final FileChooser chooser = new FileChooser();
    public FileChooser getFileChooser() { return chooser; }

    public Button chooseButton = new Button("Timepoint file...");
    public Button getChooseButton() { return chooseButton; }
    public Button csvButton = new Button("Load neurons from CSV file...");
    public Button getCSVButton() { return csvButton; }
    public Button optionsButton = new Button("Set own status options...");
    public Button getOptionsButton() { return optionsButton; }
    public Button imageViewButton = new Button("Done");
    public Button getImageButton() { return imageViewButton; }
    public Button clearFdsButton = new Button("Clear all fields");
    public CheckBox getBlindCheck() { return blindCheck; }
    public Button getClearFdsButton() { return clearFdsButton; }
    public String getInput() { return input.getText(); }
    public String getOutput() { return output.getText(); }
    public String getWells() { return well_tokens.getText(); }
    public String getChannels() { return channel_tokens.getText(); }
    public String getColors() { return color_tokens.getText(); }
    public String getTps() { return tps.getText(); }

    CurationGUI gui;
    ImgFileProcessing processor;

    public StartView(CurationGUI gui) {
        this.gui = gui;
    }
    public StartView(ImgFileProcessing processor) {
        this.processor = processor;
    }

    public Scene getScene() {
        final double PROGRAM_HEIGHT = 600;
        final double PROGRAM_WIDTH = 800;
        final double LAYOUT_AREA_HEIGHT = PROGRAM_HEIGHT;
        final double LAYOUT_AREA_WIDTH = (int) 0.8 * PROGRAM_WIDTH;

        /**Main menu area**/
        mainMenuArea = new GridPane();
        mainMenuArea.setPrefHeight(LAYOUT_AREA_HEIGHT);
        mainMenuArea.setPrefWidth(LAYOUT_AREA_WIDTH);
        mainMenuArea.setAlignment(Pos.CENTER);
        mainMenuArea.setHgap(5);
        mainMenuArea.setVgap(5);
        mainMenuArea.setPadding(new Insets(25, 25, 25, 25)); // set top, right, bottom, left

        /** Define text fields, buttons **/
        input.setPromptText("C:\\Users\\...    OR    /Users/...");
        input.setPrefColumnCount(14);
        input.getText();

        well_tokens.setPromptText("ex. A1-A5,B2");
        well_tokens.setPrefColumnCount(14);
        well_tokens.getText();

        channel_tokens.setPromptText("ex. RFP,DAPI");
        channel_tokens.setPrefColumnCount(14);
        channel_tokens.getText();

        color_tokens.setPrefColumnCount(14);
        color_tokens.setPromptText("ex. red,blue");
        color_tokens.getText();

        tps.setPromptText("ex. T0-T1,T3");
        tps.setPrefColumnCount(14);
        tps.getText();

        output.setPrefColumnCount(14);
        output.setPromptText("C:\\Users\\...    OR    /Users/...");
        output.getText();

        input.setText("/Users/naufaamirani/Documents/test_wells");
        well_tokens.setText("B1");
        channel_tokens.setText("RFP");
        tps.setText("T1");
        output.setText("/Users/naufaamirani/Documents/output");

        greetingL.setFont(Font.font("Dialog", FontWeight.BOLD, 14));
        explainL.setFont(Font.font("Dialog", FontWeight.LIGHT, 11));

        /**Add components to screen in specific grid conformation**/
        mainMenuArea.add(greetingL, 0, 1);
        mainMenuArea.add(csvButton, 1, 2);
        mainMenuArea.add(blindCheck,1,3);
        mainMenuArea.add(inputL, 0, 4);
        mainMenuArea.add(input, 1, 4);
        mainMenuArea.add(wellsL, 0, 5);
        mainMenuArea.add(well_tokens, 1, 5);
        mainMenuArea.add(channelsL, 0, 6);
        mainMenuArea.add(channel_tokens, 1, 6);
        mainMenuArea.add(colorsL, 0, 7);
        mainMenuArea.add(color_tokens, 1, 7);
        mainMenuArea.add(tpsL, 0, 8);
        mainMenuArea.add(tps, 1, 8);
        mainMenuArea.add(chooseButton,1,9);
        mainMenuArea.add(outputL, 0, 10);
        mainMenuArea.add(output, 1, 10);
        mainMenuArea.add(explainL,0,11);
        //mainMenuArea.add(optionsButton,1,12);
        mainMenuArea.add(imageViewButton, 1, 12);
        mainMenuArea.add(clearFdsButton, 1, 13);

        /**Set background image**/
        Image image = null;
        try {
            image = new Image(this.getClass().getResource("/neuronbkg.jpg").toURI().toString());
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        }
        // new BackgroundSize(width, height, widthAsPercentage, heightAsPercentage, contain, cover)
        BackgroundSize backgroundSize = new BackgroundSize(PROGRAM_WIDTH, PROGRAM_HEIGHT, true, true, true, true);
        // new BackgroundImage(image, repeatX, repeatY, position, size)
        BackgroundImage backgroundImage = new BackgroundImage(image, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, backgroundSize);
        // new Background(images...)
        Background background = new Background(backgroundImage);

        /**set Main Menu within bordered pane**/
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(mainMenuArea);
        borderPane.setBackground(background);

        Scene startViewScene = new Scene(borderPane, PROGRAM_WIDTH, PROGRAM_HEIGHT);

        return startViewScene;
    }
}