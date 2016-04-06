/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package tpt.dbweb.cat.evaluation;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class EvaluationStatistics {

  public Map<String, String> info = new HashMap<String, String>();

  public abstract float getRecall();

  public abstract float getPrecision();

  @JsonIgnore
  public float getF1() {
    float precision = getPrecision();
    float recall = getRecall();
    if (precision == 0 && recall == 0) return 0;
    return 2 * precision * recall / (precision + recall);
  }

  public void addMicro(EvaluationStatistics eval) {
    throw new UnsupportedOperationException();
  }

  public void addMacro(EvaluationStatistics eval) {
    throw new UnsupportedOperationException();
  }

  public String fmt(float val) {
    return new DecimalFormat("#.00").format(val);
  }

  public String formatRecall() {
    return fmt(100 * getRecall());
  }

  public String formatPrecision() {
    return fmt(100 * getPrecision());
  }

  public String formatF1() {
    return fmt(100 * getF1());
  }

}
