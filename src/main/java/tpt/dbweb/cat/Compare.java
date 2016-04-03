package tpt.dbweb.cat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;

import tpt.dbweb.cat.datatypes.EntityMention;
import tpt.dbweb.cat.datatypes.MentionChains;
import tpt.dbweb.cat.datatypes.MentionChains.Chain;
import tpt.dbweb.cat.datatypes.TaggedText;
import tpt.dbweb.cat.datatypes.iterators.CompareIterator;
import tpt.dbweb.cat.datatypes.iterators.ComparePair;
import tpt.dbweb.cat.datatypes.iterators.EntityMentionPos;
import tpt.dbweb.cat.datatypes.iterators.EntityMentionPosIterator.PosType;
import tpt.dbweb.cat.evaluation.ComparisonResult;
import tpt.dbweb.cat.evaluation.EvaluationStatistics;
import tpt.dbweb.cat.io.TaggedTextXMLReader;
import tpt.dbweb.cat.tools.MentionChainAligner;
import tpt.dbweb.cat.tools.Utility;

/**
 * Compare one or more XML files with annotations to a goldstandard and output them as a self-contained XML file.
 * It uses src/main/resources/compare-template.xml to create the output file. Please change XML transformation, CSS and Javascript there.
 *
 * @author Thomas Rebele
 *
 */
public class Compare {

  private final static Logger log = LoggerFactory.getLogger(Compare.class);

  /**
   * Command line options for compare
   */
  public static class Options {

    @Parameter(description = "Input files, treat first as the gold standard")
    public List<String> input = new ArrayList<>();

    @Parameter(names = "--out")
    public String outputFile = null;

    boolean replaceNewlineWithBR = false;

    /**
     * Only use the min mention for visualization
     */
    boolean minOnly = true;

    /**
     * Transform the entities to a more human readable form (add string of first mention and chain number)
     */
    public boolean humanReadableMentions = true;

    /**
     * remove non-mention-entities from the input
     */
    public boolean filterNMEEntities = true;

  }

  private final Options options;

  public Compare(Options options) {
    this.options = options;
  }

  /**
   * Saves the evaluation of a mark (correct, missing, wrong, toomuch) and chain information, e.g. "(1" or "2" or "3)"
   */
  private class MarkEval {

    String eval;

    String chainBefore;

    String chainAfter;
  }

  public static void compareXML(Options options, List<ComparisonResult> evaluations) throws IOException {
    if (options.outputFile != null && options.input != null && options.input.size() > 0) {
      Compare compare = new Compare(new Options());
      List<Path> paths = options.input.stream().map(str -> Paths.get(str)).collect(Collectors.toList());
      compare.compareXML(paths, Paths.get(options.outputFile), evaluations);
    }
  }

  /**
   * Do some cleanup on the text, e.g. removing unwanted entities
   * @param tt
   */
  private void cleanUp(TaggedText tt) {
    // tt.mentions.removeIf(em ->
    // options.filterEntities.contains(em.entity));
    if (options.filterNMEEntities) {
      tt.mentions.removeIf(em -> Utility.isNME(em.entity));
    }
    tt.mentions.sort(null);
  }

