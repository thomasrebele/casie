package tpt.dbweb.cat.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.BinaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javatools.datatypes.PeekIterator;
import tpt.dbweb.cat.datatypes.EntityMention;
import tpt.dbweb.cat.datatypes.TaggedText;
import tpt.dbweb.cat.datatypes.iterators.CompareIterator;
import tpt.dbweb.cat.datatypes.iterators.ComparePair;

/**
 * Collection of methods for finding similar entity mention chains between tagged texts.
 * The functions return a mapping from the entities of the first tagged text to those of the second.
 *
 * @author Thomas Rebele
 */
public class MentionChainAligner {

  private final static Logger log = LoggerFactory.getLogger(MentionChainAligner.class);

  public Map<String, String> guessEntityMapFirst(TaggedText tt0, TaggedText tt1) {
    Map<String, String> result = new HashMap<>();

    CompareIterator ci = new CompareIterator(tt0.text, tt0.id, tt0.mentions, tt1.mentions);
    for (ComparePair cp : Utility.iterable(ci)) {
      EntityMention em0 = cp.getPrincipalMention(0);
      EntityMention em1 = cp.getPrincipalMention(1);

      if (em0 != null && em0.entity != null && em1 != null && em1.entity != null) {
        result.put(em0.entity, em1.entity);
      }
    }
    return result;
  }

  /**
   * Calculate a mapping from the entity in list0 to those in list1.
   * Some corpora have only generic entity ids like newstext42-E01.
   * This method helps to evaluate precision and recall for them.
   * @param list0
   * @param list1
   * @return
   */
  public Map<String, String> guessEntityMapGreedy(TaggedText tt0, TaggedText tt1) {
    // map from tt0 entity to (map of tt1 entity to count)
    Map<String, Map<String, Integer>> possibilitiesCount = getEntityMapPosibilitiesCount(tt0, tt1);

    // transform to map from count to (map of tt0 entty to tt1 entity)
    TreeMap<Integer, Map<String, String>> reverse = new TreeMap<>();
    for (Entry<String, Map<String, Integer>> e : possibilitiesCount.entrySet()) {
      for (Entry<String, Integer> e2 : e.getValue().entrySet()) {
        reverse.compute(e2.getValue(), (key, map) -> {
          if (map == null) {
            map = new HashMap<>();
          }
          map.put(e.getKey(), e2.getKey());
          return map;
        });
      }
    }

    Map<String, String> result = new HashMap<>();
    for (Integer i : reverse.descendingKeySet()) {
      for (Entry<String, String> re : reverse.get(i).entrySet()) {
        result.putIfAbsent(re.getKey(), re.getValue());
      }
    }

    return result;
  }

  private Map<String, Map<String, Integer>> getEntityMapPosibilitiesCount(TaggedText tt0, TaggedText tt1) {
    Map<String, Map<String, Integer>> assignmentsCount = new HashMap<>();

    CompareIterator ci = new CompareIterator(tt0.text, tt0.id, tt0.mentions, tt1.mentions);
    for (ComparePair cp : Utility.iterable(ci)) {
      EntityMention em0 = cp.getPrincipalMention(0);
      EntityMention em1 = cp.getPrincipalMention(1);

      if (em0 != null && em0.entity != null && em1 != null && em1.entity != null) {
        assignmentsCount.compute(em0.entity, (tt0entity, map) -> {
          if (map == null) {
            map = new HashMap<String, Integer>();
          }
          map.compute(em1.entity, (tt1entity, counter) -> {
            if (counter == null) {
              counter = 0;
            }
            return ++counter;
          });
          return map;
        });
      }
    }
    return assignmentsCount;
  }

  /**
   * Get all possible entity maps between two tagged texts. Key and value entities have at least one mention in common.
   * @param tt0
   * @param tt1
   * @param removeSquareOfMax truncate a mapping of entity0 to entity1 if they don't have (relatively speaking) enough mentions in common.
   * @return
   */
  public Iterator<Map<String, String>> getPossibleEntityMaps(TaggedText tt0, TaggedText tt1, boolean removeSquareOfMax) {
    Map<String, Map<String, Integer>> possiblitiesCount = getEntityMapPosibilitiesCount(tt0, tt1);
    //log.error("{}", possiblitiesCount);

    TreeMap<String, Iterator<String>> iterators = new TreeMap<>();

    // populate iterators
    Map<String, String> act = new HashMap<>();
    int size = 1;
    for (Entry<String, Map<String, Integer>> entry : possiblitiesCount.entrySet()) {
      if (entry.getValue().size() > 0) {
        int limit = 0;
        if (removeSquareOfMax) {
          int max = entry.getValue().values().stream().reduce(0, BinaryOperator.maxBy(Integer::compare));
          limit = (int) Math.floor(Math.sqrt(max));
        }

        ArrayList<String> values = new ArrayList<>();
        for (Entry<String, Integer> subEntry : entry.getValue().entrySet()) {
          if (subEntry.getValue() >= limit) {
            values.add(subEntry.getKey());
          }
        }

        // remove square of max
        Iterator<String> it = values.iterator();
        size *= values.size();
        if (it.hasNext()) {
          iterators.put(entry.getKey(), it);
          act.put(entry.getKey(), it.next());
        }
      }
    }
    if (size > 1000) {
      log.error("{}", possiblitiesCount);
    }

    log.error("size: {}, text: {}", size, tt0.id);

    return new PeekIterator<Map<String, String>>() {

      boolean globalFinished = false;

      @Override
      protected Map<String, String> internalNext() throws Exception {
        if (iterators.size() == 0 || globalFinished) {
          return null;
        }
        // iterate (similar to adding 1 to a decimal number)
        Map<String, String> result = new HashMap<>(act);
        boolean localFinished = true;
        for (Entry<String, Iterator<String>> entry : iterators.entrySet()) {

          if (entry.getValue().hasNext()) {
            act.put(entry.getKey(), entry.getValue().next());
            localFinished = false;
            break;
          }

          entry.setValue(possiblitiesCount.get(entry.getKey()).keySet().iterator());
          act.put(entry.getKey(), entry.getValue().next());

        }

        if (localFinished) {
          globalFinished = true;
        }

        return result;

      }
    };

  }

}
