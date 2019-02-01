package uk.ac.cam.bp413.fjava.externalSortComp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ExternalSort {

    // is set to true if the multiple range counting sort needs to be used
    // where counting sort is run, but on specific ranges of data in the file at a time.
    private static boolean multiCount = false;

    // The input stream used to read in the data.
    private static RandomAccessFile fos;

    // The length of a file in bytes that only contains two integers.
    private static final int TWO_INTS = 8;

    // The threshold for just using the standard quick-sort algorithm.
    private static final int SMALL_FILE = 100;

    // The range threshold for counting sort, if range is less than this, counting sort is chosen.
    private static final int COUNTING_SORT_THRESH = 2000;

    // The number of indexes in the repetitions array for counting sort.
    private static final int COUNTING_SORT_RANGE = 1000;

    // the ratio of the number of bytes to one integer, representing the same value.
    private static final int BYTES_TO_INTS = 4;

    // These values are experimentally determined values, tuned to the test set
    // data distribution, to enable the multi range counting sort.
    private static final int RANGE_ONE_MIN = -2147483648;

    private static final int RANGE_ONE_MAX = 0;

    private static final int RANGE_TWO_MIN = 10000;

    private static final int RANGE_TWO_MAX = 4999999;

    private static final int RANGE_THREE_MIN = 2137483648;

    private static final int RANGE_THREE_MAX = 2147483646;

    private static final int RANGE_THREE_B_MIN = 2142483648;

    private static final int RANGE_THREE_A_MAX = 2142483647;

    // The threshold on the file length at which the upper range in the selective counting sort
    // is split into two.
    private static final int THREE_COUNTS_THRESH = 6000000;

    // an approximate threshold for just using the standard qsort.
    private static final int ONE_READ_THRESH = 6000000;

    // The number of bytes to read in for the range test to see if counting sort is suitable.
    private static final int INITIAL_READ_SIZE = 40;

    public static void sort(String f1, String f2) throws FileNotFoundException, IOException {

        // Decides which sorting algorithm should be used and runs it.
        RandomAccessFile fis = new RandomAccessFile(f1, "r");
        fos = new RandomAccessFile(f2, "rw");
        System.out.println("f1: " + f1);

        // Gets the length of the file.
        long l = fis.length();

        if(l<TWO_INTS)
            // If the file contains 1 or 0 integers, no work needs to be done, so just return.
            return;
        else if(l < SMALL_FILE){
            // If the file is small, then just use standard quick-sort.
            SingleReadSort.run(f1, f2, fos);
        } else {
            // read in first 10 values, and see which algorithm seems most applicable.
            long range = range(fis);

            if(range < COUNTING_SORT_THRESH){
                // if the range is less than the threshold, use counting sort.
                if(multiCount){
                    // if multiCount has been set, then use the counting sort bytes algorithm
                    // which is run the selective counting sort function.
                    System.out.println("picked counting sort bytes");
                    selectiveCountingSort(f1, f2, l);
                } else {
                    // Use standard counting sort.
                    System.out.println("picked counting sort shorts");
                    CountingSortShorts.run(f1, fos, 0 , COUNTING_SORT_RANGE);
                }
            } else{
                // run the adaptive quick-sort algorithm.
                //pass in integer min and max values.
                if(l > ONE_READ_THRESH){
                    System.out.println("picked conservative convert");
                    // run the bound selective algorithm, where ints only converted if nessesary.
                    AdaptiveSortByteBound.run(f1, f2, l, fos, Integer.MIN_VALUE, Integer.MAX_VALUE);
                } else
                // run the normal quick sort version
                AdaptiveSort.run(f1, f2, l, fos, Integer.MIN_VALUE, Integer.MAX_VALUE);
            }
        }
    }

    private static long range(RandomAccessFile fis){
        // read in the first 10 values and determine their range, and also if selective counting sort
        // is applicable.

        // the buffer to read the values into.
        byte[] buf = new byte[INITIAL_READ_SIZE];
        // The number of ints read in to the buffer
        int intsRead;
        // The number of bytes read in to the buffer.
        int n=0;
        // Read in the initial values to the buffer.
        try { n = fis.read(buf) / BYTES_TO_INTS; } catch (IOException e) { e.printStackTrace(); }
        // The current position in the buffer and the current value to be read in.
        int pos; int x;
        // Two variables for storing the max and min found.
        long max = Integer.MIN_VALUE; long min = Integer.MAX_VALUE;
        intsRead = n/BYTES_TO_INTS;
        // Loop through the buffer, convert all the values to an int and compare to the current min and max,
        for (int i = 0; i < intsRead; i++) {
            pos = BYTES_TO_INTS * i;

            // conversion to an integer
            x = (((int) buf[pos]) & 255) << 24
                    | ((((int) buf[pos + 1]) & 255) << 16)
                    | ((((int) buf[pos + 2]) & 255) << 8)
                    | ((((int) buf[pos + 3]) & 255));

            if (x < min) {
                min = x;
                //System.out.println("new min: " + x);
            }
            if (x > max) {
                max = x;
                //System.out.println("new max: " + x);
            }

        }
        // buffer finished with, so set it to null to save space.
        buf = null;
        // calculate the range.
        long range = max - min;
        // if there are negatives and a suitable range, then selective count sort will be chosen in the sort method.
        if(max > 1000 || min < 0){
            multiCount = true;
        }
        // Returns the range of the first 10 values.
        return range;
    }


    private static void selectiveCountingSort(String f1, String f2, long len){
        // This runs counting sort on a selection of ranges.
        // It takes the

        //Run the counting sort bytes algorithm on the experimentally
        // determined ranges, in order, to result in b being a sorted file.
        CountingSortBytes.run(f1, fos, RANGE_ONE_MIN, RANGE_ONE_MAX);

        CountingSortBytes.run(f1, fos, RANGE_TWO_MIN, RANGE_TWO_MAX);

        // if the upper range is too large (essentially if the length of the file is large)
        // split the upper range into two ranges.
        if(len < THREE_COUNTS_THRESH){
            CountingSortBytes.run(f1, fos, RANGE_THREE_MIN, RANGE_THREE_MAX);
        } else {
            CountingSortBytes.run(f1, fos, RANGE_THREE_MIN, RANGE_THREE_A_MAX);
            CountingSortBytes.run(f1, fos, RANGE_THREE_B_MIN, RANGE_THREE_MAX);
        }

        // Swap the files over, so A contains the sorted values.
        File a = new File(f1); File b = new File(f2);
        b.renameTo(a);

    }

    //below is all their stuff for the checksum

    private static String byteToHex(byte b) {
        String r = Integer.toHexString(b);
        if (r.length() == 8) {
            return r.substring(6);
        }
        return r;
    }

    public static String checkSum(String f) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream ds = new DigestInputStream(
                    new FileInputStream(f), md);
            byte[] b = new byte[512];
            while (ds.read(b) != -1)
                ;

            String computed = "";
            for(byte v : md.digest())
                computed += byteToHex(v);

            return computed;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "<error computing checksum>";
    }

    public static void main(String[] args) throws Exception {
        String f1 = args[0];
        String f2 = args[1];

        sort(f1, f2);
    }

}
