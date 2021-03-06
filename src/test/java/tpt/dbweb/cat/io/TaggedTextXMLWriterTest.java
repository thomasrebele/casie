/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package tpt.dbweb.cat.io;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import tpt.dbweb.cat.datatypes.EntityMention;
import tpt.dbweb.cat.datatypes.TaggedText;

public class TaggedTextXMLWriterTest {

  @Test
  public void test1() {

    TaggedText tt = new TaggedText();
    tt.text = "abc def ghi jkl mno pqr stu vwx";
    tt.mentions = Arrays.asList(new EntityMention(tt.text, 0, 3, "1"), new EntityMention(tt.text, 0, 7, "2"), new EntityMention(tt.text, 4, 7, "3"));

    String marked = TaggedTextXMLWriter.toMarkedText(tt);
    System.out.println(marked);

    String expected = "<mark entity='2'><mark entity='1'>abc</mark> <mark entity='3'>def</mark></mark> ghi jkl mno pqr stu vwx";
    assertEquals(expected, marked);
  }
}
