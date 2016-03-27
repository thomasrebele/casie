package tpt.dbweb.cat.datatypes.iterators;

import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import tpt.dbweb.cat.Utility;

/**
 * This class converts an iterator to an out iterator,
 * where every element of the first iterator can have multiple elements.
 *
 * E.g. iterator over 1 2 3 4
 * function(x) returns iterator over x x x
 *
 * This class is then an iterator over 1 1 1 2 2 2 3 3 3 4 4 4
 *
 * @author Thomas Rebele
 *
 * @param <In>
 * @param <Out>
 */
class NestedIterator<In, Out> implements Iterator<Out> {

  final Iterator<In> it;

  final Function<In, Iterator<Out>> fn;

  Iterator<Out> actOutIt = null;

  public NestedIterator(Iterator<In> it, Function<In, Iterator<Out>> fn) {
    this.it = it;
    this.fn = fn;
  }

  @Override
  public boolean hasNext() {
    return (actOutIt != null && actOutIt.hasNext()) || it.hasNext();
  }

  @Override
  public Out next() {
    if (actOutIt != null && actOutIt.hasNext()) {
      return actOutIt.next();
    }
    if (!it.hasNext()) {
      return null;
    }

    actOutIt = fn.apply(it.next());
    return next();
  }

  /**
   * Usage example
   * @param args
   */
  public static void main(String... args) {

    Iterator<Integer> it1 = IntStream.range(0, 10).iterator();
    Function<Integer, Iterator<Double>> fn = i -> DoubleStream.iterate(0, f -> f + 0.1).limit(i).iterator();
    NestedIterator<Integer, Double> nestedIt = new NestedIterator<Integer, Double>(it1, fn);

    for (Double d : Utility.iterable(nestedIt)) {
      System.out.println(d);
    }
  }

}