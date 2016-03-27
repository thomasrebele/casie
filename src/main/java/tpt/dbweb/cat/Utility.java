package tpt.dbweb.cat;

import java.util.Iterator;

import tpt.dbweb.cat.datatypes.iterators.PeekIterator;
import tpt.dbweb.cat.datatypes.iterators.PeekIterator.SimplePeekIterator;

public class Utility {

  public static boolean equals(Object o1, Object o2) {
    if (o1 == o2) {
      return true;
    }
    if (o1 != null) {
      return o1.equals(o2);
    }
    return false;
  }

  public static <T> Iterable<T> iterable(Iterator<T> iterator) {
    return new Iterable<T>() {

      @Override
      public Iterator<T> iterator() {
        return iterator;
      }
    };
  }

  public static <T> PeekIterator<T> peekIterator(Iterator<T> it) {
    return new SimplePeekIterator<T>(it);
  }

  public static <T> T next(Iterator<T> it, T finalValue) {
    if (it.hasNext()) {
      return it.next();
    }
    return finalValue;
  }

  public static <T> T peek(PeekIterator<T> it, T finalValue) {
    if (it.hasNext()) {
      return it.peek();
    }
    return finalValue;
  }

}
