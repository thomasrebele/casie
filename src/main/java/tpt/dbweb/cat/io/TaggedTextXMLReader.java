package tpt.dbweb.cat.io;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javatools.datatypes.PeekIterator;
import javatools.filehandlers.FileLines;
import tpt.dbweb.cat.Utility;
import tpt.dbweb.cat.datatypes.EntityMention;
import tpt.dbweb.cat.datatypes.TaggedText;
import tpt.dbweb.cat.datatypes.TextSpan;

public class TaggedTextXMLReader {

  private final static Logger log = LoggerFactory.getLogger(TaggedTextXMLReader.class);

  public static class Options {

    /** combine multiple whitespace characters to a space */
    public boolean normalizeWhitespace = true;

    /** remove whitespace before and after newlines */
    public boolean trimLines = true;

    /** remove newlines which are not paragraphs */
    public boolean stripSingleNewlineCharacters = true;

    /** combine multiple newlines to a paragraph */
    public boolean combineMultipleNewlinesToParagraph = true;
  }

  private Options options = new Options();

  public TaggedTextXMLReader() {
  }

  public TaggedTextXMLReader(Options options) {
    this.options = options;
  }

  public List<TaggedText> getTaggedTextFromFile(String f) {
    return getTaggedText(Paths.get(f));
  }

  public TaggedText getFirstTaggedTextFromString(String f) {
    return getTaggedTextFromString(f).get(0);
  }

  public List<TaggedText> getTaggedTextFromString(String f) {
    if (!f.contains("<article>")) {
      f = "<article>" + f + "</article>";
    }

    InputStream is = new ByteArrayInputStream(f.getBytes(StandardCharsets.UTF_8));
    return IteratorUtils.toList(getNormalizedIterator(is, null));
  }

