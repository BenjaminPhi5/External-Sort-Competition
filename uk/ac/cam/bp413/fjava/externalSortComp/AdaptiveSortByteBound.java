package uk.ac.cam.bp413.fjava.externalSortComp;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class AdaptiveSortByteBound {

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

    // The shift required to get only the first 8 bits of an integer.
    private static final int FIRST_BYTE_SHIFT = 24;

    // Specific bound values determined form experimental testing.
    // Used to cut a bound in half when the bound is detected, as opposed to waiting
    // for an error and then restarting the bound, to save time.
    private static final int[] TRICKY_BOUND_VALUES = new int[]{83, 105, 106, 127};

    // The main method for sorting the data.
    public static  void run(String filenameIn, String filenameOut, long length, RandomAccessFile fos,
                            int min, int max) {

        // This method takes in the names of the two files, (in order to be able to swap the
        // files over later), the length of the file, for determining bound sizes, a RandomAccessFile
        // to use as a stream to output the data, and two values min and max.
        // When running the code, these values are just integer min and max,
        // and so not necessarily needed, but I keep them here for testing purposes.

        // INITIALISING VARIABLES
        // The two files are used for swapping B with A when the algorithm has finished running.
        File a = new File(filenameIn);
        File b = new File(filenameOut);

        // Creates a new byte array to use as a buffer to read in the data.
        buf = new byte[BUF_SIZE];

        // CALCULATING THE MAX POSSIBLE SIZE OF ARRAY
        // gets the max possible integer array that can be stored, and the number of bounds
        // that will be needed to read in the whole file into memory.
        int[] runtimeCoeffs = getRunTimeCoefficients(length);

        // The number of times the files needs to be read in, essentially, how many
        // chunks do I need to split the file into.
        int divs;

        // This chunk of code is essentially for my testing purposes only, and is now mostly legacy
        // but I keep here for testing purposes.
        // Important points are that the integer array size is set given the coefficient given at runtime
        // and the number of divs required is set as well.

        // Initializes the two bounds to be zero.
        // These are used to determine the ranges of data selected for sorting at each read of the file.
        int boundLower;
        int boundUpper = 0;
        // This variable is used to hold the size of the bound (the range of values to be read in).
        // This will be the difference between bound upper and bound lower.
        int bound;

        if(runtimeCoeffs[0] <= 1){
            divs = 1;
            bound = (max-min);
        } else {
            divs = runtimeCoeffs[0];
            // divs over halves, since we need to read in negatives as well as positives,
            // so integer max represents only half the range.
            bound = Integer.MAX_VALUE/(divs/HALVES);
        }

        // only need to compare the first byte for the bounds, so
        // shift bound along to only contain the first 8 bits of the integer
        // perform the same shift on max and min
        bound = bound >> FIRST_BYTE_SHIFT;
        min = min >> FIRST_BYTE_SHIFT;
        max = max >> FIRST_BYTE_SHIFT;
        // Assigning the integer array to the max possible array size determined.
        ints = new int[runtimeCoeffs[1]];

        System.out.println("l1: " + buf.length + " l2: " + ints.length);

        for (int j = 0; j < divs; j++) {
            // For each read in, calculate the new bounds to check for
            // and call the sorting algorithm.

            // calculates the bounds with each iteration.
            if(j == 0) {
                boundLower = min;
                boundUpper = boundLower + bound;
            } else {
                boundLower = boundUpper+1;
                if(j == divs-1){
                    boundUpper = max;
                } else {
                    boundUpper = boundLower + bound -1;
                }
            }


            //calls the sort algorithm with the new bounds.
            if(boundLower == TRICKY_BOUND_VALUES[0]){
                boundUpper = TRICKY_BOUND_VALUES[1];
                sort(fis, fos, a, boundLower, boundUpper);
                sort(fis, fos, a, TRICKY_BOUND_VALUES[2], TRICKY_BOUND_VALUES[3]);
            } else {
                sort(fis, fos, a, boundLower, boundUpper);
            }

            // From experimental testing, I found that one bound always needed to be cut in half
            // and hence have accounted for this in the TRICKY_BOUND_VALUES array.
            // if this bound is detected, it splits the bound in half ,
            // to save it having to wait to detect that the bound si too large and then restart.



        }

        // Rename file b to a, so that all the sorted data is in file a.
        b.renameTo(a);


        //Alg finished, set large objects to null;
        buf = null; ints = null;
        fis = null;

    }

    // The sorting algorithm
    private static void sort(RandomAccessFile fis, RandomAccessFile fos, File a,
                             int boundLower, int boundUpper) {

        // This takes the input stream and the output stream variables, as well as the file a object
        // in order to create the input stream object, and the bounds of integers to be read in.

        // Printing out the bounds, for testing purposes.
        System.out.println("b1: " + boundLower + "\tb2: " + boundUpper);

        // Index will hold the current position in the integer array, so that I know
        // which index to write to next, and how much of the array has been used.
        int index = 0;

        boolean skip = false; // this will be set to true in the case that the bound was not big enoungh
        // and the algorithm has to run itself recursively in smaller bounds.
        // in this case, the file writing will be done by the recursively called
        // functions, and hence can be skipped in the outer function.

        try {
            // Initialises the input stream object (which I am now using a random access file object for)
            fis = new RandomAccessFile(a, "r");

            // Read in the desired vales to be sorted into the array, and return the number of values read.
            index = readFromBuffer(a, boundLower, boundUpper);

            fis.close();

        } catch (IOException e) {
            e.printStackTrace();

        } catch (IndexOutOfBoundsException iob) {
            // here the range was not big enough, so just try running it again on half the bound, until it works
            // skip is now true, as writing will be handled by the recursions of sort.
            skip = true;
            System.out.println("bound halved");
            // calculates half the current bound size.
            int modBound = (boundUpper-boundLower)/HALVES;

            // recalls the sort algorithm, splitting the current bound into two.
            sort(fis, fos, a, boundLower, boundLower+modBound);
            sort(fis, fos, a,boundLower+modBound+1, boundUpper);
        }


        if (!skip) {
            // runs if recursion was not required (and hence data has not been sorted in
            // recursive calls of the function).

            if (index > 0) {
                // if there are not zero elements in the current bound.
                // Create a new sorter object and run the sorting algorithm on it directly, passing in
                // the start and end point in the array to be sorted.
                ForkJoinQSort sorter = new ForkJoinQSort();
                sorter.run(ints, 0, index - 1);
            }


            // write out the sorted array to the output file.
            writeToBuffer(index, fos);
        }
    }

    private static int[] getRunTimeCoefficients(long length){
        // Get the current runtime environment
        // and hence calculate the max size of integer array allocatable,
        // and how many chunks the file needs to be split into to fit into that array.
        Runtime r = Runtime.getRuntime();
        double free = r.freeMemory()*R;
        int divs = (int)Math.ceil(length/(free));
        int arrSize = (int)Math.floor(free/4);

        return new int[]{divs, arrSize};
    }

    private static int readFromBuffer(File a, int boundLower, int boundUpper) throws IOException{

        try {
            // assigned the random access file to read from file a
            fis = new RandomAccessFile(a, "r");
        } catch (IOException e){
            e.printStackTrace();
        }

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

        // the first byte of each integer that is read in.
        byte firstPos;

        // The loop condition reads in the next buffer of bytes from the file, until there are no
        // more bytes to be read in.
        while ((n = fis.read(buf)) != READ_FINISHED) {
            intsRead = n / BYTES_TO_INTS;

            // iterate through all the ints currently being held in the byte buffer
            // if they fall in the given bound, add them to the int array to be sorted.
            for (int i = 0; i < intsRead; i++) {
                pos = BYTES_TO_INTS * i;

                firstPos = buf[pos];
                // check if the current value fits in the given bound, and if so add it to the array.
                if(boundLower <= firstPos && firstPos <= boundUpper) {

                    // Convert the next set of 4 bytes to an integer.
                    x = (((int) firstPos) & 255) << 24
                            | ((((int) buf[pos + 1]) & 255) << 16)
                            | ((((int) buf[pos + 2]) & 255) << 8)
                            | ((((int) buf[pos + 3]) & 255));

                    // add the value to the array and increment x.
                    ints[index] = x;
                    index++;
                }

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


