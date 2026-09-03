import java.util.Arrays;
import java.util.Random;

public class CutoffBench {
    public static void main(String[] args) {
        int[] cutoffs = new int[] {44, 45, 46, 47, 48, 49, 50, 51, 52};

        for (int cutoff : cutoffs) {
            double total = 0.0;
            for (int size : SAMPLE_SIZES) {
                for (int randomness : RANDOMNESS) {
                    int[] sample = generateSample(size, randomness);
                    total += execute(new QuickOptimized(true, true, cutoff)::sort, sample);
                }
            }
            System.out.printf("cutoff=%d total=%.6f%n", cutoff, total);
        }
    }

    private static final int[] SAMPLE_SIZES = new int[] {10, 30, 100, 300, 1000, 3000, 10000, 30000, 100000};
    private static final int[] RANDOMNESS = new int[] {100, 5, 0};

    public static int[] generateSample(int size, int randomness) {
        int[] sample = new int[size];

        Random random = new Random(12345678 * size);
        int previousElement = 0;
        for (int i = 0; i < size; i++) {
            if (random.nextInt(100) >= randomness) {
                int randomOffset = random.nextInt(3);
                int currentElement = previousElement + randomOffset;
                sample[i] = currentElement;
                previousElement = currentElement;
            } else {
                sample[i] = random.nextInt(size);
            }
        }

        return sample;
    }

    private static double execute(java.util.function.Consumer<int[]> algorithm, int[] input) {
        final long target = 10000000;
        final int MAX_LIVES = 3;
        int repetitions = 1;
        long runtime = Long.MAX_VALUE;
        int lives = MAX_LIVES;
        while (true) {
            int[][] inputs = new int[repetitions][];
            for (int i = 0; i < repetitions; i++) {
                inputs[i] = Arrays.copyOf(input, input.length);
            }
            System.gc();
            Thread.yield();

            long startTime = System.nanoTime();
            for (int i = 0; i < repetitions; i++) {
                algorithm.accept(inputs[i]);
            }
            long endTime = System.nanoTime();
            runtime = Math.min(runtime, endTime - startTime);

            if (repetitions == 1 && runtime >= 30 * target) {
                break;
            }
            if (runtime >= target) {
                if (lives == 0) break;
                lives--;
            } else {
                if (runtime == 0) {
                    repetitions *= 5;
                } else {
                    double factor = target / runtime;
                    if (factor < 2) factor = 2;
                    if (factor > 5) factor = 5;
                    repetitions *= factor;
                }
                runtime = Long.MAX_VALUE;
                lives = MAX_LIVES;
            }
        }
        return (double) runtime / ((double) repetitions * 1000000000);
    }
}

class QuickOptimized {
    public QuickOptimized(boolean shuffleFirst, boolean useMedianOfThree, int insertionSortCutoff) {
        this.shuffleFirst = shuffleFirst;
        this.useMedianOfThree = useMedianOfThree;
        this.insertionSortCutoff = insertionSortCutoff;
    }

    private final boolean shuffleFirst;
    private final boolean useMedianOfThree;
    private final int insertionSortCutoff;

    public void sort(int[] a) {
        if (shuffleFirst) {
            shuffle(a);
        }
        sort(a, 0, a.length - 1);
        assert Insertion.isSorted(a);
    }

    private void sort(int[] a, int lo, int hi) {
        if (hi <= lo) return;
        if (hi - lo + 1 <= insertionSortCutoff) {
            Insertion.sort(a, lo, hi);
            return;
        }
        int j = partition(a, lo, hi);
        sort(a, lo, j - 1);
        sort(a, j + 1, hi);
        assert Insertion.isSorted(a, lo, hi);
    }

    private int partition(int[] a, int lo, int hi) {
        if (useMedianOfThree) {
            int mid = lo + (hi - lo) / 2;
            int median = medianOfThree(a, lo, mid, hi);
            exchange(a, lo, median);
        }

        int i = lo;
        int j = hi + 1;
        int pivot = a[lo];

        while (a[++i] < pivot) {
            if (i == hi) {
                exchange(a, lo, hi);
                return hi;
            }
        }

        while (pivot < a[--j]) {
            if (j == lo + 1) {
                return lo;
            }
        }

        while (i < j) {
            exchange(a, i, j);
            while (a[++i] < pivot);
            while (pivot < a[--j]);
        }

        exchange(a, lo, j);
        return j;
    }

    private static void exchange(int[] a, int i, int j) {
        int swap = a[i];
        a[i] = a[j];
        a[j] = swap;
    }

    private static int medianOfThree(int[] a, int i, int j, int k) {
        boolean x = a[i] < a[j];
        boolean y = a[j] < a[k];
        boolean z = a[k] < a[i];
        if (x == y) return j;
        if (y == z) return k;
        return i;
    }

    private static void shuffle(int[] a) {
        for (int i = 0; i < a.length; i++) {
            int j = i + random.nextInt(a.length - i);
            exchange(a, i, j);
        }
    }

    private static final Random random = new Random(314159265);
}

class Insertion {
    public static final void sort(int[] a) {
        sort(a, 0, a.length - 1);
    }

    public static final void sort(int[] a, int lo, int hi) {
        for (int i = lo; i <= hi; i++) {
            int value = a[i];
            int j = i;
            while (j > lo && a[j - 1] > value) {
                a[j] = a[j - 1];
                j--;
            }
            a[j] = value;
        }
        assert(isSorted(a, lo, hi));
    }

    public static boolean isSorted(int[] a) {
        return isSorted(a, 0, a.length - 1);
    }

    public static boolean isSorted(int[] a, int lo, int hi) {
        for (int i = lo; i < hi; i++) {
            if (!(a[i + 1] >= a[i])) return false;
        }
        return true;
    }

    private Insertion() { }
}
