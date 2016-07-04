package tpt.dbweb.cat.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractInitials {

  private static Pattern tokenizer = Pattern.compile("[\\w&&[^_]]+");

  public static String getInitials(String str) {
    if (str == null || str.length() == 0) return "";
    StringBuilder sb = new StringBuilder();
    Matcher m = tokenizer.matcher(str);
    String group = null;
    while (m.find()) {
      group = m.group();
      sb.append(m.group().charAt(0));
    }
    if (sb.length() < 2 && group != null) {
      sb.append(group.substring(1, Math.min(group.length(), 2)));
    }
    return sb.toString();
  }

  public static void main(String[] args) {

    System.out.println(getInitials("Bertrand_Russel"));
    System.out.println(getInitials("missiles"));
    System.out.println(getInitials("{John_Russel,Katharine_Russel}"));

  }
}
