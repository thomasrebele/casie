package tpt.dbweb.cat.evaluation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tpt.dbweb.cat.datatypes.Fraction;
import tpt.dbweb.cat.datatypes.TaggedText;
import tpt.dbweb.cat.io.ConllWriter;
import tpt.dbweb.cat.io.TaggedTextXMLReader;

/**
 * Calls lib/reference-coreference-scorers and parses the output.
 * @author Thomas Rebele
 *
 */
public class ReferenceEvaluator {

  private static Logger log = LoggerFactory.getLogger(ReferenceEvaluator.class);

  public class Options {

    public boolean singleFile = true;
  }

  private final Options options = new Options();

  // create a regex to parse
  private Pattern resultPattern = null;

  public ReferenceEvaluator() {
    String floatingPointRegex = "\\d*\\.?\\d+";
    String nomRegex = "\\((?<RecallNom>" + floatingPointRegex + ")\\s*";
    String denomRegex = "\\s*(?<RecallDenom>" + floatingPointRegex + ")\\)";
    String percentRegex = "\\s*(?<RecallPct>" + floatingPointRegex + ")\\%\\s*";
    String recallPattern = "Recall:\\s*(?:" + nomRegex + "/" + denomRegex + ")?" + percentRegex;
    String precisionPattern = recallPattern.replaceAll("Recall", "Precision");
    String f1Pattern = recallPattern.replaceAll("Recall", "F1");
    resultPattern = Pattern.compile(recallPattern + precisionPattern + f1Pattern + ".*");
  }

  /**
   * Compares two tagged text XML files.
   * @param goldstandard path to tagged text XML
   * @param compare path to tagged text XML
   * @param tmpDirectory where to store generated files
   * @return
   */
  public ComparisonResult compareXMLFiles(Path goldstandard, Path compare, Path tmpDirectory) {
    TaggedTextXMLReader.Options options = new TaggedTextXMLReader.Options();
    TaggedTextXMLReader reader = new TaggedTextXMLReader(options);
    ConllWriter conll = new ConllWriter();
    ReferenceEvaluator evaluator = new ReferenceEvaluator();

    ComparisonResult result = new ComparisonResult();
    // many files
    List<TaggedText> goldstd = reader.getTaggedText(goldstandard), cmp = reader.getTaggedText(compare);
    String scorerOutput = goldstandard.getFileName() + "-" + compare.getFileName() + "-scorer-output";
    if (this.options.singleFile) {
      log.warn("using only one thread");
      Path goldstdConllFile = tmpDirectory.resolve(goldstandard.getFileName() + ".conll");
      conll.writeTTList(goldstd, goldstdConllFile);

      Path compareConllFile = tmpDirectory.resolve(compare.getFileName() + ".conll");
      conll.writeTTList(cmp, compareConllFile);

      return evaluator.compareConllFiles(goldstdConllFile, compareConllFile, tmpDirectory.resolve(scorerOutput + ".txt"));

    } else {
      int cnt = Math.min(goldstd.size(), cmp.size());
      List<ComparisonResult> lst = IntStream.range(0, cnt).parallel().mapToObj((i) -> {
        String id = goldstd.get(i).id;
        Path goldstdConllFile = tmpDirectory.resolve(goldstandard.getFileName() + "-" + id);
        conll.writeTT(goldstd.get(i), goldstdConllFile);

        Path compareConllFile = tmpDirectory.resolve(compare.getFileName() + "-" + id);
        conll.writeTT(cmp.get(i), compareConllFile);

        return evaluator.compareConllFiles(goldstdConllFile, compareConllFile,
            tmpDirectory.resolve(scorerOutput + Thread.currentThread().getName() + ".txt"));
      }).collect(Collectors.toList());

      lst.forEach(cr -> result.merge(cr));
    }
    return result;
  }

