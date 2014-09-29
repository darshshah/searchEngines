/**
 *  This class implements the NEAR operator for all retrieval models.
 *  #NEAR/3(apple pie) will find apple pie, apple x pie and apple x x pie
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class QryopIlNear extends QryopIl {

	int delta = 0;   // will store the Near/# term
	
	public QryopIlNear(int num) {
		  this.delta = num;
	}
	
  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new QryopIlSyn (arg1, arg2, arg3, ...).
   */
  public QryopIlNear(Qryop... q) {
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
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluate(RetrievalModel r) throws IOException {

    //  Initialization

    allocDaaTPtrs (r);
    syntaxCheckArgResults (this.daatPtrs);
    QryResult result = new QryResult ();
    int nearFreq = 0; // This stores the frequency of occurence of near teem. 
    
    DaaTPtr ptr0 = this.daatPtrs.get(0);
    result.invertedList.field =  new String (ptr0.invList.field);
    
    EVALUATEDOCUMENTS:
    for ( ; ptr0.nextDoc < ptr0.invList.postings.size(); ptr0.nextDoc ++) {

      int ptr0Docid = ptr0.invList.getDocid (ptr0.nextDoc);

      //  Do the other query arguments have the ptr0Docid?

      for (int j=1; j<this.daatPtrs.size(); j++) {

    	  DaaTPtr ptrj = this.daatPtrs.get(j);
    	  
    	  
			while (true) {
			  int currdocid = 0; 
				
			  if (ptrj.nextDoc >= ptrj.invList.postings.size())
			    break EVALUATEDOCUMENTS;		// No more docs can match
			  else
			    if ((currdocid = ptrj.invList.getDocid (ptrj.nextDoc)) > ptr0Docid)
			      continue EVALUATEDOCUMENTS;	// The ptr0docid can't match.
			  else
			    if ((currdocid = ptrj.invList.getDocid (ptrj.nextDoc)) < ptr0Docid)
			      ptrj.nextDoc ++;			// Not yet at the right doc.
			  else
			      break;				// ptrj matches ptr0Docid
			}
      }

      //  The ptr0Docid matched all query arguments. So all doc ids are same.
      // Now test for near-ness
     
      //  savedpos array is used for storing the position indexes of all Daat lists.(query terms) 
      int [] savedpos = new int [this.daatPtrs.size()];   
     
      nearFreq = 0;  // reset freq for each docid
      int pos=0; 
      ArrayList<Integer> positions = new ArrayList<Integer>();
      // The outer loop will iterate through all the doc positings for the first term
      // The internal loop will search the other terms in query order
      EVALUATEPOSITIONS:
      for (int k = 0; k < ptr0.invList.postings.elementAt(ptr0.nextDoc).positions.size(); k++)
      {
    	  int poscompare = ptr0.invList.postings.elementAt(ptr0.nextDoc).positions.get(k);
    	  
    	  for (int j=1; j<this.daatPtrs.size(); j++) 
    	  {
    		  DaaTPtr ptrj = this.daatPtrs.get(j);
   	      
	    	 while(true)
	    	  { 
	    		  if (savedpos[j] >= ptrj.invList.postings.elementAt(ptrj.nextDoc).positions.size() ) // end of list
	    		  {
	    			 break EVALUATEPOSITIONS;
	    		  }
	    		  else if (poscompare  >  (pos = ptrj.invList.postings.elementAt(ptrj.nextDoc).positions.get(savedpos[j])))  // not yet
	    		  {
	    			  savedpos[j] =  savedpos[j] + 1;
	    		  }
	    		  else if (poscompare <  (pos = ptrj.invList.postings.elementAt(ptrj.nextDoc).positions.get(savedpos[j])) && ((pos = ptrj.invList.postings.elementAt(ptrj.nextDoc).positions.get(savedpos[j])) - poscompare) > this.delta) // didn't match
	    		  {
	    			  continue EVALUATEPOSITIONS;
	    		  }
	    		  else	// matched
	    		  {
	    			  break;
	    		  }
	    		  
	    	  }
	    	 
	    	// update poscompare to have position of next list to compare in array.
   		  poscompare = ptrj.invList.postings.elementAt(ptrj.nextDoc).positions.get(savedpos[j]);
        } 	  
    	  // increment the occurence frequency.
    	  nearFreq++;
    	  // Save the pos compare here. It matched at postition k. So, add the position k to the list.
    	  positions.add(ptr0.invList.postings.elementAt(ptr0.nextDoc).positions.get(k));  
    	  // increment the position index of jth list as we already processed the current positon index.
    	  for (int l = 1; l <this.daatPtrs.size(); l++ )
    	  {
    		  savedpos[l] =  savedpos[l] + 1;  
    	  }
    	  
      }
   
      	if (nearFreq > 0) {  
      	
      	  if (r instanceof RetrievalModelUnrankedBoolean)  {
      		  result.invertedList.add(ptr0Docid, 1);  // put freq as 1 for unranked boolean
      	  }
      	  else {
      		  result.invertedList.appendPosting(ptr0Docid, positions);
      	  }
      	}
	    
    }
   
    freeDaaTPtrs ();
   
    return result;
  }
     

  /**
   *  Return the smallest unexamined docid from the DaaTPtrs.
   *  @return The smallest internal document id.
   */
  public int getSmallestCurrentDocid () {

    int nextDocid = Integer.MAX_VALUE;

    for (int i=0; i<this.daatPtrs.size(); i++) {
      DaaTPtr ptri = this.daatPtrs.get(i);
      if (nextDocid > ptri.invList.getDocid (ptri.nextDoc))
	nextDocid = ptri.invList.getDocid (ptri.nextDoc);
      }

    return (nextDocid);
  }

  /**
   *  syntaxCheckArgResults does syntax checking that can only be done
   *  after query arguments are evaluated.
   *  @param ptrs A list of DaaTPtrs for this query operator.
   *  @return True if the syntax is valid, false otherwise.
   */
  public Boolean syntaxCheckArgResults (List<DaaTPtr> ptrs) {

    for (int i=0; i<this.args.size(); i++) {

      if (! (this.args.get(i) instanceof QryopIl)) 
	QryEval.fatalError ("Error:  Invalid argument in " +
			    this.toString());
      else
	if ((i>0) &&
	    (! ptrs.get(i).invList.field.equals (ptrs.get(0).invList.field)))
	  QryEval.fatalError ("Error:  Arguments must be in the same field:  " +
			      this.toString());
    }

    return true;
  }

  /*
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (Iterator<Qryop> i = this.args.iterator(); i.hasNext(); )
      result += (i.next().toString() + " ");

    return ("#NEAR( " + result + ")");
  }
}