  /**
   * Check whether we can accept the input, i.e. all the tagged texts have the same text.
   * @param files list of filenames to output more useful information to the user
   * @param tts list of tagged texts
   * @return true if tagged texts have the right format
   */
  private boolean checkTaggedTexts(List<Path> files, List<TaggedText> tts) {
    // print message when article ids are not the same text
    for (int i = 1; i < tts.size(); i++) {
      TaggedText tt0 = tts.get(0), ttI = tts.get(i);
      if (!tt0.id.equals(ttI.id)) {
        StringBuilder sb = new StringBuilder();
        sb.append("article id is not the same (file " + files.get(0) + ", id " + tt0.id + " and file " + files.get(i) + ", id " + ttI.id + ")");
        sb.append("\n>>>");
        sb.append(tt0.text);
        sb.append("\n<<<\n>>>");
        sb.append(ttI.text);
        sb.append("\n<<<\n");
        log.warn(sb.toString());
        return false;
      }

      // print message when article texts are not the same
      if (!tt0.text.equals(ttI.text)) {
        if (log.isWarnEnabled()) {
          StringBuilder sb = new StringBuilder();
          sb.append(
              "text of article is not the same (file " + files.get(0) + ", id " + tt0.id + " and file " + files.get(i) + ", id " + ttI.id + ")");
          sb.append(", common prefix: '");
          int prefixLen = Utility.getCommonPrefixLength(tt0.text, ttI.text);
          sb.append(tt0.text.substring(0, prefixLen));
          sb.append("'");
          log.warn(sb.toString());
          log.warn("1st text continues with " + tt0.text.substring(prefixLen, Math.min(prefixLen + 10, tt0.text.length())));
          log.warn("2nd text continues with " + ttI.text.substring(prefixLen, Math.min(prefixLen + 10, ttI.text.length())));

          log.warn("length 1st text: " + tt0.text.length());
          log.warn("length 2nd text: " + ttI.text.length());
        }
        return false;
      }
    }
    return true;
  }

  /**
   * Load XML files, compare them and write the output XML files to out
   * @param files
   * @param out
   * @param evaluations
   * @throws IOException
   */
  public void compareXML(List<Path> files, Path out, List<ComparisonResult> evaluations) throws IOException {
    log.info("comparing {}; writing output to {}", files, out);
    boolean docEvaluationNotFound = false;

    StringWriter sw = new StringWriter();
    PrintWriter ps = new PrintWriter(sw);
    try {
      TaggedTextXMLReader ttxr = new TaggedTextXMLReader();
      ps.append("<annotators>\n");
      List<Iterator<TaggedText>> ttIts = new ArrayList<>();
      // load files and print annotator info
      for (int i = 0; i < files.size(); i++) {
        ttIts.add(ttxr.iteratePath(files.get(i)));
        ps.append("\t<annotator id='" + i + "' file='");
        ps.append(StringEscapeUtils.escapeXml11(files.get(i).toString()));
        ps.append("'/>\n");
      }
      ps.append("</annotators>\n");

      // print evaluation
      List<Map<String, EvaluationStatistics>> evals = new ArrayList<>();
      Map<String, EvaluationStatistics> eval;
      if (evaluations != null) {
        for (int i = 0; i < evaluations.size(); i++) {
          ComparisonResult combinedEvaluations = evaluations.get(i).combine();
          eval = new TreeMap<>();
          evals.add(eval);

          // type is macro / micro
          for (String type : combinedEvaluations.docidToMetricToResult.keySet()) {
            Map<String, EvaluationStatistics> metricToResult = combinedEvaluations.docidToMetricToResult.get(type);
            for (String metric : metricToResult.keySet()) {
              eval.put(metric + " (" + type + ")", metricToResult.get(metric));
            }
          }
        }
        ps.print(printMetrics(evals));
        evals.clear();
      }

      // print comparison of articles
      while (ttIts.stream().allMatch(it -> it.hasNext())) {
        List<TaggedText> tts = ttIts.stream().map(it -> it.next()).collect(Collectors.toList());
        tts.forEach(tt -> cleanUp(tt));
        if (!checkTaggedTexts(files, tts)) {
          break;
        }

        // do comparison and write to output
        ps.print("  <article id='");
        ps.print(tts.get(0).id);
        ps.println("'>");

        ps.print(compare(tts));
        ps.println();

        // print evaluation of article
        if (evaluations != null) {
          docEvaluationNotFound = true;
          for (int i = 0; i < evaluations.size(); i++) {
            eval = evaluations.get(i).docidToMetricToResult.get(tts.get(0).id);
            evals.add(eval);
            if (eval != null) {
              docEvaluationNotFound = false;
            }
          }
          if (docEvaluationNotFound == false) {
            ps.print(printMetrics(evals));
          } else {
          }
        } else {
          docEvaluationNotFound = true;
        }
        if (docEvaluationNotFound) {
          log.warn("evaluation not found for {}", tts.get(0).id);
        }
        ps.println("  </article>");
      }

      if (docEvaluationNotFound && evaluations != null && evaluations.size() > 0) {
        log.warn("available evaluations: {}", evaluations.get(0).docidToMetricToResult.keySet());
      }
    } catch (FileNotFoundException e) {
      log.error("file not found: {}", e.getMessage());
    }

    // load template and replace <article/>
    String template = Utility.readResourceAsString("compare-template.xml");
    sw.toString();
    String output = template.replace("<article/>", sw.toString());
    FileUtils.writeStringToFile(out.toFile(), output);
  }

