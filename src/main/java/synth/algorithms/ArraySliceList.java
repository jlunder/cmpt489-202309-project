package synth.algorithms;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ArraySliceList<T> implements List<T> {
    private T[] items;
    private int fromIndex;
    private int toIndex;

    ArraySliceList(T[] items, int fromIndex, int toIndex) {
        this.items = items;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    @Override
    public boolean add(T e) {
        throw new UnsupportedOperationException("Read-only array slice");
    }

    @Override
    public void add(int arg0, T arg1) {
        throw new UnsupportedOperationException("Read-only array slice");
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException("Read-only array slice");
    }

    @Override
    public boolean addAll(int arg0, Collection<? extends T> arg1) {
        throw new UnsupportedOperationException("Read-only array slice");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Read-only array slice");
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (var e : c) {
            if (!contains(e)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public T get(int index) {
        return items[fromIndex + index];
    }

    @Override
    public int indexOf(Object o) {
        for (int i = fromIndex; i < toIndex; ++i) {
            if (items[i] == o) {
                return i - fromIndex;
            }
        }
        return -1;
    }

    @Override
    public boolean isEmpty() {
        return fromIndex == toIndex;
    }

    @Override
    public Iterator<T> iterator() {
        return listIterator();
    }

    @Override
    public int lastIndexOf(Object o) {
        for (int i = toIndex; i > fromIndex;) {
            --i;
            if (items[i] == o) {
                return i - fromIndex;
            }
        }
        return -1;
    }

    @Override
    public ListIterator<T> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return new ListIterator<T>() {
            int i = fromIndex;

            @Override
            public boolean hasNext() {
                return i < toIndex;
            }

            @Override
            public void add(T e) {
                throw new UnsupportedOperationException("Read-only array slice");
            }

            @Override
            public boolean hasPrevious() {
                return i > fromIndex;
            }

            @Override
            public T next() {
                return items[i++];
            }

            @Override
            public int nextIndex() {
                return i + 1;
            }

            @Override
            public T previous() {
                return items[--i];
            }

            @Override
            public int previousIndex() {
                return i - 1 - fromIndex;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Read-only array slice");
            }

            @Override
            public void set(T e) {
                throw new UnsupportedOperationException("Read-only array slice");
            }
        };
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Read-only array slice");
    }

    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException("Read-only array slice");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Read-only array slice");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Read-only array slice");
    }

    @Override
    public T set(int arg0, T arg1) {
        throw new UnsupportedOperationException("Read-only array slice");
    }

    @Override
    public int size() {
        return toIndex - fromIndex;
    }

    @Override
    public List<T> subList(int from, int to) {
        return new ArraySliceList<T>(items, fromIndex + from, fromIndex + to);
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOfRange(items, fromIndex, toIndex);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> U[] toArray(U[] a) {
        return Arrays.copyOfRange((U[]) items, fromIndex, toIndex);
    }

}
