/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package tpt.dbweb.cat.io;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tpt.dbweb.cat.datatypes.EntityMention;
import tpt.dbweb.cat.datatypes.TaggedText;

public class ConllReader {

  private final static Logger log = LoggerFactory.getLogger(ConllReader.class);

  /**
   * Read tabular conll file and parse the last column as coreferences.
   * @param file
   * @param textColumn
   * @return
   */
  public static List<TaggedText> readConllFile(Path file, int textColumn) {
    List<TaggedText> result = new ArrayList<>();
    Map<String, List<List<String>>> docs = ConllWriter.readConllDocuments(file);

    // for each document between '#begin document' and '#end document'
    docs.forEach((docId, table) -> {
      TaggedText tt = new TaggedText();
      tt.id = docId;

      // track starting corefs
      StringBuilder sb = new StringBuilder();
      Map<String, List<EntityMention>> entityIdToMentions = new HashMap<>();

      // for each line in document
      table.forEach(row -> {
        // add whitespace characters between words
        if (row.size() == 0) {
          sb.append("\n");
          return;
        }
        if (sb.length() > 0) {
          if (sb.charAt(sb.length() - 1) != '\n') {
            sb.append(" ");
          }
        }

        String word = row.get(textColumn);
        String corefCol = row.get(row.size() - 1);

        // parse starting/ending corefs
        List<String> corefs = Arrays.asList(corefCol.split("\\|"));
        List<String> endingCorefs = new ArrayList<>();
        corefs.forEach(coref -> {
          String entityId = coref.replace("(", "").replace(")", "");
          if (coref.startsWith("(")) {
            EntityMention em = new EntityMention(null, sb.length(), sb.length(), entityId);
            entityIdToMentions.computeIfAbsent(entityId, k -> new ArrayList<>()).add(em);
          }
          if (coref.endsWith(")")) {
            endingCorefs.add(entityId);
          }
        });

        // build text
        sb.append(word);

        // deal with ending corefs
        endingCorefs.forEach(coref -> {
          List<EntityMention> ems = entityIdToMentions.get(coref);
          if (ems.size() == 0) {
            log.error("coref {} has no starting coref?", coref);
            return;
          }

          EntityMention last = ems.remove(ems.size() - 1);
          last.end = sb.length();
          tt.mentions.add(last);
        });

      });

      // entity mentions which do not close
      for (String entityId : entityIdToMentions.keySet()) {
        for (EntityMention em : entityIdToMentions.get(entityId)) {
          log.error("coref {} has no endig tag", entityId);
        }
      }

      tt.text = sb.toString();
      tt.mentions.forEach(em -> em.text = tt.text);
      result.add(tt);
    });

    return result;
  }

}
