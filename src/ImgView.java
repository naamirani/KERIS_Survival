import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;

import javax.imageio.ImageIO;
import java.io.*;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static javafx.collections.FXCollections.shuffle;


public class ImgView {

    CurationGUI gui;
    StartView startView;
    ImgFileProcessing processor = new ImgFileProcessing();

    /**
     * set references for scene components so can be assigned in gui
     **/

    File file;
    Menu menuTitle;
    Menu neuronIndex;
    int currIndex;

    ImageView imageView;
    ImageViewPane viewPane;
    Slider sliderContrast;
    Slider sliderBrightness;
    Button layerButton;
    StackPane root;

    ImageView prevIv;

    Group test;

    String currWellName;
    List<String> currWellList;

    Timepoint currTimePoint;
    LinkedList<Timepoint> timePoints;
    ArrayList<ImageView> layers;

    HashMap<String, LinkedList<Timepoint>> wellTimepoints;

    HashMap<String, HashMap<Integer, List<String>>> neuronStatuses;

    HashMap<String, ObservableList<Integer>> neuronIDLists;
    HashMap<Integer, List<String>> currStatuses;
    HashMap<Integer, String> layerNames;

    Boolean customStatuses;
    ObservableList<Integer> currNeuronIdList;
    String[] statusOps;
    ObservableList<String> statusOptions = FXCollections.observableArrayList();
    ListView<Integer> neuronListView;
    ListView<String> statusListView;

    HashMap<String, String> blindMap = processor.getBlindMap();
    Map<String, List<String>> map;
    int channelSize;

    Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

    public ImgView(CurationGUI gui) {
        this.gui = gui;
        this.imageView = new ImageView();
        this.prevIv = new ImageView();
        this.root = new StackPane();
    }

    /************************************/

