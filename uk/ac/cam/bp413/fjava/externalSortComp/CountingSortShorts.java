package uk.ac.cam.bp413.fjava.externalSortComp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class CountingSortShorts {

    // The size of the buffer used for reading data.
    private static final int BUF_SIZE = 1<<16;

    // The array used to store the number of repeats of each value.
    private static short[] arr;

    // The buffer that is used to read in bytes from the file.
    private static byte[] buf;


    // the ratio of the number of bytes to one integer, representing the same value.
    private static final int BYTES_TO_INTS = 4;

    // the value returned by the read method in Random Access File when there are no more bytes to read.
    private static final int READ_FINISHED = -1;

    // The input stream used to read in the data.
    private static FileInputStream fis;

    public static void run(String fin, RandomAccessFile fos, int min, int lx){

        // This function takes the name of the file to read in, and the object used for an output stream,
        // as well as the number of indexes to be held by the repetitions array.

        // This version of counting sort assumes high numbers of repeats, hence using a short to store the
        // number of repeats if each element, and offsets all values by the min..

        // Creates a new array for storing repeats.
        arr = new short[lx];
        // Creates a new byte array to use as a buffer to read in the data.
        buf = new byte[BUF_SIZE];

        try {
            // Creates a new stream to read in the data.
            fis = new FileInputStream(fin);

            // read in the integers within the given range, and work out the number of repeats for each.
            readFromBuffer(min);

            // set fos to point to file a
            fos = new RandomAccessFile(fin, "rw");

            // write out the values in sorted order to file A.
            writeToBuffer(fos, min);


        } catch (IOException e) {
            e.printStackTrace();
        }

        arr = null;
        buf = null;

    }

    private static void readFromBuffer(int boundLower) throws IOException{

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


                // offset by the lower bound, and then increment the repetitions at index x in the repetitions array.
                x = x - boundLower;
                arr[x] = (short) (arr[x] + 1);
            }

        }
    }

    private static void writeToBuffer(RandomAccessFile fos, int min) {

        // this method writes out each integer, the number of times it is repeated, to the file.

        // loc is the current location in the buffer to be written.
        int loc = 0;

        // the current value to be written.
        int x;

        // The four bytes that make up the byte components of the current value to be written.
        byte b1; byte b2; byte b3; byte b4;

        // The number of repetitions of the current value.
        int reps;

        // the length of the repetitions array.
        int length = arr.length;

        try {

            for (int i = 0; i < length; i++) {
                // iterate through the repetitions array to get the number of repetitions at each index.


                // gets the number of repetitions. Use the unsigned function incase exactly 8 bits were used
                // and would otherwise get a negative number.
                reps = Short.toUnsignedInt(arr[i]);

                // reverse the min offset to get the origional value back.
                x = i + min;
                if (reps > 0) {
                    // convert the current integer value to be written to four bytes.
                    b1 = (byte) (x >> 24);
                    b2 = (byte) (x >> 16);
                    b3 = (byte) (x >> 8);
                    b4 = (byte) (x);

                    for (int j = 0; j < reps; j++) {
                        // write the current integer repeatedly until the number of repetitions is reached.

                        // checks if the buffer is full.
                        if (loc == BUF_SIZE) {
                            // if the buffer is full, write its contents out to the file.
                            fos.write(buf);
                            // the buffer has been emptied, so set its current location back to the start.
                            loc = 0;
                        }

                        // copy the bytes into the buffer.
                        buf[loc] = b1;
                        buf[loc + 1] = b2;
                        buf[loc + 2] = b3;
                        buf[loc + 3] = b4;
                        // increment the loc value to the next int position in the buffer.
                        loc += 4;
                    }
                }
            }

            if (loc > 0) {
                // if there are any values left over in the buffer, write them out to the file.
                fos.write(buf, 0, loc);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