  /**
   * Compare two files in Conll format (!)
   * @param goldstandard
   * @param compare
   * @param scorerOutput save output of scorer to this file if set
   * @return
   */
  public ComparisonResult compareConllFiles(Path goldstandard, Path compare, Path scorerOutput) {
    log.debug("comparing {} {}", goldstandard, compare);
    String cmd = "lib/reference-coreference-scorers/scorer.pl all " + goldstandard + " " + compare;
    String str = execExternalCommand(cmd);
    log.trace("reference-coreference-scorers output: {}", str);
    if (scorerOutput != null) {
      try {
        FileUtils.writeStringToFile(scorerOutput.toFile(), str);
        log.info("wrote scorer output to {}", scorerOutput);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return parseScorerOutput(str);
  }

  /**
   * Compare two Conll directories by concatenating them into one file.
   * Please ensure that the two directories have the same structure.
   * Files must end with 'conll'
   * @param goldstandardDir
   * @param compareDir
   * @param scorerOutput
   * @return
   */
  public ComparisonResult compareConllDirectorysAsSingleFile(Path goldstandardDir, Path compareDir, Path tmpDirectory) {
    try {
      Map<String, Path> goldstd = new HashMap<>();

      List<Path> cmp = Files.walk(compareDir).filter(p -> p.toString().endsWith("conll")).collect(Collectors.toList());
      Files.walk(goldstandardDir).filter(p -> p.toString().endsWith("conll")).forEach(p -> goldstd.put(p.getFileName().toString(), p));

      Path cmpFileGoldstd = tmpDirectory.resolve(goldstandardDir.toString().replace("/", "-"));
      Path cmpFileCmp = tmpDirectory.resolve(compareDir.toString().replace("/", "-"));

      for (Path cmpPath : cmp) {
        // try to find cmp file
        Path goldstdPath = goldstd.get(cmpPath.getFileName().toString());
        if (goldstdPath == null) {
          log.warn("warning, compare file not found for goldstandard file: {}", cmpPath);
          continue;
        }

        FileUtils.writeStringToFile(cmpFileGoldstd.toFile(), FileUtils.readFileToString(goldstdPath.toFile()), true);
        FileUtils.writeStringToFile(cmpFileCmp.toFile(), FileUtils.readFileToString(cmpPath.toFile()), true);
      }

      String scorerOutput = cmpFileGoldstd.getFileName() + "-" + cmpFileCmp.getFileName() + "-scorer-output";
      return compareConllFiles(cmpFileGoldstd, cmpFileCmp, tmpDirectory.resolve(scorerOutput + ".txt"));

    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Call external command
   * @param cmd
   * @return its std output as a string
   */
  private String execExternalCommand(String cmd) {
    log.debug("executing command: {}", cmd);
    Process p = null;
    try {
      p = Runtime.getRuntime().exec(cmd);
    } catch (IOException e1) {
      e1.printStackTrace();
    }
    InputStream is = p.getInputStream();
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    StringWriter sw = new StringWriter();
    try {
      while (p.isAlive()) {
        try {
          char[] buffer = new char[1024 * 4];
          int n = 0;
          while (-1 != (n = br.read(buffer))) {
            sw.write(buffer, 0, n);
          }
          if (p.isAlive()) {
            Thread.sleep(10);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    } catch (InterruptedException e1) {
      Thread.currentThread().interrupt();
    }
    String str = sw.toString();
    return str;
  }

  /**
   * Get documents, metrics and their respective recall and precision values
   * @param output
   * @return
   */
  public ComparisonResult parseScorerOutput(String output) {
    String[] lines = output.split("\n");

    if (lines.length < 1) {
      return null;
    }
    if (!lines[0].startsWith("version: 8.01")) {
      log.warn("expected version 8.01, but got {}", lines[0]);
    }
    ComparisonResult result = new ComparisonResult();

    // go through all lines
    String actMetric = null;
    String actDocid = null;
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];

      // track document and metric info
      String metricPrefix = "METRIC ";
      String docidPrefix = "====>";
      String totalsPrefix = "====== TOTALS =======";
      if (line.startsWith(metricPrefix)) {
        actMetric = line.substring(metricPrefix.length(), line.length() - 1);
      }
      if (line.startsWith(docidPrefix)) {
        actDocid = line.substring(docidPrefix.length() + 1, line.length() - 1);
      }
      if (line.startsWith(totalsPrefix)) {
        actDocid = null;
      }

      // parse line if possible
      Matcher m = resultPattern.matcher(line);
      if (m.matches()) {
        double recallnom = Float.parseFloat(m.group("RecallNom"));
        double recalldenom = Float.parseFloat(m.group("RecallDenom"));
        double precisionnom = Float.parseFloat(m.group("PrecisionNom"));
        double precisiondenom = Float.parseFloat(m.group("PrecisionDenom"));

        /*int recallpercent = Integer.parseInt(m.group("RecallPct"));
        int precisionpercent = Integer.parseInt(m.group("PrecisionPct"));
        int f1percent = Integer.parseInt(m.group("F1Pct"));*/
        if (actMetric == null) {
          log.error("metric is null, line {}", i);
        } else if (actDocid == null) {
          log.error("docid is null, line {}", i);
        } else {
          Fraction recall = new Fraction(recallnom, recalldenom);
          Fraction precision = new Fraction(precisionnom, precisiondenom);
          ValueEvaluationStatistics value = new ValueEvaluationStatistics(recall, precision);
          result.docidToMetricToResult.computeIfAbsent(actDocid, k -> new HashMap<>()).put(actMetric, value);
        }
      } else if (line.contains("Recall")) {
        log.debug("pattern didn't match {}", line);
      }
    }
    return result;
  }

  public static void main(String[] args) {
    ReferenceEvaluator eval = new ReferenceEvaluator();

    /*ComparisonResult r = eval.compareConllFiles(Paths.get("result/ace2004/roth-dev/dev/conll-format/ace2004-aida2014-with-nme.xml"),
        Paths.get("result/ace2004/roth-dev/dev/conll-format/tagged-by-aida.xml"));*/
    Path goldstd = Paths.get("test-data/ace2004/roth-dev/dev/ace2004-aida2014-with-nme.xml");
    Path compare = Paths.get("result/ace2004/roth-dev/dev/tagged-by-aida.xml");
    Path tmpDir = Paths.get("result/ace2004/roth-dev/dev/conll-format");
    ComparisonResult r = eval.compareXMLFiles(goldstd, compare, tmpDir);

    System.out.println(r.docidToMetricToResult);
  }
}
