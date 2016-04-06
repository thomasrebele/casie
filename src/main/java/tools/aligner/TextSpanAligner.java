/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package tools.aligner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import tools.aligner.DiffMatchPatch.Diff;
import tools.aligner.DiffMatchPatch.Operation;
import tpt.dbweb.cat.datatypes.TextSpan;

/**
 * Align a list of text spans to a similar string which has some characters removed or added.
 * Useful if a tool or input format chances the text from which the text spans were calculated.
 *
 * See the main method for an usage example.
 * @author Thomas Rebele
 *
 */
public class TextSpanAligner<T extends TextSpan> {

  /** save how much each position in srcText should be moved to arrive at corresponding position in dest */
  TreeMap<Integer, Integer> posToShift = new TreeMap<>();

  String dst;

  public TextSpanAligner(String src, String dst) {

    DiffMatchPatch dmp = new DiffMatchPatch();
    // use google-diff-match-patch to find differences in the text
    int accumulatedAdjust = 0, pos = 0;
    for (Diff d : dmp.diff_main(src, dst)) {
      // operations are relative to src string
      if (d.operation == Operation.DELETE) {
        accumulatedAdjust -= d.text.length();
        pos += d.text.length();
      } else if (d.operation == Operation.EQUAL) {
        pos += d.text.length();
      } else if (d.operation == Operation.INSERT) {
        accumulatedAdjust += d.text.length();
      }
      posToShift.put(pos, accumulatedAdjust);
    }
    if (posToShift.size() > 0) {
      int firstShift = posToShift.ceilingEntry(0).getValue();
      if (firstShift < 0) {
        posToShift.put(0, firstShift);
      }
    }

    this.dst = dst;
  }

  /**
   * Align a text span according to the src and dst text which were given to the constructor.
   * @param tr
   * @return
   */
  public T align(T tr) {
    if (tr == null) return null;
    TextSpan clone = null;
    try {
      clone = tr.clone();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
    @SuppressWarnings("unchecked")
    T result = (T) clone;
    Entry<Integer, Integer> entry;
    // adjust start
    entry = posToShift.floorEntry(result.start);
    if (entry != null) {
      Integer shiftStart = entry.getValue();
      if (shiftStart != null) {
        result.start += shiftStart;
      }
    }
    // adjust end
    entry = posToShift.floorEntry(result.end - 1);
    if (entry != null) {
      Integer shiftEnd = entry.getValue();
      if (shiftEnd != null) {
        result.end += shiftEnd;
      }
    }
    // update text and add to result
    result.text = dst;
    return result;
  }

  /**
   * See class description.
   * @param src
   * @param dest
   * @return
   */
  public List<T> align(List<T> src) {
    if (src == null || src.size() == 0) return src;
    List<T> result = new ArrayList<T>();

    for (T tr : src) {
      result.add(align(tr));
    }

    return result;
  }

  public static void main(String[] args) {
    // string which is used to create the TextSpan objects
    String str1 = "\n abc def ghi";
    // string, which the TextSpan objects should be algined to
    String str2 = "abc  def  ghi";

    // create text spans
    List<TextSpan> em = Arrays.asList(new TextSpan(str1, 2, 5), new TextSpan(str1, 6, 9), new TextSpan(str1, 10, 13), new TextSpan(str1, 6, 13),
        new TextSpan(str1, 2, 13));
    System.out.println(em);

    // now we change some characters and update the text spans
    em = new TextSpanAligner<TextSpan>(str1, str2).align(em);

    System.out.println(em);

    str1 = "President Obama\n announced to journalists this evening";
    str2 = "President Obama announced to journalists this evening ( Saturday )";

    em = Arrays.asList(new TextSpan(str1, 10, 15), new TextSpan(str1, str1.length() - 7, str1.length()));
    System.out.println(em);

    em = new TextSpanAligner<TextSpan>(str1, str2).align(em);
    System.out.println(em);
  }

}
