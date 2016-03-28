package tpt.dbweb.cat.datatypes.iterators;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import tpt.dbweb.cat.datatypes.EntityMention;
import tpt.dbweb.cat.tools.Utility;

/**
 * Data class storing a view of a slice of two or more tagged texts.
 * It contains a list of entity mention positions for each text.
 *
 * @author Thomas Rebele
 */
public class ComparePair {

  /**
   * Stores entity mention positions that were encountered at position 'end'
   */
  public List<List<EntityMentionPos>> emps = null;

  public ComparePair(int start, int end) {
    this(start, end, 2);
  }

  public ComparePair(int start, int end, int numberOfTexts) {
    this.start = start;
    this.end = end;
    emps = new ArrayList<>();
    for (int i = 0; i < numberOfTexts; i++) {
      emps.add(new ArrayList<>());
    }
  }

  /**
   * Which part of the text is described by this compare pair.
   */
  public int start = 0, end = 0;

  @Override
  public String toString() {
    return "compared (" + start + "-" + end + ") emps sizes " + emps.get(0).size() + " " + emps.get(1).size() + " entity mention pos: " + getPos(0)
    + " --- " + getPos(1) + ";";
  }

  /**
   * Get top most / last encountered entity mention
   * @param listIndex index of entity mention list (i.e. 0 for left text, 1 for right text)
   * @return
   */
  public EntityMention getPrincipalMention(int listIndex) {
    if (emps == null || emps.size() <= listIndex) {
      return null;
    }
    List<EntityMentionPos> list = emps.get(listIndex);
    if (list == null || list.size() == 0) {
      return null;
    }
    for (int j = list.size() - 1; j >= 0; j--) {
      EntityMention em = list.get(j).em;
      if (em != null && em.start < end) {
        return em;
      }
    }
    return null;
  }

  /**
   * Get all entity mentions that are valid at end.
   * @param listIndex index of entity mention list (i.e. 0 for left text, 1 for right text)
   * @return
   */
  public List<EntityMention> getMentions(int listIndex) {
    if (emps == null || emps.size() <= listIndex) {
      return null;
    }
    List<EntityMentionPos> list = emps.get(listIndex);
    return list.stream().map(emp -> emp.em).collect(Collectors.toList());
  }

  /**
   * Get top most / last encountered entity mention position
   * @param listIndex index of entity mention list (i.e. 0 for left text, 1 for right text)
   * @return
   */
  public EntityMentionPos getPos(int listIndex) {
    if (emps == null || emps.size() <= listIndex) {
      return null;
    }
    List<EntityMentionPos> list = emps.get(listIndex);
    if (list == null || list.size() == 0) {
      return null;
    }
    return list.get(list.size() - 1);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof ComparePair) {
      ComparePair o = (ComparePair) other;
      boolean result = o.start == start;
      result &= o.end == end;
      result &= Utility.equals(o.emps, emps);
      return result;
    }
    return false;
  }
}