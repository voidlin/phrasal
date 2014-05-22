package edu.stanford.nlp.mt.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.stanford.nlp.mt.Phrasal;
import edu.stanford.nlp.mt.metrics.CorpusLevelMetricFactory;
import edu.stanford.nlp.mt.metrics.EvaluationMetric;
import edu.stanford.nlp.mt.metrics.IncrementalEvaluationMetric;
import edu.stanford.nlp.mt.metrics.Metrics;
import edu.stanford.nlp.mt.metrics.SentenceLevelMetricFactory;
import edu.stanford.nlp.mt.util.IOTools;
import edu.stanford.nlp.mt.util.IString;
import edu.stanford.nlp.mt.util.IStrings;
import edu.stanford.nlp.mt.util.RichTranslation;
import edu.stanford.nlp.mt.util.ScoredFeaturizedTranslation;
import edu.stanford.nlp.mt.util.Sequence;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.Generics;

/**
 * Plot a BLEU learning curve from a list of intermediate weights generated by OnlineTuner.
 * 
 * @author Spence Green
 *
 */
public class OnlineLearningCurve {

  /**
   * @param args
   * @throws IOException 
   */
  public static void main(String[] args) throws IOException {
    if (args.length < 5) {
      System.err.printf("Usage: java %s ini_file input_file ref_csv_list sl_metric wts [wts]%n", 
          OnlineLearningCurve.class.getName());
      System.exit(-1);
    }
    String iniFile = args[0];
    String sourceFile = args[1];
    String[] refFiles = args[2].split(",");
    String slMetricStr = args[3];
    String clMetricStr = SentenceLevelMetricFactory.sentenceLevelToCorpusLevel(slMetricStr);
    System.err.printf("Evaluation metric: %s%n", clMetricStr.toUpperCase());
    
    // Read the references and apply NIST tokenization
    final List<List<Sequence<IString>>> references = Metrics.readReferences(refFiles, true);
    
    // Read the list of weights
    List<String> wts = new ArrayList<String>();
    for (int i = 4; i < args.length; ++i) {
      wts.add(args[i]);
    }

    System.err.println("Loading Phrasal...");
    Phrasal p = null;
    Map<String, List<String>> config = IOTools.readConfigFile(iniFile);
    // Don't write an nbest list
    config.remove(Phrasal.NBEST_LIST_OPT);
    if ( ! config.containsKey(Phrasal.LOG_PREFIX)) {
      int delim = iniFile.lastIndexOf('.');
      String logFilePrefix = iniFile.substring(0, delim) + ".learn";
      List<String> params = Generics.newArrayList(1);
      params.add(logFilePrefix);
      config.put(Phrasal.LOG_PREFIX, params);
    }
    p = Phrasal.loadDecoder(config);

    final int numThreads = p.getNumThreads();
    for (String wtsFile : wts) {
      Counter<String> w = IOTools.readWeights(wtsFile);
      for (int i = 0; i < numThreads; ++i) {
        p.getScorer(i).updateWeights(w);
      }
      
      System.err.printf("Decoding with %s%n", wtsFile);
      
      // Decode
      List<RichTranslation<IString,String>> translations = 
          p.decode(new FileInputStream(new File(sourceFile)), false);
      if (translations.size() != references.size()) {
        throw new RuntimeException("Number of translations does not match reference list size");
      }
      
      // Corpus-level evaluation
      EvaluationMetric<IString,String> metric = CorpusLevelMetricFactory.newMetric(clMetricStr, references);
      IncrementalEvaluationMetric<IString,String> incMetric = metric.getIncrementalMetric();
      for (RichTranslation<IString,String> translation : translations) {
        // Apply NIST tokenization so that the learning curve accurately reflects
        // the BLEUMetric evaluation (see the main() method of that class).
        String translationStr = NISTTokenizer.tokenize(translation.translation.toString());
        ScoredFeaturizedTranslation<IString,String> tr = 
            new ScoredFeaturizedTranslation<IString,String>(IStrings.tokenize(translationStr), 
                translation.features, translation.score);
        incMetric.add(tr);
      }
      System.out.printf("%s\t%.2f%n",wtsFile,100.0*incMetric.score());
    }
  }
}
