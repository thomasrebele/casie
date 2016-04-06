/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package tpt.dbweb.cat.datatypes.iterators;

import tpt.dbweb.cat.datatypes.EntityMention;
import tpt.dbweb.cat.datatypes.iterators.EntityMentionPosIterator.PosType;

/**
 * Data class with entity mention and position. It also stores whether the position is at the start, in the middle, at the end or invalid.
 * @author Thomas Rebele
 */
public class EntityMentionPos implements Comparable<EntityMentionPos> {

  public int pos;

  public EntityMention em;

  public PosType posType;

  public EntityMentionPos() {
  }

  public EntityMentionPos(int pos, EntityMention em) {
    this.pos = pos;
    this.em = em;
    this.posType = PosType.INVALID;
    if (pos == em.start) {
      this.posType = PosType.START;
    } else if (pos == em.end) {
      this.posType = PosType.END;
    } else if (em.start < pos && pos < em.end) {
      this.posType = PosType.INTERMEDIATE;
    }
  }

  @Override
  public String toString() {
    return (em == null ? "null" : em.toString()) + " mark " + pos + " type " + posType;
  }

  @Override
  public int compareTo(EntityMentionPos o) {
    int cmp = Integer.compare(pos, o.pos);
    if (cmp != 0) {
      return cmp;
    }
    // TODO: check whether this applies to all cases
    return em.compareTo(o.em);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof EntityMentionPos) {
      EntityMentionPos empos = (EntityMentionPos) o;
      boolean result = pos == empos.pos;
      result &= posType == empos.posType;
      result &= em == null ? empos.em == null : em.equals(empos.em);
      return result;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int val = 17;
    val = val * 31 + pos;
    val = val * 31 + em.hashCode();
    val = val * 31 + posType.hashCode();
    return val;
  }
}