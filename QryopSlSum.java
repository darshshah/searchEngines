
/**
 *  This class implements the OR operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;
import java.util.HashMap;
import java.util.TreeSet;

public class QryopSlSum extends QryopSl {

  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
   *  @param q A query argument (a query operator).
   */
  public QryopSlSum(Qryop... q) {
    for (int i = 0; i < q.length; i++)
      this.args.add(q[i]);
  }

  /**
   *  Appends an argument to the list of query operator arguments.  This
   *  simplifies the design of some query parsing architectures.
   *  @param {q} q The query argument (query operator) to append.
   *  @return void
   *  @throws IOException
   */
  public void add (Qryop a) {
    this.args.add(a);
  }

  /**
   *  Evaluates the query operator, including any child operators and
   *  returns the result.
   *  Will call different functions for ranked and unranked booleans
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluate(RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelBM25)
      return (evaluateBM25 (r));
  
    return null;
  }

  /**
   *  Evaluates the query operator for BM25 retrieval models,
   *  including any child operators and returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateBM25 (RetrievalModel r) throws IOException {

    //  Initialization
	double k3 = r.k_3;
	int qtf = 1; // hardcoding to one for now.
	double USERweight = ((k3 + 1)*qtf)/(k3 + qtf);
	
    allocDaaTPtrs (r);
    QryResult result = new QryResult ();

    //  Sort the arguments so that the shortest lists are first.  This
    //  improves the efficiency of exact-match OR without changing
    //  the result.
    int max = this.daatPtrs.get(0).scoreList.scores.size();
    int maxindex = 0;
    
    for (int i=1; i< this.daatPtrs.size(); i++) {
    	if (max < this.daatPtrs.get(i).scoreList.scores.size() )
    	{
    		max = this.daatPtrs.get(i).scoreList.scores.size();
    		maxindex = i;
    	}
    }
    	
	ScoreList tmpScoreList = this.daatPtrs.get(0).scoreList;
    this.daatPtrs.get(0).scoreList = this.daatPtrs.get(maxindex).scoreList;
    this.daatPtrs.get(maxindex).scoreList = tmpScoreList;
    

    // take the longest list  i.e. the last one in the array.
    DaaTPtr ptr0 = this.daatPtrs.get(0);
    
    // Hashmap will store the doc id and corresponding score. 
    HashMap<Integer, Double> hm = new HashMap<Integer, Double>();
    double currScore = 0;
    
    for ( ; ptr0.nextDoc < ptr0.scoreList.scores.size(); ptr0.nextDoc ++) {

      int ptr0Docid = ptr0.scoreList.getDocid (ptr0.nextDoc);
   
      //  If docid already not present, then add the doc id.
      if (!hm.containsKey(ptr0Docid))
      {
    	  hm.put(ptr0Docid, ptr0.scoreList.getDocidScore(ptr0.nextDoc)*USERweight);
      }
      else   // get the score of existing doc id and put the maximum score in it.  
      {   
    	  currScore = hm.get(ptr0.scoreList.getDocid (ptr0.nextDoc));
    	  hm.put(ptr0.scoreList.getDocid (ptr0.nextDoc), currScore + ptr0.scoreList.getDocidScore(ptr0.nextDoc)*USERweight);
      }
      
      for (int j=1; j < this.daatPtrs.size(); j++) {

    	  DaaTPtr ptrj = this.daatPtrs.get(j);
    	  
    	  //  If docid already not present, then add the doc id.
    	  if (ptrj.nextDoc < ptr0.scoreList.scores.size() && ptrj.nextDoc <  ptrj.scoreList.scores.size() )
    	  {
    		  if (!hm.containsKey(ptrj.scoreList.getDocid (ptrj.nextDoc)))
    	      {
    	    	  hm.put(ptrj.scoreList.getDocid (ptrj.nextDoc), ptrj.scoreList.getDocidScore (ptrj.nextDoc)*USERweight);
    	      }
    		  else   // get the score of existing doc id and put the maximum score in it.
    		  {  
    			  currScore = hm.get(ptrj.scoreList.getDocid (ptrj.nextDoc));
    			  hm.put(ptrj.scoreList.getDocid (ptrj.nextDoc), currScore + ptrj.scoreList.getDocidScore(ptrj.nextDoc)*USERweight);
    		  }
    		  
    		  ptrj.nextDoc++;	 
    	  }  			
      }
      
    }

    // Put the docid and score into docScores. Sorting is handled in the QryEval function.
    for (Integer docids : hm.keySet())
    {
    	result.docScores.add (docids.intValue(), hm.get(docids));
    }
    
    freeDaaTPtrs ();

    return result;
  }

  /*
   *  Calculate the default score for the specified document if it
   *  does not match the query operator.  This score is 0 for many
   *  retrieval models, but not all retrieval models.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @param docid The internal id of the document that needs a default score.
   *  @return The default score.
   */
  public double getDefaultScore (RetrievalModel r, long docid) throws IOException {

    if (r instanceof RetrievalModelBM25)
      return (0.0);

    return 0.0;
  }
  
  
  /*
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (int i=0; i<this.args.size(); i++)
      result += this.args.get(i).toString() + " ";

    return ("#SUM( " + result + ")");
  }
}