    public Scene getScene(String result, File select, CheckBox check,
                          String inputDir, String outputDir, String chnls, String clrs, String csvPath, String status) throws FileNotFoundException {
        CurationGUI.getStage().setOnCloseRequest(event -> {
            event.consume();
        });

        final double PROGRAM_HEIGHT = primaryScreenBounds.getHeight();
        final double PROGRAM_WIDTH = primaryScreenBounds.getWidth()/2;

        List<String> colors = Arrays.asList(clrs.split(","));
        List<String> channels = Arrays.asList(chnls.split(","));

        channelSize = channels.size();

        statusOps = new String[]{"Alive", "Dead", "Censored"};

        System.out.println(status);
        if (status == null) {
            System.out.println("got is empty");
            for (String stat : statusOps) {
                statusOptions.add(stat);
            }
            customStatuses = false;
        }
        else {
            System.out.println("got other");
            for (String stat: status.split(",")) {
                statusOptions.add(stat);
            }
            customStatuses = true;
        }

        layers = new ArrayList<ImageView>();
        layerNames = new HashMap<Integer, String>();

        Button clearTpButton = new Button("Undo neuron");
        Button homeButton = new Button("Reset to T0");

        Label inst1 = new Label("Click to mark neuron");
        Label inst2 = new Label("Click and drag to pan image");
        Label dirD = new Label("Navigate right: D");
        Label dirA = new Label("Navigate left: A");

        MenuBar menuBar1 = new MenuBar();
        MenuBar menuBar2 = new MenuBar();
        Menu menuWell = new Menu("Wells");
        neuronIndex = new Menu("Next neuron #: 1");
        menuTitle = new Menu("");
        map = processor.getWellSets(check, outputDir);
        if(colors.size()>1) {
            processor.setColors(map, colors, channels);
        }
        wellTimepoints = new HashMap<>();
        //Create neuron status array for each well
        neuronStatuses = new HashMap<String, HashMap<Integer, List<String>>>();
        neuronIDLists = new HashMap<String, ObservableList<Integer>>();
        for (String wellName : map.keySet()) {
            HashMap<Integer, List<String>> NeuronsStatus = new HashMap<Integer, List<String>>();
            ObservableList<Integer> NeuronIDList = FXCollections.observableArrayList();
            neuronStatuses.put(wellName, NeuronsStatus);
            neuronIDLists.put(wellName, NeuronIDList);
            List<String>tempWellList = map.get(wellName);
            LinkedList<Timepoint> tempTimePoints = new LinkedList<Timepoint>();
            for (int i = 0; i < tempWellList.size()/channelSize; i++) {
                String tpName = getTimepointName(tempWellList.get(i));
                tempTimePoints.add(new Timepoint(tpName));
            }
            wellTimepoints.put(wellName, tempTimePoints);
        }
        if (!csvPath.equals("Load neurons from CSV file...")) {
            readFromCSV(csvPath, map, result);
        }

        //Show first well from directory
        Iterator<String> firstItr = map.keySet().iterator();
        currWellName = firstItr.next();
        currWellList = map.get(currWellName);
        currIndex = 0;
        currStatuses = neuronStatuses.get(currWellName);
        currNeuronIdList = neuronIDLists.get(currWellName);
        neuronListView = new ListView<>(currNeuronIdList);
        statusListView = new ListView<>(statusOptions);
        if(customStatuses){
            statusListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        }
        //If
        timePoints = wellTimepoints.get(currWellName);
        currTimePoint = timePoints.get(currIndex);

        String fn = currWellList.get(currIndex);
        file = new File(fn);
        if (check.isSelected() == false) {
            if (isWindows()) {
                menuTitle = new Menu("            " + fn.substring(fn.lastIndexOf("\\") + 1, fn.lastIndexOf(".")));
            } else {
                menuTitle = new Menu("            " + fn.substring(fn.lastIndexOf("/") + 1, fn.lastIndexOf(".")));
            }
        }
        else {
            String segments[] = fn.split("_");
            menuTitle = new Menu("            " + segments[2]);
        }

        layerButton = new Button(fn.split("_")[6]);

        Image image = new Image(file.toURI().toString());
//        imageView.setImage(image);
        //updateColor(channels, colors);

        neuronIndex.setText("Next Neuron #: " + Integer.toString(currTimePoint.currLabelIndex));

        Button printToCsv = new Button("Print tracked neurons to CSV");
        Button exit = new Button("Exit");

        EventHandler<ActionEvent> wellItemPressEvent = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                // Remove current timepoint labels and circles
                viewPane.getChildren().remove(currTimePoint.getMarkersGroup());
                viewPane.getChildren().remove(currTimePoint.getLabelsGroup());
                // Get new well
                MenuItem currMenuItem = (MenuItem) event.getSource();
                // Redefine current wellList and current time point, show first time point and first image
                if (check.isSelected() == true) {
                    currWellName = getKeyByValue(blindMap, currMenuItem.getText());
                }
                else {
                    currWellName = currMenuItem.getText();
                }
                currWellList = map.get(currWellName);
//                images = getCurrWellList(map, channels);
                currIndex = 0;
                currNeuronIdList = neuronIDLists.get(currWellName);
                neuronListView.setItems(currNeuronIdList);
                currStatuses = neuronStatuses.get(currWellName);

                timePoints = wellTimepoints.get(currWellName);
                currTimePoint = timePoints.get(currIndex);

                String fileName = currWellList.get(currIndex);
                //file = new File(fileName);
                if (check.isSelected() == false) {
                    if (isWindows()) {
                        menuTitle.setText("            " + fileName.substring(fileName.lastIndexOf("\\") + 1, fileName.lastIndexOf(".")));
                    } else {
                        menuTitle.setText("            " + fileName.substring(fileName.lastIndexOf("/") + 1, fileName.lastIndexOf(".")));
                    }
                }
                else {
                    String segments[] = fileName.split("_");
                    menuTitle.setText("            " + segments[2]);
                }

                //Image image = new Image(file.toURI().toString());
//                imageView.setImage(image);
                layers = new ArrayList<ImageView>();
                viewPane.getChildren().remove(test);
                test = getCurrTpList(map,channels,colors,outputDir);
                viewPane.getChildren().add(test);
                updateColor(channels, colors);
                if (!viewPane.getChildren().contains(currTimePoint.getMarkersGroup())) {
                    viewPane.getChildren().addAll(currTimePoint.getMarkersGroup());
                    viewPane.getChildren().addAll(currTimePoint.getLabelsGroup());
                }
                neuronIndex.setText("Next Neuron #: " + Integer.toString(currTimePoint.currLabelIndex));
            }
        };

        EventHandler<ActionEvent> printCSVEvent = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (customStatuses) {
                    printCSVCustom(select, outputDir);
                }
                else {
                    printCSV(select, outputDir);}
            }
        };

        EventHandler<ActionEvent> exitEvent = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.getButtonTypes().remove(ButtonType.OK);
                alert.getButtonTypes().add(ButtonType.CANCEL);
                alert.getButtonTypes().add(ButtonType.YES);
                alert.setTitle("Java Curation Tool");
                alert.setContentText(String.format("Quit application?"));
                Optional<ButtonType> res = alert.showAndWait();

                if (res.isPresent()) {
                    if (res.get().equals(ButtonType.YES)) {
                        if (timePoints.indexOf(currTimePoint) == timePoints.size()-1) {
                            WritableImage snap = root.snapshot(new SnapshotParameters(), null);
                            String ssPath;
                            if (isWindows()) {
                                ssPath = inputDir + "\\screenshot-" + currWellName + "-" + "T" + (currTimePoint.tpIndex)  + ".png";
                            } else {
                                ssPath = inputDir + "/screenshot-" + currWellName + "-" + "T" + (currTimePoint.tpIndex)  + ".png";
                            }
                            File ss = new File(ssPath);
                            try {
                                ImageIO.write(SwingFXUtils.fromFXImage(snap, null), "png", ss);
                            } catch (IOException error) {
                                error.printStackTrace();
                            }
                        }

                        if (customStatuses) {
                            printCSVCustom(select, outputDir);
                        }
                        else {
                            printCSV(select, outputDir);
                        }
                        //Clear output dir of previous PNG files
                        //Delete all old PNG files from output dir
                        File tempOutputDir = new File(outputDir);
                        File[] listFilesO = tempOutputDir.listFiles();
                        for(File pngFile : listFilesO){
                            if(pngFile.getName().contains(".png")) {
                                pngFile.delete();
                            }
                        }
                        //prompt for keeping screenshot of which tp
                        TextInputDialog dialog = new TextInputDialog("T0");
                        dialog.setHeaderText("Timepoint screenshot(s) to keep:");
                        dialog.getDialogPane().getButtonTypes().remove(ButtonType.CANCEL);

                        String result = dialog.showAndWait().orElse("n/a");

                        if (result.equals("n/a")) {
                            Platform.exit();
                        }
                        else {

                            File tempInputDir = new File(inputDir);
                            File[] listFilesI = tempInputDir.listFiles();
                            List<String> finalTpsKeep = new ArrayList<>();
                            List<String> timepointsKeep = Arrays.asList(result.split(","));
                            for (String time : timepointsKeep) {
                                time = time.replaceAll("T", "");
                                if (time.contains("-")) {
                                    String[] timeChunks = time.split("-");
                                    for (int i = Integer.parseInt(timeChunks[0]); i <= Integer.parseInt(timeChunks[1]); i++) {
                                        finalTpsKeep.add(Integer.toString(i));
                                    }
                                } else {
                                    finalTpsKeep.add(time);
                                }
                            }
                            for (File shotFile : listFilesI) {
                                if (finalTpsKeep.parallelStream().anyMatch(shotFile.getName()::contains) == false && shotFile.getName().contains("screenshot")) {
                                    shotFile.delete();
                                }
                            }
                            Platform.exit();
                        }
                    }
                    else if (res.get().equals(ButtonType.CANCEL)) {
                        alert.close();
                    }
                }
            }
        };

        ClickPanHandler labelNeuronPressEvent = new ClickPanHandler(
                e -> System.out.println("Dragged"),
                e -> {
                    if (e.getButton() == MouseButton.PRIMARY) {
                        if (customStatuses || (!customStatuses && (!currStatuses.containsKey(currTimePoint.currLabelIndex) || currStatuses.get(currTimePoint.currLabelIndex).get(0).equals("Alive")))) {
                            System.out.println("[" + e.getX() + ", " + e.getY() + "]");
                            Neuron neuron = new Neuron(e.getX(), e.getY(), result);
                            //Add neuron object to the current timepoint
                            // IF PROVIDE CSV:
                            // extract second column, times each LAST timepoint w corresponding number
                            // ELSE
                            neuron.setLastTp(Integer.toString(timePoints.indexOf(currTimePoint)));
                            System.out.println(neuron.getLastTp());
                            if(!customStatuses) {
                                List<String> Status =  new ArrayList<String>();
                                Status.add("Alive");
                                currStatuses.put(currTimePoint.currLabelIndex, Status);
                            }
                            else{
                                if(!currStatuses.containsKey(currTimePoint.currLabelIndex)) {
                                    List<String> Status = new ArrayList<String>();
                                    currStatuses.put(currTimePoint.currLabelIndex, Status);
                                }
                            }
                            viewPane.getChildren().remove(currTimePoint.getMarkersGroup());
                            viewPane.getChildren().remove(currTimePoint.getLabelsGroup());
                            currTimePoint.add(neuron, currStatuses, currTimePoint.currLabelIndex, result,customStatuses);
                            //Check if the current timepoint is being displayed already
                            if (!viewPane.getChildren().contains(currTimePoint.getMarkersGroup())) {
                                viewPane.getChildren().addAll(currTimePoint.getMarkersGroup());
                                viewPane.getChildren().addAll(currTimePoint.getLabelsGroup());
                            }

                            //generate listview for neurons for censor
                            if (!neuronListView.getItems().contains(Integer.parseInt(neuron.getLabel()))) {
                                currNeuronIdList.add(Integer.parseInt(neuron.getLabel()));
                                if(!customStatuses) {
                                    statusListView.getSelectionModel().select("Alive");
                                }

                            }
                            neuronListView.getSelectionModel().selectNext();
                            neuronIndex.setText("Next Neuron #: " + Integer.toString(currTimePoint.currLabelIndex));
                        } else {
                            Alert alert = new Alert(Alert.AlertType.WARNING, "You can't place a neuron that is currently dead or censored! Please change status first.", ButtonType.OK);
                            alert.showAndWait();
                        }
                    }
                });

        EventHandler<ActionEvent> clearTpsEvent = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
