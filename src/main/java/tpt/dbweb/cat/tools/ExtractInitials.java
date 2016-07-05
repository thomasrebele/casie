package tpt.dbweb.cat.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractInitials {

  private static Pattern tokenizer = Pattern.compile("([\\w&&[^_0-9]]+)|(\\d+)");

  public static String getInitials(String str) {
    if (str == null || str.length() == 0) return "";
    StringBuilder sb = new StringBuilder();
    Matcher m = tokenizer.matcher(str);
    String group = null;
    while (m.find()) {
      group = m.group(1);
      if (group != null && group.length() > 0) {
        sb.append(group.charAt(0));
        continue;
      }
      group = m.group(2);
      if (group != null && group.length() > 0) {
        sb.append(group);
      }
      group = null;
    }
    if (sb.length() < 2 && group != null) {
      sb.append(group.substring(1, Math.min(group.length(), 3)));
    }
    return sb.toString();
  }

  public static void main(String[] args) {

    System.out.println(getInitials("Bertrand_Russel"));
    System.out.println(getInitials("missiles"));
    System.out.println(getInitials("mi"));
    System.out.println(getInitials("{John_Russel,Katharine_Russel}"));
    System.out.println(getInitials("{John_Russel123,Katharine_Russel}"));
    System.out.println(getInitials("123"));

  }
}
