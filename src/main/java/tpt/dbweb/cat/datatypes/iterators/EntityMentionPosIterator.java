package tpt.dbweb.cat.datatypes.iterators;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import tpt.dbweb.cat.datatypes.EntityMention;

/**
 * Iterates over positions (start, end) of an entity mention iterator.
 * The input iterator in the natural entity mention order give back an iterator which iterates over all positions sequentially.
 * @author Thomas Rebele
 */
public class EntityMentionPosIterator extends ReorderNestedIterator<EntityMention, EntityMentionPos> {

  public enum PosType {
    START, INTERMEDIATE, END, INVALID
  }

  /**
   * @see EntityMentionPosIterator
   * @param it ordered iterator
   */
  public EntityMentionPosIterator(Iterable<EntityMention> it) {
    this(it.iterator());
  }

  /**
   * @see EntityMentionPosIterator
   * @param it ordered iterator
   */
  public EntityMentionPosIterator(Iterator<EntityMention> it) {
    super(it, em -> {
      return new Iterator<EntityMentionPos>() {

        EntityMention localEM = em;

        PosType posType = PosType.START;

        @Override
        public boolean hasNext() {
          return localEM != null;
        }

        @Override
        public EntityMentionPos next() {
          if (em == null) {
            return null;
          }
          if (posType == PosType.START) {
            posType = PosType.END;
            EntityMentionPos emm = new EntityMentionPos();
            emm.em = em;
            emm.pos = em.start;
            emm.posType = PosType.START;
            return emm;
          } else {

            EntityMentionPos emm = new EntityMentionPos();
            emm.em = em;
            emm.pos = em.end;
            emm.posType = PosType.END;
            localEM = null;
            return emm;
          }
        }
      };
    } , null);
  }

  @Override
  public EntityMentionPos next() {
    if (hasNext()) {
      return super.next();
    } else {
      return getFinalPos();
    }
  }

  public static EntityMentionPos getFinalPos() {
    EntityMentionPos emp = new EntityMentionPos();
    emp.pos = Integer.MAX_VALUE;
    emp.posType = PosType.INVALID;
    emp.em = null;
    return emp;

  }

  public static void main(String[] args) {
    String str = "Lorem ipsum doloret";
    List<EntityMention> em1 = Arrays.asList(new EntityMention(str, 0, 10, "test"), new EntityMention(str, 2, 8, "test2"),
        new EntityMention(str, 3, 9, "test3"));

    EntityMentionPosIterator emi = new EntityMentionPosIterator(em1.iterator());

    while (emi.hasNext()) {
      System.out.println(emi.next());
    }
  }
}