//                currTimePoint.removeAll();
                viewPane.getChildren().remove(currTimePoint.getMarkersGroup());
                viewPane.getChildren().remove(currTimePoint.getLabelsGroup());
                currTimePoint.removeNeuron(currTimePoint.currLabelIndex-1, result);
                //Check if the current timepoint is being displayed already
                if (!viewPane.getChildren().contains(currTimePoint.getMarkersGroup())) {
                    viewPane.getChildren().addAll(currTimePoint.getMarkersGroup());
                    viewPane.getChildren().addAll(currTimePoint.getLabelsGroup());
                }
                currTimePoint.currLabelIndex -= 1;
                //HashMap<Integer, String> NeuronsStatus = new HashMap<Integer, String>();
                //ObservableList<Integer> NeuronIDList = FXCollections.observableArrayList();
                //neuronStatuses.put(currWellName, NeuronsStatus);
                //neuronIDLists.put(currWellName, NeuronIDList);
                //neuronListView.setItems(NeuronIDList);
            }
        };

        EventHandler<ActionEvent> homePressEvent = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                //take screenshot of root pane before moving on
                //save to original TIF folder per well (as PNG)
                WritableImage snap = root.snapshot(new SnapshotParameters(), null);
                String ssPath;
                if (isWindows()) {
                    ssPath = inputDir + "\\screenshot-" + currWellName + "-" + "T" + (currTimePoint.tpIndex)  + ".png";
                } else {
                    ssPath = inputDir + "/screenshot-" + currWellName + "-" + "T" + (currTimePoint.tpIndex)  + ".png";
                }
                File ss = new File(ssPath);
                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(snap, null), "png", ss);
                } catch (IOException error) {
                    error.printStackTrace();
                }

                Timepoint oldTimePoint = timePoints.get(currIndex);
                currIndex = 0;
                currTimePoint = timePoints.get(currIndex);

                viewPane.getChildren().remove(oldTimePoint.getMarkersGroup());
                viewPane.getChildren().remove(oldTimePoint.getLabelsGroup());

                String fileName = currWellList.get(currIndex);
                //file = new File(fileName);

                //Image firstImage = new Image(file.toURI().toString());

                if (check.isSelected() == false) {
                    if (isWindows()) {
                        menuTitle.setText("            " + fileName.substring(fileName.lastIndexOf("\\") + 1, fileName.lastIndexOf(".")));
                    } else {
                        menuTitle.setText("            " + fileName.substring(fileName.lastIndexOf("/") + 1, fileName.lastIndexOf(".")));
                    }
                }
                else {
                    String segments[] = fileName.split("_");
                    menuTitle.setText("            " + segments[2]);
                }

//                imageView.setImage(firstImage);
                viewPane.getChildren().remove(test);
                test = getCurrTpList(map,channels,colors,outputDir);
                viewPane.getChildren().add(test);
                updateColor(channels, colors);
                if (!viewPane.getChildren().contains(currTimePoint.getMarkersGroup())) {
                    viewPane.getChildren().addAll(currTimePoint.getMarkersGroup());
                    viewPane.getChildren().addAll(currTimePoint.getLabelsGroup());
                }
                neuronIndex.setText("Next Neuron #: " + Integer.toString(currTimePoint.currLabelIndex));
            }
        };

        EventHandler<ActionEvent> mainMenuEvent = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                startView = new StartView(gui);
                CurationGUI.getStage().setScene(startView.getScene());
            }
        };

        EventHandler<ActionEvent> layerPressEvent = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int currLayer = getKeyByValue(layerNames, layerButton.getText());
                if(currLayer==channelSize){
                    System.out.println("Reset: " + layerNames.get(1));
                    layerButton.setText(layerNames.get(1));
                }
                else {
                    layerButton.setText(layerNames.get(currLayer + 1));
                }
            }
        };

        EventHandler<MouseEvent> updateListView = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Integer selectedNeuron = neuronListView.getSelectionModel().getSelectedItem();
                List<String> selectedStatus = currStatuses.get(selectedNeuron);
                if (selectedStatus != null) {
                    System.out.println("current status: " + selectedStatus);
                    // curr censor as continuous property of neuron w the given number! pass on
                    statusListView.getSelectionModel().clearSelection();
                    if(!customStatuses) {
                        statusListView.getSelectionModel().select(selectedStatus.get(0));
                    }
                    else{
                        if(selectedStatus.size() >0) {
                            int[] indexes = new int[selectedStatus.size()];
                            int i = 0;
                            for(String status:selectedStatus){
                                indexes[i] = statusOptions.indexOf(status);
                                i++;
                            }
                            statusListView.getSelectionModel().selectIndices(-1,indexes);
                        }
                    }
                    currTimePoint.setCurrLabelIndex(selectedNeuron);
                    neuronIndex.setText("Next Neuron #: " + Integer.toString(currTimePoint.currLabelIndex));
                }
            }
        };

        EventHandler<MouseEvent> updateStatusListView = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Integer selectedNeuron = neuronListView.getSelectionModel().getSelectedItem();
                List<String> selectedStatus = new ArrayList<String>();
                for(String status:statusListView.getSelectionModel().getSelectedItems()){
                    selectedStatus.add(status);
                }
                selectedStatus = selectedStatus;
                if (selectedStatus != null) {
                    currStatuses.put(selectedNeuron, selectedStatus);
                    //currTimePoint.getNeurons().get(selectedNeuron).status = selectedStatus;
                    System.out.println("Neuron #" + selectedNeuron + " current status: " + selectedStatus);
                    // curr censor as continuous property of neuron w the given number! pass on
                    if(!customStatuses) {
                        statusListView.getSelectionModel().select(selectedStatus.get(0));
                    }
                    else{
                        for(String status:selectedStatus){
                            statusListView.getSelectionModel().select(status);
                        }
                    }

                    //Remove the neuron from the view if its not alive anymore
                    System.out.println(selectedStatus);
                    if (!customStatuses && (selectedStatus.get(0).equals("Dead") || selectedStatus.get(0).equals("Censored"))) {
                        // SET NEURON LAST TP
                        currTimePoint.getNeurons().get(selectedNeuron).setLastTp(Integer.toString(timePoints.indexOf(currTimePoint)));
                        viewPane.getChildren().remove(currTimePoint.getMarkersGroup());
                        viewPane.getChildren().remove(currTimePoint.getLabelsGroup());
                        currTimePoint.removeNeuron(selectedNeuron, result);
                        //Check if the current timepoint is being displayed already
                        if (!viewPane.getChildren().contains(currTimePoint.getMarkersGroup())) {
                            viewPane.getChildren().addAll(currTimePoint.getMarkersGroup());
                            viewPane.getChildren().addAll(currTimePoint.getLabelsGroup());
                        }
                    }
                }
            }
        };

