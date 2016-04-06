/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package tpt.dbweb.cat.datatypes;

import java.util.HashMap;
import java.util.Map;

public class TextSpan implements Comparable<TextSpan>, Cloneable {

  public TextSpan() {

  }

  public TextSpan(String text, int start, int end) {
    this.start = start;
    this.end = end;
    this.text = text;
  }

  public TextSpan(TextSpan span) {
    this.text = span.text;
    this.start = span.start;
    this.end = span.end;

    this.infoMap = span.infoMap == null ? null : new HashMap<>(span.infoMap);
  }

  public String text;

  public int start, end;

  /**
   * additional information (for example as additional attributes to the <code>&lt;mark ...&gt;</code> annotation)
   */
  public HashMap<String, String> infoMap;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 17;
    result = prime * result + end;
    result = prime * result + start;
    result = prime * result + ((text == null) ? 0 : text.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null || !(other instanceof TextSpan)) return false;
    TextSpan o = (TextSpan) other;
    boolean textEq = text == null ? text == o.text : text.equals(o.text);
    boolean startEq = start == o.start;
    boolean endEq = end == o.end;
    return textEq && startEq && endEq;
  }

  @Override
  public int compareTo(TextSpan o) {
    if (o == null) return -1;
    int startCmp = Integer.compare(start, o.start);
    return startCmp != 0 ? startCmp : -Integer.compare(end, o.end);
  }

  @Override
  public String toString() {
    String str = spanString();
    str = str == null ? null : str.replace("\n", " \u21B5 ");
    if (info(false) != null) str = str + " " + info();
    return str;
  }

  public String spanString() {
    if (start < 0) return "(" + start + "-" + end + ", invalid)";
    if (text != null && end > text.length()) return "(" + start + "-" + end + ", invalid)";
    if (end < start) return "(" + start + "-" + end + ", invalid)";
    return text == null ? "" : text.substring(start, end);

  }

  public boolean isSameSpan(TextSpan o) {
    if (o == null) return false;
    return start == o.start && end == o.end;
  }

  public boolean isOverlapping(TextSpan o) {
    if (o == null) return false;
    int upperStart = Math.max(start, o.start);
    int lowerEnd = Math.min(end, o.end);
    return lowerEnd - upperStart > 0;
  }

  public boolean contains(TextSpan o) {
    if (o == null) return false;
    return start <= o.start && end >= o.end;
  }

  public Map<String, String> info() {
    return info(true);
  }

  /**
   * Get infos map, to save additional information
   * @param create
   * @return
   */
  public Map<String, String> info(boolean create) {
    return infoMap == null ? infoMap = (create ? new HashMap<>(1) : null) : infoMap;
  }

  @SuppressWarnings("unchecked")
  @Override
  public TextSpan clone() throws CloneNotSupportedException {
    TextSpan result = (TextSpan) super.clone();
    if (result.infoMap != null) {
      result.infoMap = (HashMap<String, String>) result.infoMap.clone();
    }
    return result;
  }
}