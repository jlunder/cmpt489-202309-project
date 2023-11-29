package synth.algorithms.lia;

import synth.core.Environment;

public class Term {
    private final String name;
    private final int xPower;
    private final int yPower;
    private final int zPower;

    public Term(int xPower, int yPower, int zPower) {
        assert xPower >= 0 && yPower >= 0 && zPower >= 0;
        if (xPower == 0 && yPower == 0 && zPower == 0) {
            this.name = "c";
        } else {
            this.name = "a_" + (xPower > 0 ? "x" + (xPower > 1 ? Integer.toString(xPower) : "") : "")
                    + (yPower > 0 ? "y" + (yPower > 1 ? Integer.toString(yPower) : "") : "")
                    + (zPower > 0 ? "z" + (zPower > 1 ? Integer.toString(zPower) : "") : "");
        }
        this.xPower = xPower;
        this.yPower = yPower;
        this.zPower = zPower;
    }

    public String name() {
        return this.name;
    }

    public int evalTerm(Environment env) {
        int result = 1;
        int x = env.x();
        for (int i = 0; i < xPower; ++i)
            result *= x;
        int y = env.y();
        for (int i = 0; i < yPower; ++i)
            result *= y;
        int z = env.z();
        for (int i = 0; i < zPower; ++i)
            result *= z;
        return result;
    }
}