  private String printMetrics(List<Map<String, EvaluationStatistics>> evaluations) {
    StringBuilder sb = new StringBuilder();
    sb.append("<metrics>\n");
    for (int i = 0; i < evaluations.size(); i++) {
      Map<String, EvaluationStatistics> map = evaluations.get(i);
      sb.append("<annotator id='" + i + "'>\n");
      for (String name : map.keySet()) {
        sb.append("    <metric name='" + name + "'");
        EvaluationStatistics es = map.get(name);
        sb.append(" recall='" + es.getRecall() + "'");
        sb.append(" precision='" + es.getPrecision() + "'/>\n");
      }
      sb.append("</annotator>\n");
    }
    sb.append("</metrics>\n");
    return sb.toString();
  }

  public String compare(List<TaggedText> tts) {
    StringBuilder builder = new StringBuilder();
    List<List<EntityMention>> mentions = new ArrayList<>();

    // track open marks and entity mentions
    List<String> openMarks = new ArrayList<>(); // list is overkill

    // replace mentions by their minimum
    for (int i = 0; i < tts.size(); i++) {
      if (options.minOnly) {
        mentions.add(new ArrayList<>());

        for (EntityMention em : tts.get(i).mentions) {
          mentions.get(i).add(em.getMinMention());
        }
      } else {
        mentions.add(tts.get(i).mentions);
      }
    }

    // chains for both documents
    List<MentionChains> chains = mentions.stream().map(eml -> new MentionChains(eml)).collect(Collectors.toList());

    // align chains to chain0
    for (int i = 1; i < chains.size(); i++) {
      Map<String, String> map = new MentionChainAligner().guessEntityMapGreedy(tts.get(0), tts.get(i));

      int unmappedIdx = chains.get(0).entityToChain.size() + 1;
      for (String entity : chains.get(i).entityToChain.keySet()) {
        String mappedEntity = map.get(entity);
        Chain entityChainI = chains.get(i).entityToChain.get(entity);
        if (mappedEntity == null) {
          entityChainI.idx = unmappedIdx++;
        } else {
          Chain entityChain0 = chains.get(0).entityToChain.get(mappedEntity);
          if (entityChain0 == null) {
            entityChainI.idx = unmappedIdx++;
          } else {
            entityChainI.idx = entityChain0.idx;
          }
        }
      }
    }

    // generate new entity names for human output
    List<Map<String, String>> entityMentionToOutput = new ArrayList<>();
    entityMentionToOutput.add(getEntityRenameMap(mentions.get(0), options.humanReadableMentions, null));
    for (int i = 0; i < mentions.size(); i++) {
      entityMentionToOutput.add(getEntityRenameMap(mentions.get(i), options.humanReadableMentions, chains.get(0)));
    }

    // iterate over mentions
    CompareIterator cmpIt = new CompareIterator(tts.get(0).text, tts.get(0).id, mentions);
    ComparePair last = null;
    for (ComparePair pair : Utility.iterable(cmpIt)) {
      log.trace("{}, text {}", pair, tts.get(0).text.substring(pair.start, pair.end));
      // escape span that was compared
      String escaped = StringEscapeUtils.escapeXml10(tts.get(0).text.substring(pair.start, pair.end));
      if (options.replaceNewlineWithBR) {
        escaped = escaped.replace("\n\n\n", "<br/>");
        escaped = escaped.replace("\n\n", "<br/>");
        escaped = escaped.replace("\n", "<br/>");
      }

      boolean hasEntity = false;
      List<EntityMention> principalMentions = new ArrayList<>();
      for (int i = 0; i < mentions.size(); i++) {
        EntityMention em = pair.getPrincipalMention(i);
        // filter AIDA out-of-knowledge-base-entities
        if (em != null && "--OOKBE--".equals(em.entity)) {
          em = null;
        }
        hasEntity |= em != null;
        principalMentions.add(em);
      }

      // create mark tag
      List<MarkEval> evals = null;
      if (hasEntity) {
        openMarks.add("entities " + principalMentions);
        builder.append("<mark ");

        evals = new ArrayList<>();
        boolean split = evaluateMark(last, pair, principalMentions, chains, evals);
        builder.append(" split='" + Boolean.toString(split) + "'");
        // add entity and other information
        if (principalMentions.get(0) != null) {
          builder.append(" entity0='" + entityMentionToOutput.get(0).get(principalMentions.get(0).entity) + "'");
        }

        // TODO: how to deal with additional information?
        for (int i = 0; i < mentions.size(); i++) {
          if (principalMentions.get(i) != null) {
            /*if (principalMentions.get(i).info() != null) {
              for (Entry<String, String> info : principalMentions.get(i).info().entrySet()) {
                builder.append(" " + info.getKey() + i + "='"
                    + StringEscapeUtils.escapeXml10(info.getValue()) + "'");
              }
            }*/
          }
        }

        addChainInfo("0", evals.get(0), builder);
        builder.append(">");

        // print out individual annotator evaluations
        for (int i = 1; i < mentions.size(); i++) {
          builder.append("<annotator index='" + i + "'");
          EntityMention em = principalMentions.get(i);
          if (em != null) {
            builder.append(" entity='" + StringEscapeUtils.escapeXml11(em.entity) + "'");
          }
          if (evals.get(i).eval != null) {
            builder.append(" eval='" + evals.get(i).eval + "'");
            addAnnotatorInfo(i, evals, principalMentions, builder);
          }
          addChainInfo(null, evals.get(i), builder);
          // Note: newline character introduces a space between a mark and its before chain annotations
          builder.append("/>\n");
        }

      }

      // generate chain indices for super/subscript
      // doChainAnnotation(pair, chains, builder);

      // escape and print
      builder.append(escaped);
      // close mark tags
      while (openMarks.size() > 0) {
        openMarks.remove(openMarks.size() - 1);
        builder.append("</mark>\n");
      }

      last = pair;
    }

    return builder.toString().trim();
  }

