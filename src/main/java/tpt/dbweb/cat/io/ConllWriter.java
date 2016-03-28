package tpt.dbweb.cat.io;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.aligner.TextSpanAligner;
import tpt.dbweb.cat.datatypes.TaggedText;
import tpt.dbweb.cat.datatypes.TextSpan;
import tpt.dbweb.cat.datatypes.iterators.EntityMentionPos;
import tpt.dbweb.cat.datatypes.iterators.EntityMentionPosIterator;
import tpt.dbweb.cat.datatypes.iterators.EntityMentionPosIterator.PosType;
import tpt.dbweb.cat.tools.RegexWordTokenizer;
import tpt.dbweb.cat.tools.Tokenizer;

/**
 * Transform tagged text xml file to CoNLL format. See http://conll.cemantix.org/2011/data.html for details.
 * Use this if you want to compare with reference-coreference-scorers
 * @author Thomas Rebele
 */
public class ConllWriter {

  private static Logger log = LoggerFactory.getLogger(ConllWriter.class);

  public static class ConllDocumentPart {

    public String title;

    public TaggedText tt;

    /**
     * Align text column of conll format to these text spans
     */
    public List<TextSpan> alignToSpans;

    /**
     * Align text column of conll format to these columns. This class ignores it, if alignToSpans is set.
     */
    public List<String> alignToWords;
  }

  // TODO: use conll file to find correct tokenization
  Tokenizer wordTokenizer = new RegexWordTokenizer();

  public static void main(String[] args) throws IOException {

  }

  public void writeTT(TaggedText tt, Path outputFile) {
    writeTTList(Arrays.asList(tt), outputFile);
  }

  public void writeTTList(List<TaggedText> tts, Path outputFile) {
    List<ConllDocumentPart> parts = new ArrayList<>();
    Tokenizer wd = new RegexWordTokenizer();
    for (TaggedText tt : tts) {
      ConllDocumentPart part = new ConllDocumentPart();
      part.tt = tt;
      part.title = tt.id;
      part.alignToWords = wd.getTokens(tt.text).stream().map(ts -> ts.spanString()).collect(Collectors.toList());
      parts.add(part);
    }
    writePartList(parts, outputFile);
  }

  public void writePartList(List<ConllDocumentPart> parts, Path outputFile) {
    writePartList(parts, outputFile, false);
  }

