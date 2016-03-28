package tpt.dbweb.cat.evaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import tpt.dbweb.cat.datatypes.Fraction;

public class ValueEvaluationStatistics extends EvaluationStatistics {

  private static Logger log = LoggerFactory.getLogger(ValueEvaluationStatistics.class);

  @JsonProperty("recall")
  private Fraction recall = Fraction.EMPTY;

  @JsonProperty("precision")
  private Fraction precision = Fraction.EMPTY;

  @JsonProperty("weight")
  private Fraction weight = Fraction.ONE;

  public ValueEvaluationStatistics() {
    this.weight = Fraction.EMPTY;
  }

  public ValueEvaluationStatistics(Fraction recall, Fraction precision) {
    this.recall = recall;
    this.precision = precision;
  }

  public ValueEvaluationStatistics(Fraction recall, Fraction precision, Fraction weight) {
    this.recall = recall;
    this.precision = precision;
    this.weight = weight;
  }

  @Override
  @JsonIgnore
  public float getRecall() {
    return (float) recall.value(0);
  }

  @Override
  @JsonIgnore
  public float getPrecision() {
    return (float) precision.value(0);
  }

  /**
   * Doesn't support weights!
   */
  @Override
  public void addMicro(EvaluationStatistics r) {
    if (r instanceof ValueEvaluationStatistics) {
      Fraction newRecall = this.recall;
      Fraction newPrecision = this.precision;

      if (newPrecision.value(0) > 1 || newRecall.value(0) > 1) {
        log.error("micro: recall or precision bigger than 1 in {}", this);
      }

      ValueEvaluationStatistics ri = (ValueEvaluationStatistics) r;
      Fraction factor = new Fraction(ri.weight.value(), ri.weight.value());
      newRecall = recall.addMicro(factor.multiply(ri.recall));
      newPrecision = precision.addMicro(factor.multiply(ri.precision));
      this.weight = Fraction.ONE;
      info.putAll(ri.info);

      if (newPrecision.value(0) > 1 || newRecall.value(0) > 1) {
        log.error("micro: recall or precision bigger than 1 in {}", this);
      }

      this.recall = newRecall;
      this.precision = newPrecision;
    }
  }

  @Override
  public void addMacro(EvaluationStatistics r) {
    if (r instanceof ValueEvaluationStatistics) {
      if (r.getPrecision() > 1 || r.getRecall() > 1) {
        log.error("macro input: recall or precision bigger than 1 in {}", r);
      }

      ValueEvaluationStatistics ri = (ValueEvaluationStatistics) r;
      Fraction newWeight = weight.add(ri.weight);
      this.recall = (weight.multiply(recall).add(ri.weight.multiply(ri.recall))).divide(newWeight);
      this.precision = (weight.multiply(precision).add(ri.weight.multiply(ri.precision))).divide(newWeight);
      this.weight = newWeight;
      info.putAll(ri.info);

      if (getPrecision() > 1 || getRecall() > 1) {
        log.error("macro: recall or precision bigger than 1 in {}", this);
      }
    }
  }

  @Override
  public String toString() {
    return "recall: " + formatRecall() + "% precision: " + formatPrecision() + "% f1: " + formatF1() + "%  " + (info != null ? info : ""); // + " (weight: " + weight + ")";
  }

}
