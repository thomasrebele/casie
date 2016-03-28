package tpt.dbweb.cat.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import javatools.datatypes.PeekIterator;
import javatools.datatypes.PeekIterator.SimplePeekIterator;
import javatools.datatypes.Trie;

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

  public static String findLongestPrefix(String text, String search) {
    Trie t = new Trie();
    for (int i = 0; i < search.length(); i++) {
      t.add(search.substring(0, i));
    }

    int maxi = 0, maxlen = 0;
    for (int i = 0; i < text.length(); i++) {
      int len = t.containedLength(search, i);
      if (len > maxlen) {
        maxlen = len;
        maxi = i;
      }
    }

    return text.substring(maxi, maxi + maxlen);
  }

  public static <T> Set<T> addToSet(Set<T> set, T em) {
    if (set == null) {
      set = new HashSet<>();
    }
    set.add(em);
    return set;
  }

  public static String readResourceAsString(String src) throws IOException {
    InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(src);
    return IOUtils.toString(is);
  }

  public static int getCommonPrefixLength(String first, String second) {
    int i = 0;
    for (; i < Math.min(first.length(), second.length()); i++) {
      if (first.charAt(i) != second.charAt(i)) {
        break;
      }
    }
    return i;
  }

  /**
   * Check if string represents a Non-Matched-Entity
   * @param string
   * @return
   */
  public static boolean isNME(String string) {
    return Arrays.asList("--", "--OOKBE--", "--NME--").contains(string);
  }

}
