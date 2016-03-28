package tpt.dbweb.cat.evaluation;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import tpt.dbweb.cat.datatypes.Fraction;

/**
 * Data class which stores a mapping from documents to metrics to precision/recall
 * @author Thomas Rebele
 */
public class ComparisonResult {

  public Map<String, Map<String, EvaluationStatistics>> docidToMetricToResult = new TreeMap<>();

  @Override
  public String toString() {
    return docidToMetricToResult.toString();
  }

  /**
   * Combines the results contained in this object. Result is also a ComparisonResult instance, with virtual documents "macro" and "micro".
   * Only works if it contains ValueEvaluationStatistics results
   * @return
   */
  public ComparisonResult combine() {
    ComparisonResult result = new ComparisonResult();

    for (String docId : docidToMetricToResult.keySet()) {
      Map<String, EvaluationStatistics> metricToResult = docidToMetricToResult.get(docId);
      for (String metric : docidToMetricToResult.get(docId).keySet()) {
        EvaluationStatistics stat = metricToResult.get(metric);

        // macro
        EvaluationStatistics macro = result.docidToMetricToResult.computeIfAbsent("macro", k -> new TreeMap<>()).computeIfAbsent(metric,
            k -> new ValueEvaluationStatistics());
        macro.addMacro(stat);

        EvaluationStatistics micro = result.docidToMetricToResult.computeIfAbsent("micro", k -> new TreeMap<>()).computeIfAbsent(metric,
            k -> new ValueEvaluationStatistics());
        micro.addMicro(stat);
      }
    }

    return result;
  }

  /**
   * Convert map to list, by putting the keys into the info property of EvaluationStatistics.
   * ATTENTION: this changes the EvaluationStatistic objects!
   * @return
   */
  public List<EvaluationStatistics> condense() {
    List<EvaluationStatistics> result = new ArrayList<>();

    for (String docId : docidToMetricToResult.keySet()) {
      Map<String, EvaluationStatistics> metricToResult = docidToMetricToResult.get(docId);
      for (String metric : metricToResult.keySet()) {
        EvaluationStatistics stat = metricToResult.get(metric);
        stat.info.put("docid", docId);

        // TODO: how to deal with this properly?
        if (stat.info.get("metric") == null) {
          stat.info.put("metric", metric);
        }
        result.add(stat);
      }
    }

    return result;
  }

  /**
   * Put results of other into this object.
   * @param other
   */
  public void merge(ComparisonResult other) {
    for (String docId : other.docidToMetricToResult.keySet()) {
      Map<String, EvaluationStatistics> metricToResult = other.docidToMetricToResult.get(docId);
      for (String metric : metricToResult.keySet()) {
        docidToMetricToResult.computeIfAbsent(docId, k -> new TreeMap<>()).computeIfAbsent(metric, k -> metricToResult.get(metric));
      }
    }
  }

  private static ObjectMapper mapper = null;

  static {
    mapper = new ObjectMapper();
    mapper.enableDefaultTyping();
  }

  public void write(Path file) throws JsonGenerationException, JsonMappingException, IOException {
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.writeValue(file.toFile(), this);
  }

  public static ComparisonResult read(Path file) throws JsonParseException, JsonMappingException, IOException {
    return mapper.readValue(file.toFile(), ComparisonResult.class);
  }

  public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
    ComparisonResult result = new ComparisonResult();
    result.docidToMetricToResult.computeIfAbsent("doc", k -> new TreeMap<>()).put("part", new ValueEvaluationStatistics(Fraction.ONE, Fraction.ONE));

    System.out.println(mapper.writeValueAsString(new Fraction(1.0, 2.0)));
    String ves = mapper.writeValueAsString(new ValueEvaluationStatistics(Fraction.ONE, new Fraction(1.0, 2.0)));
    System.out.println(ves);

    System.out.println(mapper.readValue(ves, ValueEvaluationStatistics.class));

    result.write(Paths.get("/tmp/test.json"));
    read(Paths.get("/tmp/test.json"));
  }
}