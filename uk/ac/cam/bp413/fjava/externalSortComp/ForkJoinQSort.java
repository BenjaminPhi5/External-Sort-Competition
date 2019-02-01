package uk.ac.cam.bp413.fjava.externalSortComp;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class ForkJoinQSort {

    // The array of values to be sorted
    private static int[] arr;
    // The threshold for creatign new threads.
    // If the range of values sorted on a given thread goes less than
    // this value, it will not invoke more threads.
    private static final int THRESH = 1500;
    // The pool object that holds all the threads.
    ForkJoinPool pool;

    // to allow the array to be assigned without using run,
    // so that for small arrays, stSort can be run directly.
    public void setArr(int[] a){
        arr = a;
    }

    public void run(int[] a, int start, int end){
        // This algorithm takes in an array to be sorted and the start and end position
        // in that array to sort through.

        // assigns the array to be sorted.
        arr = a;

        // Creates a new sorter object
        DPQsortAction sorter = new DPQsortAction(start, end);

        // Creates a new pool for threads, and sets it to run
        // the sorter object.
        pool = new ForkJoinPool();
        pool.invoke(sorter);
        // Waits for the pool to finish executing.
        pool.shutdown();

        // to save space since pool no longer needed.
        pool = null;

    }

    // The class that runs the quicksort algorithm.
    public class DPQsortAction extends RecursiveAction{

        int start; int end;

        DPQsortAction(int start, int end){
            // sets up the start and end values.
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            // This function runs the quicksort algorithm

            // p and q will hold the values of the two pointers,
            // at the start and end of the array.
            int p; int q; int temp;

            // gets the length of values to be sorted.
            int len = end-start;

            // if array is very small, use insertion sort.
            if (len < 27) {
                for (int i = start + 1; i <= end; i++) {
                    for (int j = i; j > start && arr[j] < arr[j - 1]; j--) {
                        temp = arr[j];arr[j] = arr[j-1];arr[j-1] = temp;
                    }
                }
                // sorting is done so return here.
                return;
            }

            // else double pivot quick sort

            // Check that the pivots are in order, of not swap them
            if(arr[start] > arr[end]){
                temp = arr[start];arr[start] = arr[end];arr[end] = temp;
            }

            // now assign p and q to the pivots.
            p= arr[start]; q = arr[end];

            // l is the lower pointer, g i the greater pointer.
            int l = start+1;
            int g = end-1;

            // iterate through the values while the current value pointer k is less than the greater pointer
            for(int k = l; k<= g; k++){

                // if element is less than left pointer
                if(arr[k] < p){
                    temp = arr[k];arr[k] = arr[l];arr[l] = temp;
                    l++;// pointer to less than left pointer moves up by one
                }

                // if element between left point and right pointer dont care
                // as already in right place

                // if element >= the right pivot.
                else if(arr[k] > q){

                    // may be able to move where the right pivot section starts first
                    while(k<g && arr[g] > q) {
                        g--;
                    }

                    // at this point, know arr[k] is greater than rp, so stick it behind where g points
                    temp = arr[k];arr[k] = arr[g];arr[g] = temp;
                    g--;
                    //subsequently move the >= rp sectin down by one

                    // check if new element at arr[k] the old g isnt now in wrong section (its currently in the between
                    // lp and rp section, but may infact be less than the rp
                    if(arr[k] < p){
                        temp = arr[k];arr[k] = arr[l];arr[l] = temp;
                        l++;
                    }
                }
            }
            // put pivots into positions
            temp = arr[l-1];arr[l-1] = arr[start];arr[start] = temp;
            temp = arr[g+1];arr[g+1] = arr[end];arr[end] = temp;

            //recursively call the quick sort now
            // if the range of values to be sorted was greater than the threshold, invoke new threads
            if(len >= THRESH){
                invokeAll(new DPQsortAction(start, l-2),
                        new DPQsortAction(l, g),
                        new DPQsortAction(g+2, end));
            }

            // else now complete the rest of the quick-sort on the current thread.
            else {

                stSort(start, l - 2);
                //System.out.println("after: " + j + ", " + g);
                stSort(l, g);
                stSort(g + 2, end);
            }

        }

    }

    public void stSort(int start, int end){

        int p; int q; int temp;

        int len = end-start;
        if (len < 27) { // insertion sort for tiny array
            for (int i = start + 1; i <= end; i++) {
                for (int j = i; j > start && arr[j] < arr[j - 1]; j--) {
                    temp = arr[j];arr[j] = arr[j-1];arr[j-1] = temp;
                }
            }
            return;
        }

        // else double pivot quick sort
        if(arr[start] > arr[end]){
            temp = arr[start];arr[start] = arr[end];arr[end] = temp;
        }

        p= arr[start]; q = arr[end];

        int l = start+1;
        int g = end-1;

        for(int k = l; k<= g; k++){

            // if element is less than left pointer
            if(arr[k] < p){
                temp = arr[k];arr[k] = arr[l];arr[l] = temp;
                l++;// pointer to less than left pointer moves up by one
            }

            // if element between left point and right pointer dont care
            // as already in right place

            // if element >= the right pivot.
            else if(arr[k] > q){

                // may be able to move where the right pivot section starts first
                while(k<g && arr[g] > q) {
                    g--;
                }

                // at this point, know arr[k] is greater than rp, so stick it behind where g points
                temp = arr[k];arr[k] = arr[g];arr[g] = temp;
                g--;
                //subsequently move the >= rp sectin down by one

                // check if new element at arr[k] the old g isnt now in wrong section (its currently in the between
                // lp and rp section, but may infact be less than the rp
                if(arr[k] < p){
                    temp = arr[k];arr[k] = arr[l];arr[l] = temp;
                    l++;
                }
            }
        }
        // put pivots into positions
        temp = arr[l-1];arr[l-1] = arr[start];arr[start] = temp;
        temp = arr[g+1];arr[g+1] = arr[end];arr[end] = temp;

        //recursively call the quick sort now
        //System.out.print("before: " + j + ", " + g);

        stSort(start, l-2);
        //System.out.println("after: " + j + ", " + g);
        stSort(l, g);
        stSort(g+2, end);

    }
}
