package tpt.dbweb.cat.datatypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tpt.dbweb.cat.tools.Utility;

/**
 * A text, where text spans can get tagged with entities.
 * @author Thomas Rebele
 */
public class TaggedText {

  // TODO: move this to infoMap
  public String id;

  public String text;

  public List<EntityMention> mentions = new ArrayList<>();

  /**
   * additional information (for example as additional attributes to the <code>&lt;mark ...&gt;</code> annotation)
   */
  public HashMap<String, String> infoMap;

  /**
   * Get infos map, to save additional information
   * @return
   */
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

  @Override
  public String toString() {
    return text + " {" + mentions + "}";
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof TaggedText) {
      TaggedText o = (TaggedText) other;
      boolean result = Utility.equals(id, o.id);
      result &= Utility.equals(text, o.text);
      result &= Utility.equals(mentions, o.mentions);
      return result;
    }
    return false;
  }
}