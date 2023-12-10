package synth.util;

public class Bits {
    public static int fillRight(int x) {
        int y = x;
        y |= y >>> 1;
        y |= y >>> 2;
        y |= y >>> 4;
        y |= y >>> 8;
        y |= y >>> 16;
        return y;
    }

    public static int nextPower2(int x) {
        return fillRight(x - 1) + 1;
    }
}
