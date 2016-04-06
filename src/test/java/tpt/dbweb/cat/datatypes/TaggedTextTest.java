/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package tpt.dbweb.cat.datatypes;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

public class TaggedTextTest {

  public static void assertEquality(TaggedText expected, TaggedText actual) {
    assertEquals(expected.id, actual.id);
    assertEquals(expected.text, actual.text);

    if (expected.mentions == null || actual.mentions == null) {
      assertEquals(expected.mentions, actual.mentions);
    }

    Iterator<EntityMention> expectedIt = expected.mentions.iterator();
    Iterator<EntityMention> actualIt = actual.mentions.iterator();

    while (expectedIt.hasNext() && actualIt.hasNext()) {
      assertEquals(expectedIt.next(), actualIt.next());
    }

    while (expectedIt.hasNext() || actualIt.hasNext()) {
      EntityMention exp = expectedIt.hasNext() ? expectedIt.next() : null;
      EntityMention act = actualIt.hasNext() ? actualIt.next() : null;
      assertEquals(exp, act);
    }

    assertEquals(expected, actual);
  }

}