//        ImageView testIv = new ImageView();
        viewPane = new ImageViewPane(this.imageView);
//        viewPane = new ImageViewPane(testIv);

        clearTpButton.setOnAction(clearTpsEvent);
        homeButton.setOnAction(homePressEvent);
        layerButton.setOnAction(layerPressEvent);
        neuronListView.setOnMouseClicked(updateListView);

        statusListView.setOnMouseClicked(updateStatusListView);

        test = getCurrTpList(map,channels,colors,outputDir);

        viewPane.getChildren().add(test);

        VBox vbox = new VBox();
        root = new StackPane();
        root.getChildren().addAll(viewPane);
//        menuBar.prefWidthProperty().bind(root.widthProperty());

        VBox.setVgrow(root, Priority.ALWAYS);

        Iterator<String> itr = map.keySet().iterator();
        while (itr.hasNext()) {
            MenuItem chooseWell;
            if (check.isSelected() == true) {
                chooseWell = new MenuItem(blindMap.get(itr.next()));
            }
            else {
                chooseWell = new MenuItem(itr.next());
            }
            menuWell.getItems().add(chooseWell);
            chooseWell.setOnAction(wellItemPressEvent);
        }

        if (check.isSelected() == true) {
            shuffle(menuWell.getItems());
        }

        printToCsv.setOnAction(printCSVEvent);
        exit.setOnAction(printCSVEvent);
        exit.setOnAction(exitEvent);

        HBox menus = new HBox();

        menuBar1.getMenus().addAll(menuWell, neuronIndex);
        menuBar2.getMenus().addAll(menuTitle);
        menuBar1.setVisible(true);
        menuBar2.setVisible(true);

        Region spacer = new Region();
        spacer.getStyleClass().add("menu-bar");
        HBox.setHgrow(spacer, Priority.SOMETIMES);

        menus.getChildren().addAll(menuBar1, spacer, menuBar2);

        sliderContrast = new Slider(0, 1, 0);
        sliderBrightness = new Slider(0, 1, 0);
