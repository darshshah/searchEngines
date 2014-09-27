/**
 *  QryEval illustrates the architecture for the portion of a search
 *  engine that evaluates queries.  It is a template for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 *  Modified by Darsh Shah (darshs)
 *  
 */

import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class QryEval {

  static String usage = "Usage:  java " + System.getProperty("sun.java.command")
      + " paramFile\n\n";

  //  The index file reader is accessible via a global variable. This
  //  isn't great programming style, but the alternative is for every
  //  query operator to store or pass this value, which creates its
  //  own headaches.

  public static IndexReader READER;

  //  Create and configure an English analyzer that will be used for
  //  query parsing.

  public static EnglishAnalyzerConfigurable analyzer =
      new EnglishAnalyzerConfigurable (Version.LUCENE_43);
  static {
    analyzer.setLowercase(true);
    analyzer.setStopwordRemoval(true);
    analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
  }

  /**
   *  @param args The only argument is the path to the parameter file.
   *  @throws Exception
   */
  public static void main(String[] args) throws Exception {
    
    // must supply parameter file
    if (args.length < 1) {
      System.err.println(usage);
      System.exit(1);
    }

    // read in the parameter file; one parameter per line in format of key=value
    Map<String, String> params = new HashMap<String, String>();
    Scanner scan = new Scanner(new File(args[0]));
    String line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split("=");
      params.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());
    scan.close();
    
    // parameters required for this example to run
    if (!params.containsKey("indexPath")) {
      System.err.println("Error: Parameters were missing.");
      System.exit(1);
    }

    // open the index
    READER = DirectoryReader.open(FSDirectory.open(new File(params.get("indexPath"))));

    if (READER == null) {
      System.err.println(usage);
      System.exit(1);
    }

    DocLengthStore s = new DocLengthStore(READER);
    RetrievalModel model = null;
    if(params.get("retrievalAlgorithm").equals("UnrankedBoolean"))
    {
    	model = new RetrievalModelUnrankedBoolean();
    }
    else if (params.get("retrievalAlgorithm").equals("RankedBoolean")) 
    {
    	model = new RetrievalModelRankedBoolean();
    }
    else if (params.get("retrievalAlgorithm").equals("BM25"))
    {
    	model = new RetrievalModelBM25();
    	if (!model.setParameter("k_1", params.get("BM25:k_1"))) {
    		System.err.println("incorrect BM25 parameter");
        	System.exit(1);
    	}
    	if (!model.setParameter("b", params.get("BM25:b"))) {
    		System.err.println("incorrect BM25 parameter ");
        	System.exit(1);
    	}
    	if (!model.setParameter("k_3", params.get("BM25:k_3"))) {
    		System.err.println("incorrect BM25 paramester");
        	System.exit(1);
    	}
    }   	
    else if (params.get("retrievalAlgorithm").equals("Indri"))
    {
    	model = new RetrievalModelIndri();
     	if (!model.setParameter("mu", params.get("Indri:mu"))) {
     		System.err.println("incorrect Indri paramester");
        	System.exit(1);
     	}	
    	if (!model.setParameter("lambda", params.get("Indri:lambda"))) {
    		System.err.println("incorrect Indri paramester");
        	System.exit(1);
    	}
    }
    else {
    	System.err.println("incorrect model");
    	System.exit(1);
    }

    /*
     *  The code below is an unorganized set of examples that show
     *  you different ways of accessing the index.  Some of these
     *  are only useful in HW2 or HW3.
     */
  /*  System.out.println(s.toString());
    // Lookup the document length of the body field of doc 0.
    System.out.println(s.getDocLength("body", 1));

    // How to use the term vector.
    TermVector tv = new TermVector(1, "body");
    System.out.println(tv.stemString(2)); // get the string for the 100th stem
    System.out.println(tv.stemDf(2)); // get its df
    System.out.println(tv.totalStemFreq(2)); // get its ctf
    */
  
    /**
     *  The index is open. Start evaluating queries. The examples
     *  below show query trees for two simple queries.  These are
     *  meant to illustrate how query nodes are created and connected.
     *  However your software will not create queries like this.  Your
     *  software will use a query parser.  See parseQuery.
     *
     *  The general pattern is to tokenize the  query term (so that it
     *  gets converted to lowercase, stopped, stemmed, etc), create a
     *  Term node to fetch the inverted list, create a Score node to
     *  convert an inverted list to a score list, evaluate the query,
     *  and print results.
     * 
     *  Modify the software so that you read a query from a file,
     *  parse it, and form the query tree automatically.
     */
   
    String queryfile = params.get("queryFilePath");
    BufferedReader br = new BufferedReader(new FileReader(queryfile));
    HashMap<Integer,String> queryList = new  HashMap<Integer,String>();
    
    // Read query file and put it into hashmap.
    String qline = br.readLine();
    while (qline != null) {
    	String words [] = qline.split(":");
        queryList.put(Integer.parseInt(words[0]),words[1]);
        qline = br.readLine();
    }
    
    br.close();
    
    Qryop qTree;
    QryResult res;
	  
    // For all the queries in the hashmap, evaluate and write the result into trecEvalOutputPath file
    for(Integer qid : queryList.keySet())
    {
    	qTree = parseQuery(queryList.get(qid));
    	res = qTree.evaluate (model);
    	writeResults(qid, res, params.get("trecEvalOutputPath"));
    } 
  }

