import javafx.scene.Group;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.opencv.imgcodecs.Imgcodecs.imread;

public class ImgFileSortTest {

    public static void main(String[] args) throws Exception {

//        ImgFileProcessing processor = new ImgFileProcessing();
//
//        File files = new File("/Users/naufaamirani/Documents/test_wells");
//        String[] directories = files.list(new FilenameFilter() {
//            @Override
//            public boolean accept(File current, String name) {
//                return new File(current, name).isDirectory();
//            }
//        });
//
//        for(String f:directories) {
//            System.out.println(f);
//        }

//        file.mkdirs();

//        String[] channelSeg = new String[]{"Potato", "Carrot"};
//        boolean contains = Arrays.stream(channelSeg).anyMatch("Peanut"::equals);
//        if (contains == true) {
//            System.out.println("Carrot is present");
//        }
//        else {
//            System.out.println("Not present");
//        }
//    }
        appendStrToFile("test");
    }

        public static void appendStrToFile(String str){
            SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss");
            Date date = new Date();
            formatter.format(date);
            String fileName = "errorlog.csv";
            try {
                // Open error log file in append mode
                BufferedWriter out = new BufferedWriter(
                        new FileWriter(fileName, true));
                out.write(formatter.format(date));
                out.write("Yeet" + "," + str);
                out.close();
            } catch (IOException e) {
                System.out.println("Exception in error handling occurred:  " + e);
            }
        }
    }
