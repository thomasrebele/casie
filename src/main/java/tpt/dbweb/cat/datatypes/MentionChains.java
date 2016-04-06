/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package tpt.dbweb.cat.datatypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import tpt.dbweb.cat.tools.Utility;

public class MentionChains {

  public static class Chain {

    public int idx;

    Set<EntityMention> mentions;
  }

  public MentionChains(List<EntityMention> mentions) {
    mentions.forEach(em -> {
      if (em.entity != null) {
        this.entityToChain.compute(em.entity, (key, set) -> {
          if (set == null) {
            set = new Chain();
            set.idx = this.entityToChain.size() + 1;
          }

          set.mentions = Utility.addToSet(set.mentions, em);
          return set;
        });
      }
    });

    entityToChain.forEach((k, v) -> {
      v.mentions.forEach(em -> mentionToChain.put(em, v));
    });
  }

  public Map<String, Chain> entityToChain = new HashMap<>();

  public Map<EntityMention, Chain> mentionToChain = new TreeMap<>();
}