  private void addAnnotatorInfo(int idx, List<MarkEval> evals, List<EntityMention> principalMentions, StringBuilder builder) {
    if (evals == null || evals.get(idx) == null) {
      return;
    }
    EntityMention emI = principalMentions.get(idx);

    if (emI != null && emI.info() != null) {
      for (Entry<String, String> info : emI.info().entrySet()) {
        builder.append(" " + info.getKey() + "='" + StringEscapeUtils.escapeXml10(info.getValue()) + "'");
      }
    }

    return;
  }

  /**
   *
   * @param evals
   * @param principalMentions
   * @return true if mark should be splitted
   */
  boolean evaluateMark(ComparePair lastPair, ComparePair pair, List<EntityMention> principalMentions, List<MentionChains> chains,
      List<MarkEval> evals) {
    MarkEval me = new MarkEval();
    me.chainBefore = chainAnnotationAttr(0, lastPair, PosType.START, chains);
    me.chainAfter = chainAnnotationAttr(0, pair, PosType.END, chains);
    evals.add(me);
    EntityMention em0 = principalMentions.get(0);
    String principalEvaluation = null;
    for (int i = 1; i < principalMentions.size(); i++) {
      EntityMention emI = principalMentions.get(i);
      String eval = null;
      if (em0 != null && em0.entity != null) {
        if (emI == null || emI.entity == null) {
          eval = "forgot";
        }
      }
      if (emI != null && emI.entity != null) {
        if (em0 != null && emI.entity.equals(em0.entity)) {
          eval = "correct";
        } else {
          if (em0 == null) {
            eval = "toomuch";
          } else {
            eval = "wrong";
          }
        }
      }
      if (eval == null) {
        eval = "";
      }
      me = new MarkEval();
      me.eval = eval;
      me.chainBefore = chainAnnotationAttr(i, lastPair, PosType.START, chains);
      me.chainAfter = chainAnnotationAttr(i, pair, PosType.END, chains);
      evals.add(me);
      if (principalEvaluation == null) {
        principalEvaluation = eval;
      } else if (!principalEvaluation.equals(eval)) {
        principalEvaluation = "split";
      }
    }
    return "split".equals(principalEvaluation);
  }