//        final Label brightnessCaption = new Label("B");
//        final Label contrastCaption = new Label("C");

        sliderBrightness.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                                Number old_val, Number new_val) {
                updateColor(channels, colors);
            }
        });
        sliderContrast.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                                Number old_val, Number new_val) {
                updateColor(channels, colors);
            }
        });

        GridPane selectionGrid = new GridPane();
        selectionGrid.setPadding(new Insets(3));
        selectionGrid.setHgap(5);
        selectionGrid.setVgap(5);
        ColumnConstraints column1 = new ColumnConstraints(50, 50, Double.MAX_VALUE);
        ColumnConstraints column2 = new ColumnConstraints(100, 100, Double.MAX_VALUE);
        selectionGrid.getColumnConstraints().addAll(column1, column2);
        selectionGrid.add(neuronListView, 0, 1);
        selectionGrid.add(statusListView, 1, 1);

        // Create panel
        Parent zoomPane = createZoomPane(root);

        VBox vbox1 = new VBox();
        vbox1.setPadding(new Insets(10, 10, 10, 10));
        vbox1.setSpacing(10);
        vbox1.getChildren().addAll(inst1, inst2, dirD, dirA);

        VBox vbox2 = new VBox();
        vbox2.setPadding(new Insets(10, 10, 10, 10));
        vbox2.setSpacing(10);
        vbox2.getChildren().addAll(layerButton, homeButton, clearTpButton);

        VBox vbox3 = new VBox();
        vbox3.setPadding(new Insets(10, 10, 10, 10));
        vbox3.setSpacing(10);
        vbox3.getChildren().addAll(printToCsv, exit);

        HBox hbox = new HBox();
        hbox.getChildren().addAll(vbox1, vbox2, selectionGrid, vbox3);
        hbox.setPadding(new Insets(10, 10, 10, 10));
        hbox.setSpacing(10);
        hbox.setAlignment(Pos.CENTER);
        vbox.getChildren().addAll(menus, zoomPane, sliderContrast, sliderBrightness, hbox);

        viewPane.addEventHandler(MouseEvent.ANY, labelNeuronPressEvent);

        // Create operator
        ZoomOperator zoomOperator = new ZoomOperator();
        zoomPane.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                double zoomFactor = 1.5;
                if (event.getDeltaY() <= 0) {
                    // zoom out
                    zoomFactor = 1 / zoomFactor;
                }
                zoomOperator.zoom(zoomPane, zoomFactor, event.getSceneX(), event.getSceneY());
            }
        });

        Scene imgViewScene = new Scene(vbox, PROGRAM_WIDTH, PROGRAM_HEIGHT);

        imgViewScene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.D) {
                if ((currIndex+1)*channelSize  < currWellList.size()) {
                    //take screenshot of root pane before moving on
                    //save to original TIF folder per well (as PNG)
                    WritableImage snap = root.snapshot(new SnapshotParameters(), null);
                    String ssPath;
                    if (isWindows()) {
                        ssPath = inputDir + "\\screenshot-" + currWellName + "-" +  "T" + (currTimePoint.tpIndex) + ".png";
                    } else {
                        ssPath = inputDir + "/screenshot-" + currWellName + "-" + "T" + (currTimePoint.tpIndex) + ".png";
                    }
                    File ss = new File(ssPath);
                    try {
                        ImageIO.write(SwingFXUtils.fromFXImage(snap, null), "png", ss);
                    } catch (IOException error) {
                        error.printStackTrace();
                    }

                    Timepoint oldTimePoint = timePoints.get(currIndex);
                    currTimePoint = timePoints.get(currIndex + 1);
                    viewPane.getChildren().remove(oldTimePoint.getMarkersGroup());
                    viewPane.getChildren().remove(oldTimePoint.getLabelsGroup());

                    String fileName = currWellList.get((currIndex+1)*channelSize);
                    //file = new File(fileName);

                    //Image forwardImage = new Image(file.toURI().toString());
                    if (check.isSelected() == false) {
                        if (isWindows()) {
                            menuTitle.setText("            " + fileName.substring(fileName.lastIndexOf("\\") + 1, fileName.lastIndexOf(".")));
                        } else {
                            menuTitle.setText("            " + fileName.substring(fileName.lastIndexOf("/") + 1, fileName.lastIndexOf(".")));
                        }
                    }
                    else {
                        String segments[] = fileName.split("_");
                        menuTitle.setText("            " + segments[2]);
                    }

                    //imageView.setImage(forwardImage);
                    viewPane.getChildren().remove(test);
                    test = getCurrTpList(map,channels,colors,outputDir);
                    viewPane.getChildren().add(test);
                    updateColor(channels, colors);
                    viewPane.getChildren().addAll(currTimePoint.getMarkersGroup());
                    viewPane.getChildren().addAll(currTimePoint.getLabelsGroup());
                    currIndex = currIndex + 1;
                    neuronIndex.setText("Next Neuron #: " + Integer.toString(currTimePoint.currLabelIndex));

                    if (timePoints.indexOf(currTimePoint) > 0) {
                        String prevN;
                        if (isWindows()) {
                            prevN = inputDir + "\\screenshot-" + currWellName + "-" + "T" + (currTimePoint.tpIndex-1) + ".png";
                        } else {
                            prevN = inputDir + "/screenshot-" + currWellName + "-" + "T" + (currTimePoint.tpIndex-1) + ".png";
                        }
                        Image prevImage = new Image(new File(prevN).toURI().toString());
                        prevIv.setImage(prevImage);
                    }
                    else {
                        String tempPath;
                        if (result.equals("boi")) { tempPath = "\\neuronbkg2.jpg"; }
                        else {
                            tempPath = "/neuronbkg.jpg";
                        }
                        Image tempImage = null;
                        try {
                            tempImage = new Image(
                                    ImgView.class.getResource(tempPath).toURI().toString());
                        } catch (URISyntaxException ex) {
                            ex.printStackTrace();
                        }
                        prevIv.setImage(tempImage);
                    }
                }
            } else if (e.getCode() == KeyCode.A) {
                if ((currIndex-1)*channelSize >= 0) {

                    Timepoint oldTimePoint = timePoints.get(currIndex);
                    currTimePoint = timePoints.get(currIndex - 1);
                    viewPane.getChildren().remove(oldTimePoint.getMarkersGroup());
                    viewPane.getChildren().remove(oldTimePoint.getLabelsGroup());

                    String fileName = currWellList.get((currIndex-1)*channelSize);
                    //file = new File(fileName);

                    //Image backImage = new Image(file.toURI().toString());
                    if (check.isSelected() == false) {
                        if (isWindows()) {
                            menuTitle.setText("            " + fileName.substring(fileName.lastIndexOf("\\") + 1, fileName.lastIndexOf(".")));
                        } else {
                            menuTitle.setText("            " + fileName.substring(fileName.lastIndexOf("/") + 1, fileName.lastIndexOf(".")));
                        }
                    }
                    else {
                        String segments[] = fileName.split("_");
                        menuTitle.setText("            " + segments[2]);
                    }

                    //imageView.setImage(backImage);
                    viewPane.getChildren().remove(test);
                    test = getCurrTpList(map,channels,colors,outputDir);
                    viewPane.getChildren().add(test);
                    updateColor(channels, colors);
                    viewPane.getChildren().addAll(currTimePoint.getMarkersGroup());
                    viewPane.getChildren().addAll(currTimePoint.getLabelsGroup());
                    currIndex = currIndex - 1;
                    neuronIndex.setText("Next Neuron #: " + Integer.toString(currTimePoint.currLabelIndex));

                    if (timePoints.indexOf(currTimePoint) > 0) {
                        String prevN;
                        if (isWindows()) {
                            prevN = inputDir + "\\screenshot-" + currWellName + "-T" + (currTimePoint.tpIndex+1) + ".png";
                        }
                        else {
                            prevN = inputDir + "/screenshot-" + currWellName + "-T" + (currTimePoint.tpIndex+1) + ".png";
                        }
                        Image prevImage = new Image(new File(prevN).toURI().toString());
                        prevIv.setImage(prevImage);
                    }
                    else {
                        String tempPath;
                        if (result.equals("boi")) { tempPath = "\\neuronbkg2.jpg"; }
                        else {
                            tempPath = "/neuronbkg.jpg";
                        }
                        Image tempImage = null;
                        try {
                            tempImage = new Image(
                                    ImgView.class.getResource(tempPath).toURI().toString());
                        } catch (URISyntaxException ex) {
                            ex.printStackTrace();
                        }
                        prevIv.setImage(tempImage);
                    }
                }
            }
        });
        viewPane.getChildren().addAll(currTimePoint.getMarkersGroup());
        viewPane.getChildren().addAll(currTimePoint.getLabelsGroup());

        return imgViewScene;
    }

    public String getTimepointName(String name){
        String segments[] = name.split("_");
        return segments[2];
    }

    private boolean isWindows() {
        String OS = System.getProperty("os.name").toLowerCase();
        return OS.contains("win");
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Group getCurrTpList(Map<String, List<String>> map, List<String> chnls, List<String> clrs,String outputDir) {
        layers = new ArrayList<ImageView>();
        Group layerGroup = new Group();
        //layerGroup.setBlendMode(BlendMode.OVERLAY);
        currWellList = map.get(currWellName);
        if (chnls.size()==1) {
            //...generate group of images
            int i = 1;
            for (String l : currWellList) {
                if (chnls.parallelStream().anyMatch(l::contains)==true && l.contains(currTimePoint.timepointNum)) {
                    File f;
                    if(isWindows()) {
                        f = new File(outputDir + "\\" + l);
                    }
                    else {
                        f = new File(outputDir + "/" + l);
                    }
                    System.out.println("LAYER:" + l);
                    System.out.println(l.split("_")[6]);
                    layerNames.put(i, l.split("_")[6]);
                    Image layer = new Image(f.toURI().toString());
                    ImageView imageView = new ImageView(layer);
                    //imageView.setBlendMode(BlendMode.OVERLAY);
                    //imageView.setOpacity(1);
                    layerGroup.getChildren().add(imageView);
                    layers.add(imageView);
                    i++;
                }
            }
            updateColor(chnls, clrs);
        }
        else {
            int i = 0;
            for (String l : currWellList) {
                if (chnls.parallelStream().anyMatch(l::contains)==true && l.contains(currTimePoint.timepointNum)) {
                    System.out.println(i);
                    File f = new File(outputDir+"\\"+l);
                    System.out.println("LAYER:" + l);
                    System.out.println(l.split("_")[6]);
                    layerNames.put(i+1, l.split("_")[6]);
                    Image layer = new Image(f.toURI().toString());
                    ImageView imageView = new ImageView(layer);
                    imageView.setBlendMode(BlendMode.MULTIPLY);
                    layers.add(imageView);
                    //imageView.setOpacity(1);
                    layerGroup.getChildren().add(imageView);
                    i++;
                }
            }
            updateColor(chnls, clrs);
        }
        return layerGroup;
    }

    public void updateColor(List<String> chnls, List<String> clrs) {

        ColorAdjust colorAdjust = new ColorAdjust();
        String segments[] = file.toString().split("_");
        String channel[] = segments[6].split("-");
        String chnSeg = "";
        for (String chn : channel) {
            if (chnls.contains(chn)) {
                    chnSeg = chn;
            }
        }
        int indexChn = chnls.indexOf(chnSeg);

        if(sliderContrast != null) {
            Double valueContrast = sliderContrast.valueProperty().doubleValue();
            Double valueBrightness = sliderBrightness.valueProperty().doubleValue();
            colorAdjust.setContrast(valueContrast);
            colorAdjust.setBrightness(valueBrightness);
        }

        //Only adjust hue if theres only one channel
        if (chnls.size()==1) {
            if (!(clrs.get(0).equals(""))) {
                Color c = Color.web(clrs.get(indexChn));
                colorAdjust.setHue(c.getHue());
                colorAdjust.setSaturation(1);
            }
        }
        int currLayer = 0;
        if(layerButton != null){
            currLayer = getKeyByValue(layerNames, layerButton.getText())-1;
        }
        layers.get(currLayer).setEffect(colorAdjust);
    }

    private List<String> getRecordFromLine(String line) {
        List<String> values = new ArrayList<String>();
        try (Scanner rowScanner = new Scanner(line)) {
            rowScanner.useDelimiter(",");
            while (rowScanner.hasNext()) {
                values.add(rowScanner.next());
            }
        }
        return values;
    }

    public void printCSV(File select, String outputDir){
        int rowIndex = 0;
        HashMap<Integer, ArrayList<String>> rows = new HashMap<Integer, ArrayList<String>>();
        int tempIndex = 0;
        for(String tempWellName : map.keySet()) {
            HashMap<Integer,List<String>> tempNeuronStatuses = neuronStatuses.get(tempWellName);
            int numNeurons = tempNeuronStatuses.keySet().size();
            for (int i = tempIndex; i < tempIndex+numNeurons; i++) {
                ArrayList<String> row = new ArrayList<String>();
                row.add(tempWellName);
                row.add(Integer.toString(i-tempIndex+1));
                row.add("0");
                row.add("[]");
                row.add("[]");
                row.add("");
                row.add("1");
                rows.put(i, row);
            }
            tempIndex = tempIndex + numNeurons;
        }
        for(String wellName : map.keySet()){
            HashMap<Integer,List<String>> currNeuronStatuses = neuronStatuses.get(wellName);
            //iterate through each timepoint, adding the coordinates for each neuron
            LinkedList<Timepoint> tempTimePoints = wellTimepoints.get(wellName);
            for (int i = 0; i < tempTimePoints.size(); i++) {
                HashMap<Integer, Neuron> neurons = tempTimePoints.get(i).getNeurons();
                for (Integer index : currNeuronStatuses.keySet()) {
                    if(neurons.containsKey(index)) {
                        ArrayList<String> newRow = new ArrayList<String>();
                        ArrayList<String> oldRow = rows.get(rowIndex+index-1);
                        newRow.add(oldRow.get(0));
                        newRow.add(oldRow.get(1));

                        String x = oldRow.get(3);
                        String y = oldRow.get(4);

                        Neuron currNeuron = neurons.get(index);
                        String currX = Double.toString(currNeuron.getXCoordinate());
                        String currY = Double.toString(currNeuron.getYCoordinate());
                        x = x.replace("]", "\\" + currX + "]");
                        y = y.replace("]", "\\" + currY + "]");
                        if(select != null) {
                            List<String> lines = null;
                            try {
                                lines = Files.readAllLines(select.toPath(), StandardCharsets.UTF_8);
                                String[] array = lines.get(Integer.parseInt(currNeuron.getLastTp())+1).split(",");
                                newRow.add(array[1]);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            newRow.add(currNeuron.getLastTp());
                        }
                        newRow.add(x);
                        newRow.add(y);

                        //Status is now stored per neuron in each timepoint object.
                        //When status is changed, it changes the status of that neuron in that timepoint
                        newRow.add(currNeuronStatuses.get(index).get(0));

                        // ALTERNATIVE TO STRINGUTILS?
                        int isCensored = 1;
                        if(currNeuronStatuses.get(index).get(0).equals("Censored") || (timePoints.indexOf(currTimePoint) == timePoints.size()-1)) {
                            isCensored = 0;
                        }
                        newRow.add(Integer.toString(isCensored));
                        rows.put(rowIndex+index-1, newRow);
                    }
                    else {
                        ArrayList<String> newRow = new ArrayList<String>();
                        System.out.println("new row cannot print");
                        ArrayList<String> oldRow = rows.get(rowIndex+index-1);
                        System.out.println("old row cannot print");
                        newRow.add(oldRow.get(0));
                        newRow.add(oldRow.get(1));

                        String x = oldRow.get(3);
                        String y = oldRow.get(4);
                        x = x.replace("]", "\\-]");
                        y = y.replace("]", "\\-]");
                        newRow.add(oldRow.get(2));
                        newRow.add(x);
                        newRow.add(y);

                        newRow.add(currNeuronStatuses.get(index).get(0));


                        int isCensored = 1;
                        if(currNeuronStatuses.get(index).get(0).equals("Censored")) {
                            isCensored = 0;
                        }
                        newRow.add(Integer.toString(isCensored));
                        rows.put(rowIndex+index-1, newRow);
                    }
                }
            }
            rowIndex = rowIndex + currNeuronStatuses.keySet().size();
        }
        FileWriter csvWriter;
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("MMddyyyy");
            Date date = new Date();
            System.out.println(formatter.format(date));

            String delimit;
            if (isWindows()) { delimit = "\\"; }
            else { delimit = "/" ; }
            csvWriter = new FileWriter(outputDir + delimit + "keris-" + formatter.format(date) + ".csv");
            csvWriter.append("Well");
            csvWriter.append(",");
            csvWriter.append("Neuron");
            csvWriter.append(",");
            csvWriter.append("Last Timepoint");
            csvWriter.append(",");
            csvWriter.append("X");
            csvWriter.append(",");
            csvWriter.append("Y");
            csvWriter.append(",");
            csvWriter.append("Status");
            csvWriter.append(",");
            csvWriter.append("Censor");
            csvWriter.append("\n");

            for (ArrayList<String> rowData : rows.values()) {
                csvWriter.append(String.join(",", rowData));
                csvWriter.append("\n");
            }

            csvWriter.flush();
            csvWriter.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "File written successfully.", ButtonType.OK);
            alert.showAndWait();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Error writing to file.", ButtonType.OK);
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    public void printCSVCustom(File select, String outputDir){

    }

    public void readFromCSV(String inputPath, Map<String, List<String>> map, String result) throws FileNotFoundException {
        //Parse CSV
        List<List<String>> storedNeurons = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(inputPath));) {
            while (scanner.hasNextLine()) {
                storedNeurons.add(getRecordFromLine(scanner.nextLine()));
            }
        }
        boolean shouldReadCSV = true;
        //Check and make sure that the CSV and the input matches
        for (int i =1;i<storedNeurons.size();i++) {
            String separator = "\\";
            String wellName = storedNeurons.get(i).get(0);
            List<String> xPoints = Arrays.asList(storedNeurons.get(i).get(3).split(Pattern.quote(separator)));

            LinkedList<Timepoint> tempTimePoints = wellTimepoints.get(wellName);
            if(!map.keySet().contains(wellName) || xPoints.size()-1!=tempTimePoints.size()){
                shouldReadCSV = false;
            }
        }
        if(shouldReadCSV) {
            //Start from first line because first line is column headers
            for (int i = 1; i < storedNeurons.size(); i++) {
                String wellName = storedNeurons.get(i).get(0);
                HashMap<Integer, List<String>> currNeuronStatuses = neuronStatuses.get(wellName);
                //iterate through each timepoint, adding the coordinates for each neuron
                LinkedList<Timepoint> tempTimePoints = wellTimepoints.get(wellName);
                String separator = "\\";
                int labelIndex = Integer.parseInt(storedNeurons.get(i).get(1));
                int lastTP = Integer.parseInt(storedNeurons.get(i).get(2));
                List<String> xPoints = Arrays.asList(storedNeurons.get(i).get(3).split(Pattern.quote(separator)));
                List<String> yPoints = Arrays.asList(storedNeurons.get(i).get(4).split(Pattern.quote(separator)));
                ArrayList<String> lifeStatus = new ArrayList<String>();
                lifeStatus.add(storedNeurons.get(i).get(5));
                String censorStatus = storedNeurons.get(i).get(6);
                int currIndex = 1;

                for (int j = 0; j < tempTimePoints.size(); j++) {
                    //Trim end of array string
                    String xString = xPoints.get(currIndex).replace("]", "");
                    String yString = yPoints.get(currIndex).replace("]", "");
                    if (!xString.equals("-")) {
                        Double x = Double.parseDouble(xString);
                        Double y = Double.parseDouble(yString);
                        Neuron neuron = new Neuron(x, y, result);
                        neuron.setLastTp(Integer.toString(lastTP));
                        currNeuronStatuses.put(labelIndex, lifeStatus);
                        tempTimePoints.get(j).add(neuron, currNeuronStatuses, labelIndex, result,false);
                        //generate listview for neurons for censor
                    }
                    ObservableList<Integer> tempNeuronIdList = neuronIDLists.get(wellName);
                    if (!tempNeuronIdList.contains(labelIndex)) {
                        tempNeuronIdList.add(labelIndex);

                    }
                    neuronIDLists.put(wellName, tempNeuronIdList);
                    currIndex++;
                }

                wellTimepoints.put(wellName, tempTimePoints);
            }
        }
        else{
            Alert alert = new Alert(Alert.AlertType.WARNING, "Error, CSV does not match User Inputs", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public Scene getPrevScene(Double secondary1, Double secondary2, String result) throws URISyntaxException {
        String tempPath;
        if (result.equals("boi")) { tempPath = "\\neuronbkg2.jpg"; }
        else {
            tempPath = "/neuronbkg.jpg";
        }
        Image tempImage = new Image(
                ImgView.class.getResource(tempPath).toURI().toString());
//        Image tempImage = new Image((new File(tempPath).toURI().toString()));
        prevIv.setImage(tempImage);
        StackPane prevRoot = new StackPane();
        prevRoot.getChildren().add(prevIv);

        Parent prevZoomPane = createZoomPane(prevRoot);
        ZoomOperator prevZoomOperator = new ZoomOperator();
        prevZoomPane.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                double zoomFactor = 1.5;
                if (event.getDeltaY() <= 0) {
                    // zoom out
                    zoomFactor = 1 / zoomFactor;
                }
                prevZoomOperator.zoom(prevZoomPane, zoomFactor, event.getSceneX(), event.getSceneY());
            }
        });

        VBox prevBox = new VBox();
        prevBox.getChildren().add(prevZoomPane);
        Scene prevViewScene = new Scene(prevBox, secondary1, secondary2);

        return prevViewScene;
    }

    private Parent createZoomPane(final StackPane group) {
        ScrollPane scroller;
        final double SCALE_DELTA = 1.1;
        final StackPane zoomPane = group;
        final Group zoomContent = new Group(zoomPane);
        // Create a pane for holding the content, when the content is smaller than the view port,
        // it will stay the view port size, make sure the content is centered
        final StackPane canvasPane = new StackPane();
        canvasPane.getChildren().add(zoomContent);
        final Group scrollContent = new Group(canvasPane);
        // Scroll pane for scrolling
        scroller = new ScrollPane();
        scroller.setContent(scrollContent);
        double scaleFactor = 1.1;
        double normalX = 0.1486436280241437;
        group.setScaleX(normalX * scaleFactor);
        group.setScaleY(normalX * scaleFactor);

        // move viewport so that old center remains in the center after the scaling
        Point2D scrollOffset = figureScrollOffset(scrollContent, scroller);
        repositionScroller(scrollContent, scroller, scaleFactor, scrollOffset);

        scroller.viewportBoundsProperty().addListener(new ChangeListener<Bounds>() {
            @Override
            public void changed(ObservableValue<? extends Bounds> observable,
                                Bounds oldValue, Bounds newValue) {
                canvasPane.setMinSize(newValue.getWidth(), newValue.getHeight());
            }
        });

        scroller.setPrefViewportWidth(600);
        scroller.setPrefViewportHeight(800);

        zoomPane.setOnMouseEntered(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {
                CurationGUI.getStage().getScene().setCursor(Cursor.CROSSHAIR);; //Change cursor to hand
            }
        });

        zoomPane.setOnMouseExited(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {
                CurationGUI.getStage().getScene().setCursor(Cursor.DEFAULT);; //Change cursor to hand
            }
        });

        zoomPane.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                event.consume();

                if (event.getDeltaY() == 0) {
                    return;
                }

                double scaleFactor = (event.getDeltaY() > 0) ? SCALE_DELTA
                        : 1 / SCALE_DELTA;
                // amount of scrolling in each direction in scrollContent coordinate units
                Point2D scrollOffset = figureScrollOffset(scrollContent, scroller);
                group.setScaleX(group.getScaleX() * scaleFactor);
                group.setScaleY(group.getScaleY() * scaleFactor);

                // move viewport so that old center remains in the center after the scaling
                repositionScroller(scrollContent, scroller, scaleFactor, scrollOffset);

            }
        });

        // Panning via drag
        final ObjectProperty<Point2D> lastMouseCoordinates = new SimpleObjectProperty<Point2D>();
        scrollContent.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                lastMouseCoordinates.set(new Point2D(event.getX(), event.getY()));
            }
        });

        scrollContent.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                double deltaX = event.getX() - lastMouseCoordinates.get().getX();
                double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
                double deltaH = deltaX * (scroller.getHmax() - scroller.getHmin()) / extraWidth;
                double desiredH = scroller.getHvalue() - deltaH;
                scroller.setHvalue(Math.max(0, Math.min(scroller.getHmax(), desiredH)));

                double deltaY = event.getY() - lastMouseCoordinates.get().getY();
                double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
                double deltaV = deltaY * (scroller.getHmax() - scroller.getHmin()) / extraHeight;
                double desiredV = scroller.getVvalue() - deltaV;
                scroller.setVvalue(Math.max(0, Math.min(scroller.getVmax(), desiredV)));
            }
        });

        return scroller;
    }

    private Point2D figureScrollOffset(Node scrollContent, ScrollPane scroller) {
        double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
        double hScrollProportion = (scroller.getHvalue() - scroller.getHmin()) / (scroller.getHmax() - scroller.getHmin());
        double scrollXOffset = hScrollProportion * Math.max(0, extraWidth);
        double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
        double vScrollProportion = (scroller.getVvalue() - scroller.getVmin()) / (scroller.getVmax() - scroller.getVmin());
        double scrollYOffset = vScrollProportion * Math.max(0, extraHeight);
        return new Point2D(scrollXOffset, scrollYOffset);
    }

    private void repositionScroller(Node scrollContent, ScrollPane scroller, double scaleFactor, Point2D scrollOffset) {
        double scrollXOffset = scrollOffset.getX();
        double scrollYOffset = scrollOffset.getY();
        double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
        if (extraWidth > 0) {
            double halfWidth = scroller.getViewportBounds().getWidth() / 2;
            double newScrollXOffset = (scaleFactor - 1) * halfWidth + scaleFactor * scrollXOffset;
            scroller.setHvalue(scroller.getHmin() + newScrollXOffset * (scroller.getHmax() - scroller.getHmin()) / extraWidth);
        } else {
            scroller.setHvalue(scroller.getHmin());
        }
        double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
        if (extraHeight > 0) {
            double halfHeight = scroller.getViewportBounds().getHeight() / 2;
            double newScrollYOffset = (scaleFactor - 1) * halfHeight + scaleFactor * scrollYOffset;
            scroller.setVvalue(scroller.getVmin() + newScrollYOffset * (scroller.getVmax() - scroller.getVmin()) / extraHeight);
        } else {
            scroller.setHvalue(scroller.getHmin());
        }
    }

}


