
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Timepoint {

    Group markers;
    Group labels;
    HashMap<Integer,Neuron> neurons;
    int currLabelIndex;
    String timepointNum="";
    int tpIndex=0;

    public Timepoint(String timepointNum) {
        markers = new Group();
        labels = new Group();
        neurons = new HashMap<Integer,Neuron>();
        currLabelIndex = 1;
        this.timepointNum = timepointNum;
        tpIndex = Integer.valueOf(timepointNum.replace("T",""));
    }

    public void add(Neuron neuron,  HashMap<Integer,List<String>> statuses, Integer index, String result, Boolean isCustom) {
        neuron.setLabel(Integer.toString(index));
        Double x = neuron.getXCoordinate();
        Double y = neuron.getYCoordinate();
        neuron.setStatus(statuses.get(index));
        neurons.put(index,neuron);
        markers = new Group();
        labels = new Group();
        for (Integer i : statuses.keySet()) {
            Neuron currNeuron = neurons.get(i);
            if(currNeuron != null) {
                x = currNeuron.getXCoordinate();
                y = currNeuron.getYCoordinate();
                Label neuronNum = new Label(Integer.toString(i));
                neuronNum.setFont(Font.font("Dialog", FontWeight.BOLD, 100));
                if (result.
                        equals("boi")) { result = "pink"; }
                Color c = Color.web(result);
                neuronNum.setTextFill(c);
                neuronNum.setLayoutX(x+50);
                neuronNum.setLayoutY(y-50);
                markers.getChildren().add(currNeuron.getCircle());
                labels.getChildren().add(neuronNum);
            }
        }
        int i = index+1;
        while(i<=statuses.keySet().size()) {
            if(!isCustom && statuses.get(i).get(0).equals("Alive")) {
                currLabelIndex = i;
                break;
            }
            else if(isCustom){
                currLabelIndex = i;
                break;
            }
            i++;
        }
        if(currLabelIndex == index) {
            if(i ==statuses.keySet().size()+1) {
                currLabelIndex = statuses.keySet().size()+1;
            }
            else {
                currLabelIndex++;
            }
        }
    }

    public void removeAll() {
        markers.getChildren().clear();
        labels.getChildren().clear();
        neurons.clear();
        currLabelIndex = 1;
    }

    public void removeNeuron(Integer index, String result) {
        neurons.remove(index);
        markers = new Group();
        labels = new Group();
        for (Integer i : neurons.keySet()) {
            Neuron currNeuron = neurons.get(i);
            if(currNeuron != null) {
                Double x = currNeuron.getXCoordinate();
                Double y = currNeuron.getYCoordinate();
                Label neuronNum = new Label(Integer.toString(i));
                neuronNum.setFont(Font.font("Dialog", FontWeight.BOLD, 100));
                neuronNum.setTextFill(Color.web(result));
                neuronNum.setLayoutX(x+50);
                neuronNum.setLayoutY(y-50);
                markers.getChildren().add(currNeuron.getCircle());
                labels.getChildren().add(neuronNum);
            }
        }
    }

    public void setCurrLabelIndex(Integer currIndex) {
        currLabelIndex = currIndex;
    }
    public Group getMarkersGroup() {
        return markers;
    }public Group getLabelsGroup() {
        return labels;
    }
    public HashMap<Integer,Neuron> getNeurons(){
        return this.neurons;
    }
    public Integer getTotalNeurons() {
        return currLabelIndex-1;
    }

}