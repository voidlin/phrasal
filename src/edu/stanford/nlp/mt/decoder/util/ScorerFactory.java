package edu.stanford.nlp.mt.decoder.util;

import java.io.*;

import edu.stanford.nlp.stats.Counter;

/**
 *
 * @author danielcer
 *
 */
public class ScorerFactory {

  public static final String STATIC_SCORER = "staticscorer";
  public static final String UNIFORM_SCORER = "uniformscorer";
  public static final String DEFAULT_SCORER = UNIFORM_SCORER;

  public static final String STATIC_SCORER_INLINE = "inline";
  public static final String STATIC_SCORER_FILE = "file";

  private ScorerFactory() {
  }

  /**
   *
   * @throws IOException
   */
  public static Scorer<String> factory(String scorerName, Counter<String> config)
      throws IOException {

    if (scorerName.equals(UNIFORM_SCORER)) {
      return new UniformScorer<String>();
    } else if (scorerName.equals(STATIC_SCORER)) {
      return new StaticScorer(config);
    }

    throw new RuntimeException(String.format("Unknown scorer \"%s\"",
        scorerName));
  }

}