class ImageViewPane extends Pane {

    private ObjectProperty<ImageView> imageViewProperty = new SimpleObjectProperty<ImageView>();

//    public ObjectProperty<ImageView> imageViewProperty() { return imageViewProperty; }
//    public ImageView getImageView() { return imageViewProperty.get(); }
//    public void setImageView(ImageView imageView) { this.imageViewProperty.set(imageView); }
//    public ImageViewPane() { this(new ImageView()); }

    @Override
    protected void layoutChildren() {
        ImageView imageView = imageViewProperty.get();
        if (imageView != null) {
            imageView.setFitWidth(getWidth());
            imageView.setFitHeight(getHeight());
            layoutInArea(imageView, 0, 0, getWidth(), getHeight(), 0, HPos.CENTER, VPos.CENTER);
        }
        super.layoutChildren();
    }

    public ImageViewPane(ImageView imageView) {
        imageViewProperty.addListener(new ChangeListener<ImageView>() {
            @Override
            public void changed(ObservableValue<? extends ImageView> arg0, ImageView oldIV, ImageView newIV) {
                if (oldIV != null) {
                    getChildren().remove(oldIV);
                }
                if (newIV != null) {
                    getChildren().add(newIV);
                }
            }
        });
        this.imageViewProperty.set(imageView);
    }
}

class RadioListCell extends ListCell<String> {
    private ToggleGroup group = new ToggleGroup();

    @Override
    public void updateItem(String obj, boolean empty) {
        super.updateItem(obj, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            RadioButton radioButton = new RadioButton(obj);
            radioButton.setToggleGroup(group);
            // Add Listeners if any
            setGraphic(radioButton);
        }
    }
}