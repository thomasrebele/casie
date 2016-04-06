/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package tpt.dbweb.cat.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tpt.dbweb.cat.datatypes.TextSpan;

/**
 * Tokenize a string with a simple regex. Whitespace gets removed.
 * The output includes the split characters.
 * @author Thomas Rebele
 */
public class RegexWordTokenizer implements Tokenizer {

  private final static Logger log = LoggerFactory.getLogger(RegexWordTokenizer.class);

  Pattern pattern = Pattern.compile("(?=[^\\p{Alnum}])");

  @Override
  public List<TextSpan> getTokens(String text) {
    // split text and iterate over its parts
    String[] parts = pattern.split(text);
    List<TextSpan> result = new ArrayList<>();
    int pos = 0;
    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
      int nextPos = pos + part.length();
      try {
        // remove whitespace
        String ltrim = StringUtils.stripStart(part, null);
        String trim = StringUtils.stripEnd(ltrim, null);
        int start = pos + (part.length() - ltrim.length());
        int end = start + trim.length();
        // only add non-empty text spans
        if (start < end) {
          result.add(new TextSpan(text, start, end));
        }
      } finally {
        pos = nextPos;
      }
    }
    return result;
  }

  public static void main(String[] args) {
    Tokenizer tokenizer = new RegexWordTokenizer();
    List<TextSpan> sentences = tokenizer
        .getTokens("  First sentence is this some day he'll split up \n\nthe sentences. Second sentence, what the heck. ");
    log.info("{}", sentences);

  }
}
