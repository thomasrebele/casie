package tpt.dbweb.cat.io;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tpt.dbweb.cat.datatypes.EntityMention;
import tpt.dbweb.cat.datatypes.TaggedText;
import tpt.dbweb.cat.datatypes.iterators.EntityMentionPos;
import tpt.dbweb.cat.datatypes.iterators.EntityMentionPosIterator;
import tpt.dbweb.cat.datatypes.iterators.EntityMentionPosIterator.PosType;

public class TaggedTextXMLWriter implements Closeable {

  private static Logger log = LoggerFactory.getLogger(TaggedTextXMLWriter.class);

  PrintStream ps = null;

  public TaggedTextXMLWriter(Path file) {
    try {
      if (!Files.exists(file.getParent())) {
        Files.createDirectories(file.getParent());
      }
      ps = new PrintStream(file.toFile());
      ps.println("<?xml version='1.0' encoding='UTF-8' ?>");
      ps.println("<?xml-stylesheet type='text/xsl' href='trafo.xsl' ?>");
      ps.println("<articles>");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public TaggedTextXMLWriter(PrintStream out) {
    ps = out;
  }

  @Override
  public String toString() {
    return "TaggedTextXMLWriter writing to " + ps;
  }

  @Override
  public void close() throws IOException {
    if (ps != null) {
      ps.println("</articles>");
      ps.close();
    }
  }

  /**
   * Output a tagged text as an article
   * @param attributeMap
   * @param tt
   */
  public void write(Map<String, String> attributeMap, TaggedText tt) {
    Map<String, String> tmpMap = new HashMap<>();
    if (tt.info(false) != null) {
      tmpMap.putAll(tt.info(false));
    }
    if (attributeMap != null) {
      tmpMap.putAll(attributeMap);
    }
    tmpMap.putIfAbsent("id", tt.id);
    writeString(tmpMap, toMarkedText(tt));
    ps.flush();
  }

  /**
   * Output a string as an article
   * @param attributeMap will be used as attributes for the <article> tag
   * @param text
   */
  public void writeString(Map<String, String> attributeMap, String text) {
    ps.print("  <article");
    if (attributeMap != null) {
      for (Entry<String, String> e : attributeMap.entrySet()) {
        ps.print(" " + escape(e.getKey()) + "='" + escape(e.getValue()) + "'");
      }
    }
    ps.println(">");
    ps.println("    " + text);
    ps.println("  </article>");
  }

  private static String escape(String str) {
    String result = StringEscapeUtils.escapeXml10(str);
    return result;
  }

  public static String toMarkedText(TaggedText tt) {
    if (tt.mentions != null) {
      tt.mentions.sort(null);
    }

    StringBuilder sb = new StringBuilder();
    int last = 0;
    if (tt.mentions != null) {
      for (EntityMentionPos emp : IteratorUtils.asIterable(new EntityMentionPosIterator(tt.mentions))) {
        // print text before entity mention position
        if (emp.pos < last || emp.pos > tt.text.length()) {
          log.warn("entity mention out of range: {}, {}", emp.pos, emp.posType.toString());
          continue;
        }
        sb.append(escape(tt.text.substring(last, emp.pos)));

        // print start tag
        if (emp.posType == PosType.START) {
          String entity = emp.em.entity;
          if (entity.startsWith("<") && entity.endsWith(">")) {
            entity = entity.substring(1, entity.length() - 1);
          }

          sb.append("<mark entity='");
          sb.append(escape(entity));
          sb.append("'");
          // print min mention
          if (emp.em.min != null) {
            sb.append(" min='");
            sb.append(escape(emp.em.min.spanString()));
            sb.append("'");
          }
          // print attributes
          for (Entry<String, String> attr : emp.em.info().entrySet()) {
            sb.append(" ");
            sb.append(attr.getKey());
            sb.append("='");
            sb.append(escape(attr.getValue()));
            sb.append("'");
          }
          sb.append(">");
        }

        // print end tag
        if (emp.posType == PosType.END) {
          sb.append("</mark>");
        }

        last = emp.pos;
      }
    }
    if (tt.text != null) {
      sb.append(escape(tt.text.substring(last)));
    }
    return sb.toString();
  }

  public static void main(String[] args) {
    TaggedText tt = new TaggedText();
    tt.text = "abc def ghi jkl mno pqr stu vwx";
    tt.mentions = Arrays.asList(new EntityMention(tt.text, 0, 3, "1"), new EntityMention(tt.text, 0, 7, "2"), new EntityMention(tt.text, 4, 7, "3"));

    String marked = TaggedTextXMLWriter.toMarkedText(tt);
    System.out.println(marked);

    tt.text = "Road & Track";
    tt.mentions = Arrays.asList();

    marked = TaggedTextXMLWriter.toMarkedText(tt);
    System.out.println(marked);
  }
}
