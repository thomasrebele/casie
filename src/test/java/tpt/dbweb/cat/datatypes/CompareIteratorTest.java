package tpt.dbweb.cat.datatypes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import tpt.dbweb.cat.datatypes.TaggedText;
import tpt.dbweb.cat.datatypes.iterators.CompareIterator;
import tpt.dbweb.cat.datatypes.iterators.ComparePair;
import tpt.dbweb.cat.datatypes.iterators.EntityMentionPos;
import tpt.dbweb.cat.io.TaggedTextXMLReader;

public class CompareIteratorTest {

  private void assertComparePair(ComparePair expected, ComparePair actual) {
    assertEquals(expected.start, actual.start);
    assertEquals(expected.end, actual.end);
    assertEquals(expected.emps, actual.emps);
  }

  @Test
  public void test() {

    TaggedText first = new TaggedTextXMLReader().getFirstTaggedTextFromString(
        "<mark entity='Barack_Obama' min='president'><mark entity='Determiner'>The </mark>president of the <mark entity='United_States'>United States</mark></mark>. Another sentence.");
    TaggedText second = new TaggedTextXMLReader().getFirstTaggedTextFromString(
        "The <mark entity='Barack_Obama'>president</mark> of the <mark entity='United_States'>United States</mark>. Another sentence.");

    assertEquals(first.text, second.text);

    CompareIterator it = new CompareIterator(first.text, null, first.mentions, second.mentions);

    ComparePair expPair;
    expPair = new ComparePair(0, 0);
    expPair.emps.get(0).add(new EntityMentionPos(0, first.mentions.get(0)));
    expPair.emps.get(0).add(new EntityMentionPos(0, first.mentions.get(1)));
    assertComparePair(expPair, it.next());

    // 'The '
    expPair = new ComparePair(0, 4);
    expPair.emps.get(0).add(new EntityMentionPos(4, first.mentions.get(0)));
    expPair.emps.get(0).add(new EntityMentionPos(4, first.mentions.get(1)));
    expPair.emps.get(1).add(new EntityMentionPos(4, second.mentions.get(0)));
    assertComparePair(expPair, it.next());

    // 'president'
    expPair = new ComparePair(4, 13);
    expPair.emps.get(0).add(new EntityMentionPos(13, first.mentions.get(0)));
    expPair.emps.get(1).add(new EntityMentionPos(13, second.mentions.get(0)));
    assertComparePair(expPair, it.next());

    // ' of the '
    expPair = new ComparePair(13, 21);
    expPair.emps.get(0).add(new EntityMentionPos(21, first.mentions.get(0)));
    expPair.emps.get(0).add(new EntityMentionPos(21, first.mentions.get(2)));
    expPair.emps.get(1).add(new EntityMentionPos(21, second.mentions.get(1)));
    assertComparePair(expPair, it.next());

    // 'United states'
    expPair = new ComparePair(21, 34);
    expPair.emps.get(0).add(new EntityMentionPos(34, first.mentions.get(0)));
    expPair.emps.get(0).add(new EntityMentionPos(34, first.mentions.get(2)));
    expPair.emps.get(1).add(new EntityMentionPos(34, second.mentions.get(1)));
    assertComparePair(expPair, it.next());

    // '. Another sentence.'
    expPair = new ComparePair(34, first.text.length());
    assertComparePair(expPair, it.next());

    assertFalse("compare iterator has unexpected elements", it.hasNext());
    it.close();

  }

  @Test
  public void testMulti() {

    TaggedText tt0 = new TaggedTextXMLReader().getFirstTaggedTextFromString(
        "<mark entity='Barack_Obama' min='president'>The president of the <mark entity='United_States'>United States</mark></mark>. Another sentence.");
    TaggedText tt1 = new TaggedTextXMLReader().getFirstTaggedTextFromString(
        "The <mark entity='Barack_Obama'>president</mark> of the <mark entity='United_States'>United States</mark>. Another sentence.");
    TaggedText tt2 = new TaggedTextXMLReader()
        .getFirstTaggedTextFromString("The <mark entity='Barack_Obama'>president of the United States</mark>. Another sentence.");
    TaggedText tt3 = new TaggedTextXMLReader().getFirstTaggedTextFromString(
        "<mark entity='Barack_Obama'>The president</mark> of the <mark entity='United_States'>United States</mark>. Another sentence.");

    assertEquals(tt0.text, tt1.text);

    CompareIterator it = new CompareIterator(tt0.text, null, tt0.mentions, tt1.mentions, tt2.mentions, tt3.mentions);

    ComparePair expPair;
    expPair = new ComparePair(0, 0, 4);
    expPair.emps.get(0).add(new EntityMentionPos(0, tt0.mentions.get(0)));
    expPair.emps.get(3).add(new EntityMentionPos(0, tt3.mentions.get(0)));
    assertComparePair(expPair, it.next());

    // 'The '
    expPair = new ComparePair(0, 4, 4);
    expPair.emps.get(0).add(new EntityMentionPos(4, tt0.mentions.get(0)));
    expPair.emps.get(1).add(new EntityMentionPos(4, tt1.mentions.get(0)));
    expPair.emps.get(2).add(new EntityMentionPos(4, tt2.mentions.get(0)));
    expPair.emps.get(3).add(new EntityMentionPos(4, tt3.mentions.get(0)));
    assertComparePair(expPair, it.next());

    // 'president'
    expPair = new ComparePair(4, 13, 4);
    expPair.emps.get(0).add(new EntityMentionPos(13, tt0.mentions.get(0)));
    expPair.emps.get(1).add(new EntityMentionPos(13, tt1.mentions.get(0)));
    expPair.emps.get(2).add(new EntityMentionPos(13, tt2.mentions.get(0)));
    expPair.emps.get(3).add(new EntityMentionPos(13, tt3.mentions.get(0)));
    assertComparePair(expPair, it.next());

    // ' of the '
    expPair = new ComparePair(13, 21, 4);
    expPair.emps.get(0).add(new EntityMentionPos(21, tt0.mentions.get(0)));
    expPair.emps.get(0).add(new EntityMentionPos(21, tt0.mentions.get(1)));
    expPair.emps.get(1).add(new EntityMentionPos(21, tt1.mentions.get(1)));
    expPair.emps.get(2).add(new EntityMentionPos(21, tt2.mentions.get(0)));
    expPair.emps.get(3).add(new EntityMentionPos(21, tt3.mentions.get(1)));
    assertComparePair(expPair, it.next());

    // 'United states'
    expPair = new ComparePair(21, 34, 4);
    expPair.emps.get(0).add(new EntityMentionPos(34, tt0.mentions.get(0)));
    expPair.emps.get(0).add(new EntityMentionPos(34, tt0.mentions.get(1)));
    expPair.emps.get(1).add(new EntityMentionPos(34, tt1.mentions.get(1)));
    expPair.emps.get(2).add(new EntityMentionPos(34, tt2.mentions.get(0)));
    expPair.emps.get(3).add(new EntityMentionPos(34, tt3.mentions.get(1)));
    assertComparePair(expPair, it.next());

    // '. Another sentence.'
    expPair = new ComparePair(34, tt0.text.length(), 4);
    assertComparePair(expPair, it.next());

    assertFalse("compare iterator has unexpected elements", it.hasNext());
    it.close();

  }

}
