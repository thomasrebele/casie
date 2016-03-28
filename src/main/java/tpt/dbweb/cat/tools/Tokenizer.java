package tpt.dbweb.cat.tools;

import java.util.List;

import tpt.dbweb.cat.datatypes.TextSpan;

public interface Tokenizer {

  public List<TextSpan> getTokens(String text);
}
