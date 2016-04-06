/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package tpt.dbweb.cat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;

import tpt.dbweb.cat.evaluation.ComparisonResult;
import tpt.dbweb.cat.evaluation.ReferenceEvaluator;

/**
 *
 * @author Thomas Rebele
 *
 */
public class Main {

  private final static Logger log = LoggerFactory.getLogger(Main.class);

  public static class Options {

    @Parameter(names = "-h")
    public boolean showHelp = false;

    @Parameter(names = "--tmp-dir")
    public String tmpDirectory = null;

    @Parameter(names = "--run-rcs", description = "run reference-coreference-scorers")
    public boolean runReferenceCoreferenceScorers = true;

    @ParametersDelegate
    Compare.Options compareOptions = new Compare.Options();

    @ParametersDelegate
    ReferenceEvaluator.Options refEvalOptions = new ReferenceEvaluator.Options();
  }

  public static void main(String[] args) throws IOException {
    /*args = new String[] { "--out",
        "doc/examples/russel-out.xml",
        "doc/examples/russel.xml",
        "doc/examples/russel-1.xml",
        "doc/examples/russel-2.xml",
    };*/

    // parse options
    Options options = new Options();
    if (args.length > 0) {
      JCommander jc = new JCommander(options);
      try {
        jc.parse(args);
      } catch (ParameterException e) {
        log.error(e.getMessage());
        jc.usage();
        System.exit(0);
      }
      if (options.showHelp) {
        jc.usage();
        System.exit(0);
      }
    }
    if (options.tmpDirectory == null) {
      options.tmpDirectory = System.getProperty("java.io.tmpdir");
    }

    if (options.compareOptions.input.size() < 1) {
      System.out.println("CAT needs at least one file as input");
      System.exit(-1);
    }
    if (options.compareOptions.input.size() == 1) {
      options.compareOptions.input.add(options.compareOptions.input.get(0));
      options.runReferenceCoreferenceScorers = false;
    }
    List<Path> paths = new ArrayList<>();
    for (String inputFile : options.compareOptions.input) {
      paths.add(Paths.get(inputFile));
    }

    // calculate measures of coreference chains
    List<ComparisonResult> cmp = new ArrayList<>();
    if (options.runReferenceCoreferenceScorers) {
      ReferenceEvaluator evaluator = new ReferenceEvaluator(options.refEvalOptions);
      for (int i = 1; i < paths.size(); i++) {
        cmp.add(evaluator.compareXMLFiles(paths.get(0), paths.get(i), Paths.get(options.tmpDirectory + "/conll-format/")));
      }
    }

    // compare files
    Compare.compareXML(options.compareOptions, cmp);

  }
}
