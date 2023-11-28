package synth.algorithms.lia;

public record Term(String name, int value) implements Comparable<Term> {
    @Override
    public int compareTo(Term other) {
        int result = this.name.compareTo(other.name);
        if (result != 0) {
            return result;
        }
        return Integer.compare(this.value, other.value);
    }
}
