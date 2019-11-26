import java.io.File;
import java.util.Comparator;

public class ImgFileComparator implements Comparator<File> {
    public int compare(File o1, File o2) {
        String o1_name = o1.getName();
        String o2_name = o2.getName();
        String segments1[] = o1_name.split("_");
        String segments2[] = o2_name.split("_");

        String wellLetter1 = Character.toString(segments1[4].charAt(0));
        String wellLetter2 = Character.toString(segments2[4].charAt(0));
        int wellNumber1 = Integer.parseInt(segments1[4].substring(1));
        int wellNumber2 = Integer.parseInt(segments2[4].substring(1));
        int timepoint1 = Integer.parseInt(segments1[2].substring(1));
        int timepoint2 = Integer.parseInt(segments2[2].substring(1));

        // all comparisons
        int compareWellNumber = wellNumber1 - wellNumber2;
        int compareWellLetter = wellLetter1
                .compareTo(wellLetter2);
        int compareTimepoint = timepoint1 - timepoint2;

        // 3-level comparison
        // 1: well letter
        // 2: well number
        // 3: timepoint
        if(compareWellLetter == 0) {
            return ((compareWellNumber == 0) ? compareTimepoint : compareWellNumber);
        }
        else {
            return compareWellLetter;
        }
    }
}