/**
   *  Write an error message and exit.  This can be done in other
   *  ways, but I wanted something that takes just one statement so
   *  that it is easy to insert checks without cluttering the code.
   *  @param message The error message to write before exiting.
   *  @return void
   */
  static void fatalError (String message) {
    System.err.println (message);
    System.exit(1);
  }

  /**
   *  Get the external document id for a document specified by an
   *  internal document id. If the internal id doesn't exists, returns null.
   *  
   * @param iid The internal document id of the document.
   * @throws IOException 
   */
  static String getExternalDocid (int iid) throws IOException {
    Document d = QryEval.READER.document (iid);
    String eid = d.get ("externalId");
    return eid;
  }

  /**
   *  Finds the internal document id for a document specified by its
   *  external id, e.g. clueweb09-enwp00-88-09710.  If no such
   *  document exists, it throws an exception. 
   * 
   * @param externalId The external document id of a document.s
   * @return An internal doc id suitable for finding document vectors etc.
   * @throws Exception
   */
  static int getInternalDocid (String externalId) throws Exception {
    Query q = new TermQuery(new Term("externalId", externalId));
    
    IndexSearcher searcher = new IndexSearcher(QryEval.READER);
    TopScoreDocCollector collector = TopScoreDocCollector.create(1,false);
    searcher.search(q, collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;
    
    if (hits.length < 1) {
      throw new Exception("External id not found.");
    } else {
      return hits[0].doc;
    }
  }

  /**
   * parseQuery converts a query string into a query tree.
   * 
   * @param qString
   *          A string containing a query.
   * @param qTree
   *          A query tree
   * @throws IOException
   */
  static Qryop parseQuery(String qString) throws IOException {

    Qryop currentOp = null;
    Stack<Qryop> stack = new Stack<Qryop>();

    // Add a default query operator to an unstructured query. This
    // is a tiny bit easier if unnecessary whitespace is removed.

    qString = qString.trim();

    if (qString.charAt(0) != '#') {
      qString = "#or(" + qString + ")";
    }

    // Tokenize the query.

    StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);
    String token = null;

    // Each pass of the loop processes one token. To improve
    // efficiency and clarity, the query operator on the top of the
    // stack is also stored in currentOp.

    while (tokens.hasMoreTokens()) {

      token = tokens.nextToken();

      if (token.matches("[ ,(\t\n\r]")) {
        // Ignore most delimiters.
      } else if (token.equalsIgnoreCase("#and")) {
        currentOp = new QryopSlAnd();
        stack.push(currentOp);
      } else if (token.equalsIgnoreCase("#or")) {
          currentOp = new QryopSlOr();
          stack.push(currentOp);
      } else if (token.equalsIgnoreCase("#syn")) {
        currentOp = new QryopIlSyn();
        stack.push(currentOp);
      } else if (token.toLowerCase().matches("#near/\\d*")) {  
    	  String num[] = token.split("/");
    	  currentOp = new QryopIlNear(Integer.parseInt(num[1])); // pass the number as arg in near
          stack.push(currentOp);
      } else if (token.startsWith(")")) { // Finish current query operator.
        // If the current query operator is not an argument to
        // another query operator (i.e., the stack is empty when it
        // is removed), we're done (assuming correct syntax - see
        // below). Otherwise, add the current operator as an
        // argument to the higher-level operator, and shift
        // processing back to the higher-level operator.

        stack.pop();

        if (stack.empty())
          break;

        Qryop arg = currentOp;
        currentOp = stack.peek();
        currentOp.add(arg);
      } else {

        // NOTE: You should do lexical processing of the token before
        // creating the query term, and you should check to see whether
        // the token specifies a particular field (e.g., apple.title).
    	 
    	  
    	  // pass the query to tokenizeQuery before adding it to currentOp.
    	 
    	  String [] vals = tokenizeQuery(token);
    	  
    	  if ( vals.length != 0)
    	  {
    		  String [] field = vals[0].split("\\.");  // split on . to add field term if present
    		  if (field.length > 1)
    			  currentOp.add(new QryopIlTerm(field[0], field[1]));
    		  else
    			  currentOp.add(new QryopIlTerm(field[0]));
    	  }
    		  
      }
    }

    // A broken structured query can leave unprocessed tokens on the
    // stack, so check for that.

    if (tokens.hasMoreTokens()) {
      System.err.println("Error:  Query syntax is incorrect.  " + qString);
      return null;
    }

    return currentOp;
  }

  /**
   *  Print a message indicating the amount of memory used.  The
   *  caller can indicate whether garbage collection should be
   *  performed, which slows the program but reduces memory usage.
   *  @param gc If true, run the garbage collector before reporting.
   *  @return void
   */
  public static void printMemoryUsage (boolean gc) {

    Runtime runtime = Runtime.getRuntime();

    if (gc) {
      runtime.gc();
    }

    System.out.println ("Memory used:  " +
			((runtime.totalMemory() - runtime.freeMemory()) /
			 (1024L * 1024L)) + " MB");
  }
  
  /**
   *  Given a QryResult, convert the invertedList to a scoreList for 
   *  ranking and writing results.
   * 
   *  @param result QryResult 
   *  @return void
   */
  static void convertToScorelist(QryResult result) {
	// TODO Auto-generated method stub
	  for (int i = 0; i < result.invertedList.df; i++) {

	      result.docScores.add(result.invertedList.postings.get(i).docid,
				   (float) result.invertedList.postings.get(i).tf);
	    }

}

  /**
   *  This function writes the result into a file named filename.
   *  The output format is specified as - 
   *  QueryID	Q0	DocID	Rank	Score	RunID
   *  The function will also convert the the invertedList into a scoreList using the
   *  convertToScorelist() method.
   *  It will also sort the result according to descending score.
   *  @param result QryResult 
   *  @return void
   */
  static void writeResults(int queryId, QryResult result, String filename) throws IOException {
	  BufferedWriter writer = null;
	
	  try {
	    writer = new BufferedWriter(new FileWriter(filename, true));   // write in file
	    if (result.invertedList.df > 0)
	    {
	    	convertToScorelist(result);
	    }
	    
	    result.docScores.sort_array();   // do sorting
	    
	    if (result.docScores.scores.size() < 1) {
	    	writer.write(queryId+" Q0 dummy 1 0 run-1\n");  // if no results found
	      } else {
	    for (int i = 0; i < result.docScores.scores.size() && i < 100; i++) {
	    	 writer.write(queryId+" Q0 " + getExternalDocid (result.docScores.getDocid(i))
	    			  + " "+(i+1)+" " + result.docScores.getDocidScore(i) +" run-1\n");
	    }
	    }
	   } catch (Exception e) {
	    e.printStackTrace();
	  } finally {
	    try {
	      writer.close();
	    } catch (Exception e) {
	    }
	  }
  }

  /**
   *  Given a query string, returns the terms one at a time with stopwords
   *  removed and the terms stemmed using the Krovetz stemmer. 
   * 
   *  Use this method to process raw query terms. 
   * 
   *  @param query String containing query
   *  @return Array of query tokens
   *  @throws IOException
   */
  static String[] tokenizeQuery(String query) throws IOException {

    TokenStreamComponents comp = analyzer.createComponents("dummy", new StringReader(query));
    TokenStream tokenStream = comp.getTokenStream();

    CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
    tokenStream.reset();

    List<String> tokens = new ArrayList<String>();
    while (tokenStream.incrementToken()) {
      String term = charTermAttribute.toString();
      tokens.add(term);
    }
    return tokens.toArray(new String[tokens.size()]);
  }
}
