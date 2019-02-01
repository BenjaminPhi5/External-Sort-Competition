package uk.ac.cam.bp413.fjava.externalSortComp;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class SingleReadSort {

    // The array that the selected integers are read into.
    private static int[] ints;
    // The buffer that is used to read in bytes from the file.
    private static byte[] buf;
    // The random access file used as an input stream for the data.
    private static RandomAccessFile fis;

    // The size of the buffer used for reading data.
    private static final int BUF_SIZE = 1 << 16;
    // An experimental constant for preventing heap overflow error when trying to
    // assign an array as large as is available in memory.
    private static final double R = 0.840;

    // the number of times I want to split any given range (see uses);
    private static final int HALVES = 2;

    // the ratio of the number of bytes to one integer, representing the same value.
    private static final int BYTES_TO_INTS = 4;

    // the value returned by the read method in Random Access File when there are no more bytes to read.
    private static final int READ_FINISHED = -1;

    // The main method for sorting the data.
    public static final void run(String filenameIn, String filenameOut, RandomAccessFile fos) {

        // INITIALISING VARIABLES
        // The two files are used for swapping B with A when the algorithm has finished running.
        File a = new File(filenameIn);
        File b = new File(filenameOut);

        try {
            // assigned the random access file to read from file a
            fis = new RandomAccessFile(a, "r");
        } catch (IOException e){
            e.printStackTrace();
        }

        // The sorter object used later on for sorting
        ForkJoinQSort sorter;

        // Creates a new byte array to use as a buffer to read in the data.
        buf = new byte[BUF_SIZE];

        // creates a new integer array to fit the size of the given file.
        ints = new int[(int)a.length()/BYTES_TO_INTS];

        // Index will hold the current position in the integer array, so that I know
        // which index to write to next, and how much of the array has been used.
        int index = 0;

        try {

            // Read in the desired vales to be sorted into the array, and return the number of values read.
            index = readFromBuffer();

            // call the quick sort algorithm.
            if (index > 0) {
                // run the sort on the array of integer values
                sorter = new ForkJoinQSort();
                //sorter.setArr(ints);
                // run the standard sort algorithm with no threads directly.
                //sorter.stSort(0, index-1);
                sorter.run(ints, 0, index-1);
            }

            // write the sorted array out to the file
            writeToBuffer(index, fos);

            // Rename file be to a so that a now holds the sorted data.
            b.renameTo(a);

        } catch (IOException e){
            e.printStackTrace();
        }

        //Alg finished, set relevant values to null to save space.
        buf = null; ints = null; sorter = null; fis = null; fos = null;

    }

    private static int readFromBuffer() throws IOException{

        // Reads in the entire file, one buffer at a time, selecting integer values that fall
        // in the range stated by the upper and lower bounds, and adding them to the integer
        // array of values ot be sorted.

        // The number of bytes read into the buffer after a read.
        int n;
        // The number of ints read into the buffer after a read.
        int intsRead;
        // The current position in the byte buffer.
        int pos;
        // The current integer values being processed.
        int x;
        // The current position in the integer array of values to be sorted.
        int index = 0;

        // The loop condition reads in the next buffer of bytes from the file, until there are no
        // more bytes to be read in.
        while ((n = fis.read(buf)) != READ_FINISHED) {
            intsRead = n / BYTES_TO_INTS;

            // iterate through all the ints currently being held in the byte buffer
            // if they fall in the given bound, add them to the int array to be sorted.
            for (int i = 0; i < intsRead; i++) {
                pos = BYTES_TO_INTS * i;

                // Convert the next set of 4 bytes to an integer.
                x = (((int) buf[pos]) & 255) << 24
                        | ((((int) buf[pos + 1]) & 255) << 16)
                        | ((((int) buf[pos + 2]) & 255) << 8)
                        | ((((int) buf[pos + 3]) & 255));


                // put the current integer into the int array
                ints[index] = x;
                index++;
                }
            }


        // This returns the number of values selected to be sorted.
        return index;
    }

    private static void writeToBuffer(int index, RandomAccessFile fos) {

        // this method writes out the sorted integers to the output stream fos.

        try {

            // x is the current integer value to be written.
            int x;
            // loc is the current location in the buffer to be written.
            int loc = 0;

            // iterates through each value in the sorted integer array.
            for (int k = 0; k < index; k++) {
                // gets the current integer.
                x = ints[k];

                // checks if the buffer is full.
                if (loc == BUF_SIZE) {
                    // if the buffer is full, write its contents out to the file.
                    fos.write(buf);
                    // the buffer has been emptied, so set its current location back to the start.
                    loc = 0;
                }

                // convert the current integer value to be written to a byte array.
                buf[loc] = (byte) (x >> 24);
                buf[loc + 1] = (byte) (x >> 16);
                buf[loc + 2] = (byte) (x >> 8);
                buf[loc + 3] = (byte) (x);

                // increment the location in the buffer now that a value has been written in.
                loc += BYTES_TO_INTS;
            }

            if (loc > 0) {
                // if there are any values left over in the buffer, write them out to the file.
                fos.write(buf, 0, loc);

            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

}

