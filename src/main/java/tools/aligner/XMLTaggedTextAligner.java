package tools.aligner;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tpt.dbweb.cat.datatypes.EntityMention;
import tpt.dbweb.cat.datatypes.TaggedText;
import tpt.dbweb.cat.io.TaggedTextXMLReader;
import tpt.dbweb.cat.io.TaggedTextXMLWriter;

/**
 * Load tagged texts from xml file and align them to tagged text of a second xml file.
 * @author Thomas Rebele
 */
public class XMLTaggedTextAligner {

  private static Logger log = LoggerFactory.getLogger(XMLTaggedTextAligner.class);

  public static void main(String[] args) {

    String inputFile = "/home/tr/workspace/e6540/factchecker/test-data/ace2004/ace2004-aida2014-with-nme.xml";
    String alignToFile = "/home/tr/workspace/e6540/factchecker/test-data/ace2004/ace2004.xml";
    String outputFile = "/home/tr/workspace/e6540/factchecker/test-data/ace2004/ace2004-aida2014-with-nme-aligned.xml";

    List<TaggedText> inputList = new TaggedTextXMLReader().getTaggedTextFromFile(inputFile);
    List<TaggedText> alignToList = new TaggedTextXMLReader().getTaggedTextFromFile(alignToFile);

    if (inputList.size() != alignToList.size()) {
      log.warn("different number of articles in {} and {}", inputFile, alignToFile);
    }

    int size = Math.min(inputList.size(), alignToList.size());

    try (TaggedTextXMLWriter writer = new TaggedTextXMLWriter(Paths.get(outputFile))) {
      // iterate over inputList and alignToList
      for (int i = 0; i < size; i++) {
        TaggedText input = inputList.get(i);
        TaggedText alignTo = alignToList.get(i);

        // do the alignment
        TextSpanAligner<EntityMention> aligner = new TextSpanAligner<>(input.text, alignTo.text);
        List<EntityMention> alignedMentions = aligner.align(input.mentions);
        input.mentions = new ArrayList<>();
        // keep valid mentions only
        for (EntityMention em : alignedMentions) {
          if (em.start >= 0 && em.end >= 0) {
            input.mentions.add(em);
          }
        }
        input.text = alignTo.text;
        writer.write(null, input);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}