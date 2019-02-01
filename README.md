# External-Sort-Competition
This was my solution for a programming competition at the University of Cambridge Computer Laboratory, for which I came second! The challenge was to write code that sorted numerous sets of integers from different distributions in the fastest time, with a cap on the max heap size allowed and no nio packages to be used

The task was to write an algorithm, that given a large set of integers, up to around 40MB, sort them in the fastest possible time.
The task was in Java, and the max heap size was limited on the competition server (I'm not entirely sure what they limited it to, I think it was 10MB for the largest test files).

The code was to be run multiple times with different distributions of data, and the tiem given was the total time for sorting all the data sets.

The program is given a file with the unsorted data in, and a handle to one other file which can be used as temporary space. No other files can be used, and no nio classes.
The sorted values must then be stored back int he origional file.

It is called an external sort since the entire file in some cases does not fit in memeory.

My code employs two key methods of sorting. It sorts data that it detects having high repetitions with counting sort. 
Where data appears to be taken from a random data set ranging over integer max to integer min, counting sort cannot be used.
In this case, it uses a dual pivot, multi thread quick sort implementation. The threading uses a fork join pool metaphor, using built in java libraries, which is perhaps not the fastest but saves me from dealing with all the threading intricases myself, although if I had had the time, I would have liked to have done that part myself.

All IO is done using byte buffers, and values are converted to integers manually, as and when they are required, which is much faster than Javas built in input stream or buffer classes.

My solution uses no merging techniques, and uses the spare file only if nessesary.
I won't go into all the intricate details of what makes the sort fast in this external sortign context, but you can dive into the code yourself to work out what it does!

My solution came secod in the competition, which computer science students did over the summer between first and second year,
and sorted all the files given in around 8 seconds.
