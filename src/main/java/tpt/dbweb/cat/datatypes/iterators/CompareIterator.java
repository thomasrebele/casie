package tpt.dbweb.cat.datatypes.iterators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javatools.datatypes.PeekIterator;
import tpt.dbweb.cat.datatypes.EntityMention;
import tpt.dbweb.cat.datatypes.iterators.EntityMentionPosIterator.PosType;
import tpt.dbweb.cat.tools.Utility;

/**
 * Iterate over entity mentions of two tagged texts at the same time.
 * It will traverse the text while stopping at boundaries of an entity mention of either text.
 * The boundaries with same position will be grouped together.
 * A ComparePair object will indicate information about the current position.
 *
 * The first compare pair has start 0. The 'end' attribute of the compare pairs will indicate the position of the boundaries.
 * The compare pairs represent consecutive text spans. The 'start' attribute of each successor is the same as the 'end' of its predecessor.
 * The last compare pair ends at the text length.
 *
 * i is index of entity mention list passed to constructor.
 * @author Thomas Rebele
 */
public class CompareIterator extends PeekIterator<ComparePair> {

  private final static Logger log = LoggerFactory.getLogger(CompareIterator.class);

  /**
   * Stores the current position within the text
   */
  int actPos = -1;

  /**
   * Stores the position within the text of the last iteration. Used for error checking.
   */
  int lastPos = 0;

  List<List<EntityMention>> ems = new ArrayList<>();

  String textContent = null, docid = null;

  int textLength = Integer.MAX_VALUE;

  /**
   * iterator for start/end positions
   */
  List<PeekIterator<EntityMentionPos>> posIt = new ArrayList<>();

  List<List<EntityMentionPos>> actEmps = new ArrayList<>();

  List<List<EntityMention>> openEntityMentions = new ArrayList<>();

  @SafeVarargs
  public CompareIterator(String text, String docid, List<EntityMention>... ems) {
    this(text, docid, Arrays.asList(ems));
  }

  public CompareIterator(String text, String docid, List<List<EntityMention>> ems) {
    this.ems = ems;
    for (int i = 0; i < ems.size(); i++) {
      ems.get(i).sort(null);
      posIt.add(Utility.peekIterator(new EntityMentionPosIterator(ems.get(i).iterator())));

      openEntityMentions.add(new ArrayList<>());
      actEmps.add(new ArrayList<>());
    }

    this.textContent = text;
    if (text != null) {
      textLength = textContent.length();
    }
    this.docid = docid;
  }

  ComparePair next = null;

  @Override
  protected ComparePair internalNext() throws Exception {
    // iterate over positions until end of text
    if (actPos >= textLength) {
      return null;
    }

    // collect all emps after position pos
    int newActPos = Integer.MAX_VALUE;
    for (int i = 0; i < ems.size(); i++) {
      collectEmps(i);
      EntityMentionPos emp = actEmps.get(i).get(0);
      // lower position, as both might not be valid for actual position
      newActPos = Math.min(newActPos, emp.posType == PosType.INVALID ? textLength : emp.pos);
    }
    actPos = newActPos;

    // should never happen
    if (actPos < lastPos) {
      String text = textContent.substring(actPos, lastPos);
      log.warn("Warning: pos was wrong in doc " + docid + ", lastpos " + lastPos + " pos " + actPos + " text '" + text + "'");
      actPos = lastPos;
    }

    ComparePair pair = new ComparePair(lastPos, actPos, ems.size());

    for (int i = 0; i < ems.size(); i++) {
      updateComparePair(pair, i);
    }

    lastPos = actPos;

    log.trace("try next returns {}", pair);
    return pair;
  }

  /**
   * Helper to get next entity mention pos
   * @param i
   * @return
   */
  private EntityMentionPos nextPos(int i) {
    return Utility.next(posIt.get(i), EntityMentionPosIterator.getFinalPos());
  }

  /**
   * Helper to peek next entity mention pos
   * @param i
   * @return
   */

  private EntityMentionPos peekPos(int i) {
    return Utility.peek(posIt.get(i), EntityMentionPosIterator.getFinalPos());
  }

  /**
   * Collect all entity mention pos with same position occurring after actPos.
   * Post-condition: all entity mention pos in actEmps.get(i) have same position.
   * @param i
   */
  private void collectEmps(int i) {
    EntityMentionPos thisPos = null;
    // check if we need to clear actEmps.get(i)
    if (actEmps.get(i).size() > 0) {
      thisPos = actEmps.get(i).get(0);
      if (thisPos.pos <= actPos) {
        actEmps.get(i).clear();
      }
    }
    if (actEmps.get(i).size() == 0) {
      // find next position
      thisPos = peekPos(i);
      // add all entity mention pos with same position
      while (peekPos(i).pos == thisPos.pos) {
        EntityMentionPos np = nextPos(i);
        actEmps.get(i).add(np);
        // in every case stop at end of text
        if (np.pos >= textLength) {
          break;
        }
      }
      actEmps.get(i).sort(null);
    }
  }

  /**
   * Update information of the compare pair
   * @param pair
   * @param i indicates whether to update respective to the first or second list of entity mentions.
   */
  public void updateComparePair(ComparePair pair, int i) {
    // track beginning entity mentions
    if (actEmps.get(i).get(0).pos == actPos) {
      for (EntityMentionPos actPos : actEmps.get(i)) {
        if (actPos.posType == PosType.START) {
          openEntityMentions.get(i).add(actPos.em);
        }
      }
    }

    // update entity mention pos of comparison pair
    List<EntityMentionPos> dstEmps = pair.emps.get(i);
    for (EntityMention em : openEntityMentions.get(i)) {
      dstEmps.add(new EntityMentionPos(actPos, em));
    }

    // track closing entity mentions
    if (actEmps.get(i).get(0).pos == actPos) {
      for (EntityMentionPos actPos : actEmps.get(i)) {
        if (actPos.posType == PosType.END) {
          openEntityMentions.get(i).remove(actPos.em);
        }
      }
    }
  }

  public static void main(String[] args) {
    /*String text0 = "first <mark entity='1'>second <mark entity='2'>third</mark></mark> end";
    String text1 = "first <mark entity='1'>second <mark entity='2'>third</mark></mark> end";
    TaggedText tt0 = new TaggedTextXMLReader().getFirstTaggedTextFromString(text0);
    TaggedText tt1 = new TaggedTextXMLReader().getFirstTaggedTextFromString(text1);
    
    for (ComparePair p : Utility.iterable(new CompareIterator(tt0.text, "test", tt0.mentions, tt1.mentions))) {
      System.out.println(p);
      System.out.println(p.emps.get(0));
      System.out.println(p.emps.get(1));
    }*/
  }

}