  public List<TaggedText> getTaggedText(Path path) {
    List<TaggedText> result = null;
    try {
      result = IteratorUtils.toList(iteratePath(path));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return result;
  }

  private Iterator<String> getArticleIterator(InputStream is, String debugInfo) {
    Reader r = new InputStreamReader(is);
    return new PeekIterator<String>() {

      boolean containsArticleTags = false;

      @Override
      protected String internalNext() throws Exception {
        String prefix = "<article", suffix = "</article>";
        String text = FileLines.readTo(r, suffix).toString();
        String between = FileLines.readBetween(text, prefix, suffix);
        if (between == null) {
          if (containsArticleTags) {
            return null;
          }
          if (text.contains("<articles")) {
            return null;
          }
          return text != null && text.length() > 0 ? text : null;
        }
        containsArticleTags = true;
        if (between.startsWith("s>")) {
          int pos = between.indexOf(prefix);
          if (pos > 0) {
            between = between.substring(pos + prefix.length());
          } else {
            return internalNext();
          }
        }
        String result = between != null ? prefix + between + suffix : null;
        if (result != null && result.contains("<articles>")) {
          log.error("<articles> should not be in the output of TaggedTextXMLReader");
        }
        return result;
      }
    };
  }

  private Iterator<TaggedText> getNormalizedIterator(InputStream is, String errorMessageInfo) {
    return new PeekIterator<TaggedText>() {

      Iterator<String> articleIterator = getArticleIterator(is, errorMessageInfo);

      @Override
      protected TaggedText internalNext() throws Exception {
        while (articleIterator.hasNext()) {
          String article = normalize(articleIterator.next());
          if (article.contains("<articles>")) {
            log.error("article should not contain <articles> tag");
          }
          Iterator<TaggedText> it = getIterator(IOUtils.toInputStream(article, StandardCharsets.UTF_8), errorMessageInfo + ", article " + article);
          if (it.hasNext()) {
            return it.next();
          }
        }
        return null;
      }

    };
  }

  private String normalize(String input) {
    if (options.trimLines) {
      input = input.replaceAll("[ \\t\\x0B\\f\\r]*\\n[ \\t\\x0B\\f\\r]*", "\n");
    }
    if (options.stripSingleNewlineCharacters) {
      input = input.replaceAll("([^\\n])\\n([^\\n])", "$1 $2");
    }
    if (options.combineMultipleNewlinesToParagraph) {
      input = input.replaceAll("\\n\\n+", "\n\n");
    }
    if (options.normalizeWhitespace) {
      input = input.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
    }
    return input;
  }

  private Iterator<TaggedText> getIterator(InputStream is, String errorMessageInfo) {

    XMLStreamReader tmpxsr = null;
    try {
      XMLInputFactory xif = XMLInputFactory.newInstance();
      xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
      xif.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
      xif.setProperty(XMLInputFactory.IS_VALIDATING, false);
      tmpxsr = xif.createXMLStreamReader(is);
    } catch (XMLStreamException | FactoryConfigurationError e) {
      e.printStackTrace();
      return null;
    }

    final XMLStreamReader xsr = tmpxsr;
    return new PeekIterator<TaggedText>() {

      @Override
      protected TaggedText internalNext() {
        ArrayList<TextSpan> openMarks = new ArrayList<>();
        StringBuilder pureTextSB = new StringBuilder();
        ArrayList<TextSpan> marks = new ArrayList<>();
        marks.add(new TextSpan(null, 0, 0));
        TaggedText tt = null;

        try {
          loop: while (xsr.hasNext()) {
            xsr.next();
            int event = xsr.getEventType();
            switch (event) {
              case XMLStreamConstants.START_ELEMENT:
                if ("articles".equals(xsr.getLocalName())) {
                } else if ("article".equals(xsr.getLocalName())) {
                  tt = new TaggedText();
                  for (int i = 0; i < xsr.getAttributeCount(); i++) {
                    if ("id".equals(xsr.getAttributeLocalName(i))) {
                      tt.id = xsr.getAttributeValue(i);
                    }
                    tt.info().put(xsr.getAttributeLocalName(i), xsr.getAttributeValue(i));
                  }

                } else if ("mark".equals(xsr.getLocalName())) {
                  TextSpan tr = new TextSpan(null, pureTextSB.length(), pureTextSB.length());
                  for (int i = 0; i < xsr.getAttributeCount(); i++) {
                    tr.info().put(xsr.getAttributeLocalName(i), xsr.getAttributeValue(i));
                  }

                  openMarks.add(tr);
                } else if ("br".equals(xsr.getLocalName())) {
                  // TODO: how to propagate tags from the input to the output?
                } else {
                  log.warn("ignore tag " + xsr.getLocalName());
                }
                break;
              case XMLStreamConstants.END_ELEMENT:
                if ("mark".equals(xsr.getLocalName())) {

                  // search corresponding <mark ...>
                  TextSpan tr = openMarks.remove(openMarks.size() - 1);
                  if (tr == null) {
                    log.warn("markend at " + xsr.getLocation().getCharacterOffset() + " has no corresponding mark tag");
                    break;
                  }

                  tr.end = pureTextSB.length();
                  marks.add(tr);

                } else if ("article".equals(xsr.getLocalName())) {
                  tt.text = StringUtils.stripEnd(pureTextSB.toString().trim(), " \t\n");
                  pureTextSB = new StringBuilder();

                  tt.mentions = new ArrayList<>();
                  for (TextSpan mark : marks) {

                    String entity = mark.info().get("entity");
                    if (entity == null) {
                      entity = mark.info().get("annotation");
                    }
                    if (entity != null) {
                      EntityMention e = new EntityMention(tt.text, mark.start, mark.end, entity);
                      String minMention = mark.info().get("min");
                      String mention = e.getMention();
                      if (minMention != null && !"".equals(minMention)) {
                        Pattern p = Pattern.compile(Pattern.quote(minMention));
                        Matcher m = p.matcher(mention);
                        if (m.find()) {
                          TextSpan min = new TextSpan(e.text, e.start + m.start(), e.start + m.end());
                          e.min = min;
                          if (m.find()) {
                            log.warn("found " + minMention + " two times in \"" + mention + "\"");
                          }
                        } else {
                          String prefix = Utility.findLongestPrefix(mention, minMention);
                          log.warn("didn't find min mention '" + minMention + "' in text '" + mention + "', longest prefix found: '" + prefix
                              + "' in article " + tt.id);
                        }
                      }

                      mark.info().remove("min");
                      mark.info().remove("entity");
                      if (mark.info().size() > 0) {
                        e.info().putAll(mark.info());
                      }
                      tt.mentions.add(e);
                    }
                  }
                  openMarks.clear();
                  marks.clear();
                  break loop;
                }
                break;
              case XMLStreamConstants.CHARACTERS:
                String toadd = xsr.getText();
                if (pureTextSB.length() == 0) {
                  toadd = StringUtils.stripStart(toadd, " \t\n");
                }
                if (toadd.contains("thanks")) {
                  log.info("test");
                }
                pureTextSB.append(toadd);
                break;
            }

          }
        } catch (XMLStreamException e) {
          log.error("{}", errorMessageInfo);
          throw new RuntimeException(e);
        }
        if (tt != null && tt.mentions != null) {
          tt.mentions.sort(null);
        }
        return tt;
      }
    };
  }

  public Iterator<TaggedText> iteratePath(Path path) throws FileNotFoundException {
    InputStream is = null;
    is = new FileInputStream(path.toFile());
    return getNormalizedIterator(is, path.toString());
  }

  public static void main(String... args) {
    String file = "result/ace2004/roth-dev/dev//tagged-by-pronoun.xml";
    for (TaggedText tt : new TaggedTextXMLReader().getTaggedTextFromFile(file)) {
      System.out.println(tt);
    }

    try {
      for (String str : Utility.iterable(new TaggedTextXMLReader().getArticleIterator(new FileInputStream(file), file))) {
        System.out.print("START: " + str.subSequence(0, 50));
        System.out.print("...");
        System.out.println(str.subSequence(str.length() - 50, str.length()) + " END");
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

  }
}
