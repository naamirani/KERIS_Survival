import java.awt.Color;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


public class JavaFX_ImageHistogram extends Application {

    String defaultImage = "https://images.costco-static.com/ImageDelivery/imageService?profileId=12026540&imageId=1279387-847__1&recipeName=350";

    @Override
    public void start(Stage primaryStage) {

        TextField textSrc = new TextField();
        textSrc.setText(defaultImage);
        Button btnDo = new Button("Generate Histogram");
        ImageView imageView = new ImageView();

        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        final LineChart<String, Number> chartHistogram
                = new LineChart<>(xAxis, yAxis);
        chartHistogram.setCreateSymbols(false);

        final CategoryAxis xAxis_brightness = new CategoryAxis();
        final NumberAxis yAxis_brightness = new NumberAxis();
        final LineChart<String, Number> brightnessHistogram
                = new LineChart<>(xAxis_brightness, yAxis_brightness);
        brightnessHistogram.setCreateSymbols(false);

        VBox vBoxHistogram = new VBox();
        vBoxHistogram.getChildren().addAll(chartHistogram, brightnessHistogram);
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(vBoxHistogram);

        btnDo.setOnAction((ActionEvent event) -> {

            String imageSrc = textSrc.getText();
            Image image = new Image(imageSrc);
            imageView.setImage(image);
            chartHistogram.getData().clear();
            brightnessHistogram.getData().clear();

            ImageHistogram imageHistogram = new ImageHistogram(image);
            if(imageHistogram.isSuccess()){
                chartHistogram.getData().addAll(
                        //imageHistogram.getSeriesAlpha(),
                        imageHistogram.getSeriesRed(),
                        imageHistogram.getSeriesGreen(),
                        imageHistogram.getSeriesBlue());

                brightnessHistogram.getData().add(
                        imageHistogram.getSeriesBrightness());
            }
        });

        HBox hBox = new HBox();
        hBox.getChildren().addAll(imageView, scrollPane);

        VBox vBox = new VBox();
        vBox.getChildren().addAll(textSrc, btnDo, hBox);

        StackPane root = new StackPane();
        root.getChildren().add(vBox);

        Scene scene = new Scene(root, 1000, 500);

        primaryStage.setTitle("Image Pixel Intensity");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    class ImageHistogram {

        private Image image;

        private long alpha[] = new long[256];
        private long red[] = new long[256];
        private long green[] = new long[256];
        private long blue[] = new long[256];

        private long brightness[] = new long[256];

        XYChart.Series seriesAlpha;
        XYChart.Series seriesRed;
        XYChart.Series seriesGreen;
        XYChart.Series seriesBlue;

        XYChart.Series seriesBrightness;

        private boolean success;

        ImageHistogram(Image src) {
            image = src;
            success = false;

            //init
            for (int i = 0; i < 256; i++) {
                alpha[i] = red[i] = green[i] = blue[i] = 0;
                brightness[i] = 0;
            }

            PixelReader pixelReader = image.getPixelReader();
            if (pixelReader == null) {
                return;
            }

            //count pixels
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = pixelReader.getArgb(x, y);
                    int a = (0xff & (argb >> 24));
                    int r = (0xff & (argb >> 16));
                    int g = (0xff & (argb >> 8));
                    int b = (0xff & argb);

                    alpha[a]++;
                    red[r]++;
                    green[g]++;
                    blue[b]++;

                    //Convert RGB to HSB (or HSV)
                    float[] hsb = new float[3];
                    Color.RGBtoHSB(r, g, b, hsb);
                    brightness[(int)(hsb[2]*255)]++;
                }
            }

            seriesAlpha = new XYChart.Series();
            seriesRed = new XYChart.Series();
            seriesGreen = new XYChart.Series();
            seriesBlue = new XYChart.Series();
            seriesBrightness = new XYChart.Series();
            seriesAlpha.setName("alpha");
            seriesRed.setName("red");
            seriesGreen.setName("green");
            seriesBlue.setName("blue");
            seriesBrightness.setName("Brightness");

            //copy alpha[], red[], green[], blue[], brightness
            //to seriesAlpha, seriesRed, seriesGreen, seriesBlue, seriesBrightness
            for (int i = 0; i < 256; i++) {
                seriesAlpha.getData().add(new XYChart.Data(String.valueOf(i), alpha[i]));
                seriesRed.getData().add(new XYChart.Data(String.valueOf(i), red[i]));
                seriesGreen.getData().add(new XYChart.Data(String.valueOf(i), green[i]));
                seriesBlue.getData().add(new XYChart.Data(String.valueOf(i), blue[i]));

                seriesBrightness.getData().add(new XYChart.Data(String.valueOf(i), brightness[i]));
            }

            success = true;
        }

        public boolean isSuccess() {
            return success;
        }

        public XYChart.Series getSeriesAlpha() {
            return seriesAlpha;
        }

        public XYChart.Series getSeriesRed() {
            return seriesRed;
        }

        public XYChart.Series getSeriesGreen() {
            return seriesGreen;
        }

        public XYChart.Series getSeriesBlue() {
            return seriesBlue;
        }

        public XYChart.Series getSeriesBrightness() {
            return seriesBrightness;
        }
    }

}