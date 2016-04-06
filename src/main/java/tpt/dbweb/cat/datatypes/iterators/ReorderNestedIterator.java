/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package tpt.dbweb.cat.datatypes.iterators;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import tpt.dbweb.cat.tools.Utility;

/**
 * Applies a function returning an iterator to elements of an ordered (!) iterator, and combines them to an ordered out iterator.
 * Every element of the first iterator can have multiple elements.
 *
 *
 * E.g. iterator over 1 2 3 4
 * function(x) returns iterator over x x+1 x+2
 *
 * This class is then an iterator over 1 2 2 3 3 3 4 4 4 5 5 6
 *
 * @author Thomas Rebele
 *
 * @param <In>
 * @param <Out>
 */
class ReorderNestedIterator<In, Out extends Comparable<Out>> implements Iterator<Out> {

  int firstCount = 0;

  class OutWithIterator {

    boolean first = true;

    Out element;

    Iterator<Out> outIt;
  }

  final Iterator<In> it;

  final Function<In, Iterator<Out>> fn;

  final Comparator<Out> cmp;

  PriorityQueue<OutWithIterator> queue;

  public ReorderNestedIterator(Iterator<In> it, Function<In, Iterator<Out>> fn, Comparator<Out> cmp) {
    this.it = it;
    this.fn = fn;
    this.cmp = cmp;
    this.queue = new PriorityQueue<>(
        (out1, out2) -> cmp == null ? ((Comparable<Out>) out1.element).compareTo((out2.element)) : cmp.compare(out1.element, out2.element));
  }

  @Override
  public boolean hasNext() {
    return queue.size() > 0 || it.hasNext();
  }

  private void transferInputIterator() {
    if (firstCount == 0 && it.hasNext()) {
      OutWithIterator owi = new OutWithIterator();
      owi.outIt = fn.apply(it.next());
      if (owi.outIt.hasNext()) {
        owi.element = owi.outIt.next();
        owi.first = true;
        firstCount++;
        queue.add(owi);
      } else {
        transferInputIterator();
      }
    }
  }

  @Override
  public Out next() {
    transferInputIterator();

    Out result = null;
    if (queue != null && queue.size() > 0) {
      OutWithIterator owi = queue.poll();
      result = owi.element;
      if (owi.first) {
        firstCount--;
        owi.first = false;
      }

      if (owi.outIt.hasNext()) {
        owi.element = owi.outIt.next();
        queue.add(owi);
      }
    }

    return result;
  }

  /**
   * Usage example
   * @param args
   */
  public static void main(String args[]) {

    Iterator<Integer> it1 = IntStream.range(0, 10).iterator();
    Function<Integer, Iterator<Double>> fn = i -> DoubleStream.iterate(0, f -> f + 0.1).limit(i).iterator();
    ReorderNestedIterator<Integer, Double> nestedIt = new ReorderNestedIterator<Integer, Double>(it1, fn, null);

    for (Double d : Utility.iterable(nestedIt)) {
      System.out.println(d);
    }
  }

}