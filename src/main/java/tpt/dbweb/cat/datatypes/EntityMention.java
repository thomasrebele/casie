/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package tpt.dbweb.cat.datatypes;

/**
 * An entity mention is a TextSpan (part of the text) which is assigned to an entity.
 * Additionally it has a <code>min</code> attribute, for specifying the essential part of the mention.
 *
 * @author Thomas Rebele
 *
 */
public class EntityMention extends TextSpan implements Cloneable {

  public EntityMention(String text, int start, int end, String entity) {
    super(text, start, end);
    this.entity = entity;
  }

  public EntityMention(EntityMention em) {
    super(em);
    this.entity = em.entity;
    this.min = em.min;
  }

  public String entity;

  public TextSpan min = null;

  public EntityMention referring = null;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof EntityMention) {
      EntityMention em = (EntityMention) o;

      boolean result = super.equals(o);
      result &= entity == null ? em.entity == null : entity.equals(em.entity);
      return result;
    }
    return false;
  }

  @Override
  public String toString() {
    String result = super.toString() + "(" + start + "-" + end;
    if (min != null) {
      result += ", " + min;
    }
    if (referring != null) {
      result += ", refers to " + referring;
    }
    result += "): " + entity;
    return result;
  }

  public String getMention() {
    return super.spanString();
  }

  public EntityMention getMinMention() {
    if (min == null) {
      return this;
    }
    EntityMention result = new EntityMention(this.text, min.start, min.end, entity);
    result.min = min;
    result.infoMap = infoMap;
    return result;
  }

  @Override
  public EntityMention clone() throws CloneNotSupportedException {
    return (EntityMention) super.clone();
  }
}