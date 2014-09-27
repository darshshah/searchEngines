
/**
 *  This class implements the OR operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;
import java.util.HashMap;
import java.util.TreeSet;

public class QryopSlOr extends QryopSl {

  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
   *  @param q A query argument (a query operator).
   */
  public QryopSlOr(Qryop... q) {
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

    if (r instanceof RetrievalModelUnrankedBoolean)
      return (evaluateBoolean (r));
    if (r instanceof RetrievalModelRankedBoolean)
    	return (evaluateRankedBoolean(r));

    return null;
  }

  /**
   *  Evaluates the query operator for Unranked boolean retrieval models,
   *  including any child operators and returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateBoolean (RetrievalModel r) throws IOException {

    //  Initialization

    allocDaaTPtrs (r);
    QryResult result = new QryResult ();

    //  Sort the arguments so that the shortest lists are first.  This
    //  improves the efficiency of exact-match OR without changing
    //  the result.

    for (int i=0; i<(this.daatPtrs.size()-1); i++) {
    	for (int j=i+1; j<this.daatPtrs.size(); j++) {
    		if (this.daatPtrs.get(i).scoreList.scores.size() > this.daatPtrs.get(j).scoreList.scores.size()) {
		    ScoreList tmpScoreList = this.daatPtrs.get(i).scoreList;
		    this.daatPtrs.get(i).scoreList = this.daatPtrs.get(j).scoreList;
		    this.daatPtrs.get(j).scoreList = tmpScoreList;
	}
      }
    }
    
    // take the longest list  i.e. the last one in the array.
    DaaTPtr ptr0 = this.daatPtrs.get(this.daatPtrs.size()-1);
    
    // The TreeSet will store docids. So, it will be sorted and duplicates will be removed automatically. 
    TreeSet<Integer> ts = new TreeSet<Integer>();
    double docScore = 1.0;
    
    // This is a term at a time kind of implementation. Take the doc id and put into a tree set
   
    for ( ; ptr0.nextDoc < ptr0.scoreList.scores.size(); ptr0.nextDoc ++) {

      int ptr0Docid = ptr0.scoreList.getDocid (ptr0.nextDoc);

      ts.add(ptr0Docid);
      
      for (int j=this.daatPtrs.size()-2; j >=0; j--) {

    	  DaaTPtr ptrj = this.daatPtrs.get(j);
    	  
    	  if (ptrj.nextDoc < ptr0.scoreList.scores.size() && ptrj.nextDoc <  ptrj.scoreList.scores.size() )
    	  {
    		  ts.add(ptrj.scoreList.getDocid (ptrj.nextDoc));  // add the docid to the treeSet
    		  ptrj.nextDoc++;	 
    	  }  			
      }
      
    }

    // Add the sorted docids to docScores.
    for (Integer docids : ts)
    {
    	result.docScores.add (docids.intValue(), docScore);
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

    if (r instanceof RetrievalModelUnrankedBoolean)
      return (0.0);

    return 0.0;
  }

  /**
   *  Evaluates the query operator for Rankedboolean retrieval models,
   *  including any child operators and returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateRankedBoolean (RetrievalModel r) throws IOException {
	
	    //  Initialization

	    allocDaaTPtrs (r);
	    QryResult result = new QryResult ();

	    //  Sort the arguments so that the shortest lists are first.  This
	    //  improves the efficiency of exact-match OR without changing
	    //  the result.

	    for (int i=0; i<(this.daatPtrs.size()-1); i++) {
	    	for (int j=i+1; j<this.daatPtrs.size(); j++) {
	    		if (this.daatPtrs.get(i).scoreList.scores.size() > this.daatPtrs.get(j).scoreList.scores.size()) {
			    ScoreList tmpScoreList = this.daatPtrs.get(i).scoreList;
			    this.daatPtrs.get(i).scoreList = this.daatPtrs.get(j).scoreList;
			    this.daatPtrs.get(j).scoreList = tmpScoreList;
		}
	      }
	    }

	    // take the longest list  i.e. the last one in the array.
	    DaaTPtr ptr0 = this.daatPtrs.get(this.daatPtrs.size()-1);
	    
	    // Hashmap will store the doc id and corresponding score. 
	    HashMap<Integer, Double> hm = new HashMap<Integer, Double>();
	    
	    for ( ; ptr0.nextDoc < ptr0.scoreList.scores.size(); ptr0.nextDoc ++) {

	      int ptr0Docid = ptr0.scoreList.getDocid (ptr0.nextDoc);
	      double docScore = ptr0.scoreList.getDocidScore(ptr0.nextDoc);
	      double max = docScore;
	      //  If docid already not present, then add the doc id.
	      if (!hm.containsKey(ptr0Docid))
	      {
	    	  hm.put(ptr0Docid, docScore);
	      }
	      else   // get the score of existing doc id and put the maximum score in it.  
	      {   
	    	  max = hm.get(ptr0.scoreList.getDocid (ptr0.nextDoc));
			  if(max < docScore) 
			  {
				  hm.put(ptr0.scoreList.getDocid (ptr0.nextDoc), docScore);
			  }  
	      }
	      
	      for (int j=this.daatPtrs.size()-2; j >=0; j--) {

	    	  DaaTPtr ptrj = this.daatPtrs.get(j);
	    	  
	    	  //  If docid already not present, then add the doc id.
	    	  if (ptrj.nextDoc < ptr0.scoreList.scores.size() && ptrj.nextDoc <  ptrj.scoreList.scores.size() )
	    	  {
	    		  if (!hm.containsKey(ptrj.scoreList.getDocid (ptrj.nextDoc)))
	    	      {
	    	    	  hm.put(ptrj.scoreList.getDocid (ptrj.nextDoc), ptrj.scoreList.getDocidScore (ptrj.nextDoc));
	    	      }
	    		  else   // get the score of existing doc id and put the maximum score in it.
	    		  {  
	    			  max = hm.get(ptrj.scoreList.getDocid (ptrj.nextDoc));
	    			  if(max < ptrj.scoreList.getDocidScore (ptrj.nextDoc)) 
	    			  {
	    				  hm.put(ptrj.scoreList.getDocid (ptrj.nextDoc), ptrj.scoreList.getDocidScore (ptrj.nextDoc));
	    			  }  
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
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (int i=0; i<this.args.size(); i++)
      result += this.args.get(i).toString() + " ";

    return ("#OR( " + result + ")");
  }
}
