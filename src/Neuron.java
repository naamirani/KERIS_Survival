import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Neuron {
    //get methods: xy location, label

    String label = "1";
    Double xCoor;
    Double yCoor;
    Circle circle;
    String lifeStatus;
    Integer censorStatus;
    String lastTp = "0";
    List<String> status = new ArrayList<String>();

    public Neuron() {
    }

    public Neuron(Double xCoor, Double yCoor, String result) {
        this.lifeStatus = "Alive";
        this.censorStatus = 1; //uncensored
        this.xCoor = xCoor;
        this.yCoor = yCoor;
        this.circle = new Circle(xCoor, yCoor, 30);
        if (result.equals("boi")) { result = "pink"; }
        Color c = Color.web(result);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(c);
        circle.setStrokeWidth(8);
    }

    public double getXCoordinate() {
        return xCoor;
    }

    public void setStatus(List<String> status){
        this.status = status;
    }
    public double getYCoordinate() { return yCoor; }

    public String setLabel(String newLabel) {
        return label = newLabel;
    }

    public String setLifeStatus(String status) { return this.lifeStatus = status; }
    public String getLifeStatus() {
        return this.lifeStatus;
    }

    public Integer setCensorStatus(Integer status) {
        return this.censorStatus = status;
    }
    public Integer getCensorStatus() {
        return this.censorStatus;
    }

    public String setLastTp(String last) {
        return this.lastTp = last;
    }
    public String getLastTp() {
        return this.lastTp;
    }

    public String getLabel() {
        return label;
    }

    public Circle getCircle() {
        return circle;
    }
}