  /**
   * Output entity mentions in conll format. Documents get enclosed by #begin document ... #end document
   * @param parts
   * @param outputFile
   */
  public void writePartList(List<ConllDocumentPart> parts, Path outputFile, boolean append) {
    try {
      Files.createDirectories(outputFile.getParent());
    } catch (IOException e1) {
      e1.printStackTrace();
    }
    try (Writer w = new FileWriter(outputFile.toFile(), append)) {
      for (ConllDocumentPart e : parts) {
        w.write("#begin document " + e.title + "\n");
        List<TextSpan> wordSpans = getWordSpans(e.tt.text, e.alignToWords);
        w.write(convert(e.tt, wordSpans, ""));
        w.write("#end document\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Transform a list of words in a text to a list of text spans.
   * @param text
   * @param words
   * @return list of text spans
   */
  private static List<TextSpan> getWordSpans(String text, List<String> words) {
    StringBuilder sb = new StringBuilder();
    List<TextSpan> spans = new ArrayList<TextSpan>();
    for (String word : words) {
      if (word == null) {
        spans.add(null);
      } else {
        TextSpan s = new TextSpan(null, sb.length(), sb.length() + word.length());
        spans.add(s);
        sb.append(word);
        sb.append(" ");
      }
    }

    String sbStr = sb.toString();
    spans = new TextSpanAligner<>(sbStr, text).align(spans);

    spans.forEach(span -> {
      if (span != null) {
        span.text = text;
      }
    });
    return spans;
  }

  /**
   * Reads a conll file.
   * @param file
   * @return map of document ids to a table (list of rows; a row is a list of columns)
   */
  public static Map<String, List<List<String>>> readConllDocuments(Path file) {
    Map<String, List<List<String>>> result = new HashMap<>();
    List<List<String>> docList = new ArrayList<>();
    String docid = null;
    LineIterator it;
    try {
      it = FileUtils.lineIterator(file.toFile());
      while (it.hasNext()) {
        String line = it.next();
        if (line.startsWith("#")) {
          if (line.startsWith("#begin document")) {
            docList = new ArrayList<>();
            docid = line.substring("#begin document".length() + 1);
          } else if (line.startsWith("#end document")) {
            if (docid == null) {
              log.error("doc id is null, cannot read conll file {}", file);
            } else {
              result.put(docid, docList);
            }
            docid = null;
          }
          continue;
        }
        String[] cols = line.split("\\s+");
        if (cols.length == 0 || (cols.length == 1 && "".equals(cols[0]))) {
          docList.add(Arrays.asList());
        } else {
          docList.add(Arrays.asList(cols));
        }
        // extract column for words
        // align output line by line to these words // TODO: move comment to right place
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  /**
   * Reads a column (space-separated) of a file. Ignores lines starting with #
   * @param alignToFile
   * @param columnIndex
   * @return map from docid to word list
   * @throws IOException
   */
  public static Map<String, List<String>> readColumn(Path file, int columnIndex) {
    Map<String, List<String>> result = new HashMap<>();
    // iterate over documents
    Map<String, List<List<String>>> docs = readConllDocuments(file);
    for (String docId : docs.keySet()) {
      List<List<String>> table = docs.get(docId);
      // extract column of table

      List<String> column = result.computeIfAbsent(docId, k -> new ArrayList<>());
      column.addAll(table.stream().filter(l -> l.size() > columnIndex).map(l -> {
        return l.get(columnIndex);
      }).collect(Collectors.toList()));
    }
    return result;
  }

  /**
   * Convert a tagged text to SemEval/Conll format for reference-coreference-scorers.
   * @param tt
   * @param words to which the mentions should be aligned (its best to use word column of Conll input files)
   * @param prefix
   * @throws IOException
   */
  private String convert(TaggedText tt, List<TextSpan> words, String prefix) throws IOException {
    EntityMentionPosIterator it = new EntityMentionPosIterator(tt.mentions);
    EntityMentionPos pos = it.next();

    List<List<String>> rows = new ArrayList<>();
    Map<String, Integer> entityToNumber = new HashMap<>();
    for (TextSpan word : words) {
      // assemble beginning of row
      if (word == null) {
        rows.add(null);
        continue;
      }
      ArrayList<String> row = new ArrayList<>();
      if (prefix != null && prefix.length() > 0) {
        row.add(prefix);
      }
      // escape brackets
      String wordStr = word.toString();
      wordStr = wordStr.replace("(", "-LBR-");
      wordStr = wordStr.replace(")", "-RBR-");
      row.add(wordStr);

      // check which entities start or end at this word
      List<String> startingEntities = new ArrayList<>(), endingEntities = new ArrayList<>();
      while (pos != null && pos.pos <= word.end) {
        if (pos.posType == PosType.START) {
          // fixes <one-character-word> <mark>...</mark>
          if (pos.pos == word.end) {
            break;
          }
          startingEntities.add(pos.em.entity);
        }
        if (pos.posType == PosType.END) {
          endingEntities.add(pos.em.entity);
        }
        pos = it.next();
      }

      // construct boundaries of the form (1 or (2) or 3)
      List<String> boundaries = new ArrayList<>();
      for (String entity : startingEntities) {
        int num = entityToNumber.computeIfAbsent(entity, k -> entityToNumber.size() + 1);
        String boundary = "(" + num;
        if (endingEntities.contains(entity)) {
          boundary += ")";
          endingEntities.remove(entity);
        }
        boundaries.add(boundary);
      }
      for (String entity : endingEntities) {
        int num = entityToNumber.computeIfAbsent(entity, k -> entityToNumber.size() + 1);
        boundaries.add(num + ")");
      }

      // assemble end of row
      String boundaryStr = String.join("|", boundaries);
      if (boundaryStr == null || boundaryStr.length() == 0) {
        boundaryStr = "-";
      }
      row.add(boundaryStr);
      rows.add(row);
    }
    return justPrint(rows);
  }

  /**
   * Format as a table, such that columns are aligned. E.g.
   *
   * I         (1)
   * want
   * to
   * thank
   * Bill      (4
   * Daley     4)
   * exemplary
   * @param rows
   * @return
   */
  private static String formatTable(List<List<String>> rows) {
    List<Integer> width = new ArrayList<>();

    // calculate column width
    rows.forEach(row -> {
      if (row != null) {
        while (width.size() < row.size()) {
          width.add(0);
        }
        for (int i = 0; i < row.size(); i++) {
          width.set(i, Math.max(width.get(i), row.get(i).length()));
        }
      }
    });

    // generate output
    StringBuilder sb = new StringBuilder();
    rows.forEach(row -> {
      if (row != null) {
        for (int i = 0; i < row.size(); i++) {
          sb.append(StringUtils.rightPad(StringUtils.defaultString(row.get(i)), width.get(i) + 1));
        }
      }
      sb.append("\n");
    });

    return sb.toString();
  }

  /**
   * Format as table, with space separated columns
   * @param rows
   * @return
   */
  private static String justPrint(List<List<String>> rows) {
    StringBuilder sb = new StringBuilder();
    rows.forEach(row -> {
      if (row != null) {
        sb.append(String.join(" ", row));
      }
      sb.append("\n");
    });
    return sb.toString();
  }
}