  private void addChainInfo(String idx, MarkEval eval, StringBuilder builder) {
    if (idx == null) {
      idx = "";
    }
    if (eval.chainBefore != null) {
      builder.append(" chain-before" + idx + "='" + eval.chainBefore + "'");
    }
    if (eval.chainAfter != null) {
      builder.append(" chain-after" + idx + "='" + eval.chainAfter + "'");
    }
  }

  /*private void doChainAnnotation(ComparePair pair, List<MentionChains> chains, StringBuilder builder) {
    String chainStr0 = chainAnnotationAttr(0, pair, chains);
    String chainStr1 = chainAnnotationAttr(1, pair, chains);
    if (chainStr0 != null || chainStr1 != null) {
      builder.append("<chain-mark");
      if (chainStr0 != null) {
        builder.append(" chain0='" + chainStr0 + "'");
      }
      if (chainStr1 != null) {
        builder.append(" chain1='" + chainStr1 + "'");
      }
      builder.append("/>\n");
    }
  }*/

  /**
   * Generate "(chainidx" or "chainidx" or "chainidx)" strings
   *
   * @param docIdx
   * @param pair
   * @param chains
   * @return
   */
  private String chainAnnotationAttr(int docIdx, ComparePair pair, PosType posType, List<MentionChains> chains) {
    if (pair == null || pair.getPos(docIdx) == null) {
      return null;
    }
    List<EntityMention> mentions = pair.getMentions(docIdx);
    if (mentions == null || mentions.size() == 0) {
      return null;
    }

    StringBuilder sb = new StringBuilder();
    boolean printIntermediates = !pair.emps.get(docIdx).stream().filter(emp -> (emp.posType == PosType.START || emp.posType == PosType.END)).findAny()
        .isPresent();
    for (EntityMentionPos emp : Utility.iterable(new ReverseListIterator<>(pair.emps.get(docIdx)))) {
      if (emp == null) {
        continue;
      }
      if (emp.posType != posType) {
        continue;
      }

      PosType pt = emp.posType;
      Chain c0 = chains.get(docIdx).mentionToChain.get(emp.em);
      if (c0 == null) {
        return null;
      }

      String chainStr = "" + c0.idx;
      switch (pt) {
        case START:
          chainStr = "(" + chainStr;
          break;
        case END:
          chainStr = chainStr + ")";
          break;
        case INTERMEDIATE:
          if (!printIntermediates) {
            chainStr = null;
          }
          break;
        default:
          log.warn("chainToStr cannot deal with pos type {} for compare pair", pt, pair);
      }
      if (chainStr != null) {
        if (sb.length() > 0) {
          sb.append(",");
        }
        sb.append(chainStr);
      }
    }
    return sb.toString();
  }

  private Map<String, String> getEntityRenameMap(List<EntityMention> mentions0, boolean rename, MentionChains chains) {
    Map<String, String> entityMentionToOutput0 = new HashMap<>();
    for (EntityMention m : mentions0) {
      String entity = m.entity;
      if (rename) {
        int idx = (entityMentionToOutput0.size() + 1);
        if (chains != null) {
          Chain c = chains.entityToChain.get(m);
          if (c != null) {
            idx = c.idx;
          }
        }
        int fidx = idx;
        entityMentionToOutput0.computeIfAbsent(entity,
            k -> StringEscapeUtils.escapeXml10(m.getMinMention().spanString()) + "; " + fidx + "; " + StringEscapeUtils.escapeXml10(entity));
      } else {
        entityMentionToOutput0.computeIfAbsent(entity, k -> StringEscapeUtils.escapeXml10(entity));
      }
    }
    return entityMentionToOutput0;
  }
}
