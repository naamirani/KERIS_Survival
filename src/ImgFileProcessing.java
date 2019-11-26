import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.scene.control.CheckBox;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

import java.security.SecureRandom;


public class ImgFileProcessing {

    private static final String charLower = "abcdefghijklmnopqrstuvwxyz";
    private static final String charUpper = charLower.toUpperCase();
    private static final String numbers = "0123456789";

    private static final String randomStringData = charLower + charUpper + numbers;
    private static SecureRandom random = new SecureRandom();

    LinkedHashMap<String, String> blindMap = new LinkedHashMap<>();


    public List<String> getFiles(String inputDir, String outputDir, String wls, String chnls, String tps) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Imgcodecs imageCodecs = new Imgcodecs();

        List<String> finalTimes = new ArrayList<>();
        List<String> finalWells = new ArrayList<>();

        File tempDir = new File(outputDir);
        File file = new File(inputDir);
        if (file.listFiles() == null) {
            System.out.println("You shall not pass");
            System.exit(0);
        }

        // recursively retrieve files matching users criteria from specified directory
        for (File f : file.listFiles()) {
            if (f.isDirectory()) {
                getFiles(f.getAbsolutePath(), outputDir, wls, chnls, tps);
            } else if (f.isFile()) {
                if (f.getName().endsWith(".tif")) {
                    List<String> channels = new ArrayList<>(Arrays.asList(chnls.split(",")));
                    List<String> timepoints = new ArrayList<>(Arrays.asList(tps.split(",")));

                    wls = wls.replaceAll("\\s","");
                    List<String> wells = new ArrayList<>(Arrays.asList(wls.split(",")));
                    for (String well : wells) {
                        if (well.contains("-")) {
                            String wellLetter = Character.toString(well.charAt(0));
                            well = well.replaceAll(wellLetter, "");
                            String[] wellChunks = well.split("-");
                            for (int i = Integer.parseInt(wellChunks[0]); i <= Integer.parseInt(wellChunks[1]); i++) {
                                finalWells.add(wellLetter + Integer.toString(i));
                            }
                        } else {
                            finalWells.add(well);
                        }
                    }

                    int count = 0;
                    while (count < finalWells.size()) {
                        finalWells.set(count, finalWells.get(count) + "_");
                        count += 1;
                    }

                    if (timepoints.get(0).equals("")) {
                        for (int i = 0; i <= Integer.parseInt(timepoints.get(1)); i++) {
                            finalTimes.add(Integer.toString(i));
                        }
                    }
                    else {
                        for (String time : timepoints) {
                            time = time.replaceAll("T", "");
                            if (time.contains("-")) {
                                String[] timeChunks = time.split("-");
                                for (int i = Integer.parseInt(timeChunks[0]); i <= Integer.parseInt(timeChunks[1]); i++) {
                                    finalTimes.add(Integer.toString(i));
                                }
                            } else {
                                finalTimes.add(time);
                            }
                        }
                    }

                    count = 0;
                    while (count < finalTimes.size()) {
                        finalTimes.set(count, "T" + finalTimes.get(count) + "_");
                        count += 1;
                    }

//                    List<String> channels = new ArrayList<>(Arrays.asList("RFP"));
//                    List<String> timepoints = new ArrayList<>(Arrays.asList("T0_", "T1_"));

                    // allConds: list of string lists (one for each category, allowing multiple criteria)
                    List<List<String>> allConds = new ArrayList<>();

                    allConds.add(channels);
                    allConds.add(finalTimes);
                    allConds.add(finalWells);

                    // if all conditions satisfied, generate file in output directory in png format
                    if (stringContainsItemFromList(f.getName(), allConds) == allConds.size()) {
                        System.out.println(f.getName());
//                        String url1 = f.getAbsolutePath().substring(0, f.getAbsolutePath().lastIndexOf("\\", (f.getAbsolutePath().length() - 2)) - 2);
                        String url2 = f.getName().substring(0, f.getName().lastIndexOf("."));
//                        File tempDir = new File(url1 + "test");
                        Mat matrix = imageCodecs.imread(f.getAbsolutePath());
                        if (inputDir.contains("/Users/")) {
                            System.out.println(tempDir.getAbsolutePath() + "/" + url2 + ".png");
                            imageCodecs.imwrite(tempDir.getAbsolutePath() + "/" + url2 + ".png", matrix);
                        }
                        else {
                            System.out.println(tempDir.getAbsolutePath() + "\\" + url2 + ".png");
                            imageCodecs.imwrite(tempDir.getAbsolutePath() + "\\" + url2 + ".png", matrix);

                        }
                    }
                }
            }
        }
        return finalTimes;
    }

    // checks if filename (as string) contains all possible keywords as input by user
    public int stringContainsItemFromList(String inputStr, List<List<String>> allConds) {
        int count = 0;
        for (List<String> cond : allConds) {
            if (cond.parallelStream().anyMatch(inputStr::contains) == true) {
                count++;
            }
        }
        return count;
    }

    public void setColors(Map<String, List<String>> map,List<String> clrs, List<String> chnls) {
        HashMap<String,String> channelColors = new HashMap<String,String>();
        int j = 0;
        for(String color: clrs){
            channelColors.put(chnls.get(j),color);
            j++;
        }
        if (clrs.size() != 1) {
            for (String currWellName : map.keySet()) {
                List<String> currWellList = map.get(currWellName);
                int i = 0;
                for (String l : currWellList) {
                    String segments[] = l.split("_");
                    String channel[] = segments[6].split("-");
                    String currChannel = "";
                    for (String chn : channel) {
                        if (chnls.contains(chn)) {
                                currChannel = chn;
                        }
                    }

                    String currColor = channelColors.get(currChannel);
                    System.out.println("INVERTING AND SETTING COLORS:" + l);
                    if (!currColor.equals(" ")) {
                        updateColor(l, currColor);
                    }
                    i++;
                    if(i>=clrs.size()){
                        i=0;
                    }
                }
            }
        }
    }

    private void updateColor(String url, String color) {
        File f = new File(url);
        try {
            BufferedImage img = ImageIO.read(f);
            //get image width and height
            int width = img.getWidth();
            int height = img.getHeight();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {

                    int p = img.getRGB(x, y);
                    int a = (p >> 24) & 0xff;
                    int r = (p >> 16) & 0xff;
                    int g = (p >> 8) & 0xff;
                    int b = p & 0xff;
                    if (r == 0) {
                        r = 255 - r;
                        g = 255 - g;
                        b = 255 - b;
                    } else {
                        Color c = Color.web(color);
                        r = (int) c.getRed() * 255;
                        g = (int) c.getGreen() * 255;
                        b = (int) c.getBlue() * 255;
                    }
                    p = (a << 24) | (r << 16) | (g << 8) | b;
                    img.setRGB(x, y, p);

                }
            }
            try {
                ImageIO.write(img, "png", f);
            } catch (IOException e) {
                System.out.println(e);
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }
    //given lists in test directory, generate drop down list of just well letters
    //also generate image arrays that belong to the specific well
    public Map<String, List<String>> getWellSets(CheckBox check, String dir) {

        File dirName = new File(dir);
        FilenameFilter pngFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if(name.contains(".png")){
                    return true;
                }
                return false;
            }
        };
        File[] tempArray = dirName.listFiles(pngFilter);

        //sort files
        Arrays.sort(tempArray, new ImgFileComparator());

        List<String> wellNames = new ArrayList<>();

        for (File f : tempArray) {
            String name = f.getName();
            String segments[] = name.split("_");
            String well = segments[4];
            wellNames.add(well);
        }

        LinkedHashMap<String, List<String>> hashMap = new LinkedHashMap<>();

        for (File f : tempArray) {
            String segments[] = f.getName().split("_");
            if (!hashMap.containsKey(segments[4])) {
                List<String> list = new ArrayList<>();
                list.add(f.getName());
                hashMap.put((segments[4]), list);
                if (check.isSelected() == true) {
                    blindMap.put((segments[4]), generateRandomString(5));
                }
            } else {
                hashMap.get(segments[4]).add(f.getName());
            }
        }
        return hashMap;
    }

    public static String generateRandomString(int length) {
        if (length < 1) {
            throw new IllegalArgumentException();
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            // 0-62 (exclusive), random returns 0-61
            int rndCharAt = random.nextInt(randomStringData.length());
            char rndChar = randomStringData.charAt(rndCharAt);
            // debugging
            System.out.format("%d\t:\t%c%n", rndCharAt, rndChar);
            sb.append(rndChar);
        }
        return sb.toString();
    }

    public HashMap getBlindMap() { return blindMap; }
}