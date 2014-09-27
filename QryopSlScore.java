/**
 *  This class implements the SCORE operator for all retrieval models.
 *  The single argument to a score operator is a query operator that
 *  produces an inverted list.  The SCORE operator uses this
 *  information to produce a score list that contains document ids and
 *  scores.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlScore extends QryopSl {

  /**
   *  Construct a new SCORE operator.  The SCORE operator accepts just
   *  one argument.
   *  @param q The query operator argument.
   *  @return @link{QryopSlScore}
   */
  public QryopSlScore(Qryop q) {
    this.args.add(q);
  }

  /**
   *  Construct a new SCORE operator.  Allow a SCORE operator to be
   *  created with no arguments.  This simplifies the design of some
   *  query parsing architectures.
   *  @return @link{QryopSlScore}
   */
  public QryopSlScore() {
  }

  /**
   *  Appends an argument to the list of query operator arguments.  This
   *  simplifies the design of some query parsing architectures.
   *  @param q The query argument to append.
   */
  public void add (Qryop a) {
    this.args.add(a);
  }

  /**
   *  Evaluate the query operator.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluate(RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean)
      return (evaluateBoolean (r));
    if (r instanceof RetrievalModelRankedBoolean)
    	return (evaluateRankedBoolean (r));
    if (r instanceof RetrievalModelBM25)
    	return (evaluateRankedBM25 (r));

    return null;
  }

/**
   *  Evaluate the query operator for boolean retrieval models.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

    // Evaluate the query argument.

    QryResult result = args.get(0).evaluate(r);

    // Each pass of the loop computes a score for one document. Note:
    // If the evaluate operation above returned a score list (which is
    // very possible), this loop gets skipped.

    for (int i = 0; i < result.invertedList.df; i++) {

      // DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY. 
      // Unranked Boolean. All matching documents get a score of 1.0.

      result.docScores.add(result.invertedList.postings.get(i).docid,
			   (float) 1.0);
    }

    // The SCORE operator should not return a populated inverted list.
    // If there is one, replace it with an empty inverted list.

    if (result.invertedList.df > 0)
	result.invertedList = new InvList();

    return result;
  }

  /*
   *  Calculate the default score for a document that does not match
   *  the query argument.  This score is 0 for many retrieval models,
   *  but not all retrieval models.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @param docid The internal id of the document that needs a default score.
   *  @return The default score.
   */
  public double getDefaultScore (RetrievalModel r, long docid) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean)
      return (0.0);

    return 0.0;
  }
  
  /**
   *  Evaluate the query operator for boolean retrieval models.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateRankedBoolean(RetrievalModel r) throws IOException {
	// Evaluate the query argument.

	    QryResult result = args.get(0).evaluate(r);

	    // Each pass of the loop computes a score for one document. Note:
	    // If the evaluate operation above returned a score list (which is
	    // very possible), this loop gets skipped.
	    
	    for (int i = 0; i < result.invertedList.df; i++) {

	      // DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY. 
	      // Ranked Boolean. All matching documents get a score of tf.

	      result.docScores.add(result.invertedList.postings.get(i).docid,
				   (float) result.invertedList.postings.get(i).tf);
	    }

	    // The SCORE operator should not return a populated inverted list.
	    // If there is one, replace it with an empty inverted list.

	    if (result.invertedList.df > 0)
		result.invertedList = new InvList();

	    return result;
  }
  
  public QryResult evaluateRankedBM25(RetrievalModel r) throws IOException {
		
	  QryResult result = args.get(0).evaluate(r);

	    // Each pass of the loop computes a score for one document. Note:
	    // If the evaluate operation above returned a score list (which is
	    // very possible), this loop gets skipped.
	    
	  	// Total Documents in the corpus
	    int N = QryEval.READER.numDocs();
	    // Number of docs in the collection which has this term. i.e. document freq
	    int dfreq = result.invertedList.df;
	    // avg doclen for whole collection. It is dependent upon the field. 
	    // the total number of term occurrences in all 'x' field/ 
	    //  number of documents that have 'x' field
	    double avgDocLen = QryEval.READER.getSumTotalTermFreq(result.invertedList.field) / QryEval.READER.getDocCount (result.invertedList.field);
	    // RSJ weight (the collection or idf weight)
	    double RSJweight = (double) Math.log((N - dfreq + 0.5)/(dfreq + 0.5));
	    // BM25 tunable params
	    double k1 = r.k_1;
	    double b = r.b;
	    int termFreq = 0;
	    long docLen = 0;
	    
	    for (int i = 0; i < result.invertedList.df; i++) {

	     // the term freq of term inside the document. 
	     termFreq = result.invertedList.postings.get(i).tf;
	     // lenght of the current document with the field x
	     docLen = QryEval.DocLenStore.getDocLength(result.invertedList.field, result.invertedList.postings.get(i).docid);
	     
	     // calculating the tf weight/doc weight
	     double DOCweight = termFreq / (termFreq + k1*((1-b) + b*(docLen/avgDocLen)));
	     
	      result.docScores.add(result.invertedList.postings.get(i).docid,
				   (double) (RSJweight*DOCweight));
	    }

	    // The SCORE operator should not return a populated inverted list.
	    // If there is one, replace it with an empty inverted list.

	    if (result.invertedList.df > 0)
		result.invertedList = new InvList();

	    return result;
	}
  

  /**
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (Iterator<Qryop> i = this.args.iterator(); i.hasNext(); )
      result += (i.next().toString() + " ");

    return ("#SCORE( " + result + ")");
  }
}
