package synth.core;

import java.util.Objects;

public class Example {
    /**
     * Input: mapping from variable names to their values
     */
    private final Environment input;
    /**
     * Output value
     */
    private final int output;

    public Example(Environment input, int output) {
        this.input = input;
        this.output = output;
    }

    public Environment input() {
        return input;
    }

    public int output() {
        return output;
    }

    @Override
    public int hashCode() {
        return input.hashCode() ^ output;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Example))
            return false;
        Example other = (Example) o;
        return Objects.equals(input, other.input) && output == other.output;
    }

    @Override
    public String toString() {
        return String.format("x=%d, y=%d, z=%d -> %s", input.x(), input.y(), input.z(), output);
    }
}
