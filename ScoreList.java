/**
 *  This class implements the document score list data structure
 *  and provides methods for accessing and manipulating them.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;
import java.util.*;

public class ScoreList {

  //  A little utilty class to create a <docid, score> object.

  protected class ScoreListEntry implements Comparable<ScoreListEntry> {
    private int docid;
    private double score;

    private ScoreListEntry(int docid, double score) {
      this.docid = docid;
      this.score = score;
    }

    /**
     *  CompareTo method for sorting.
     *  Sort descending according to scores and ascending by name to break ties
     *  @param ScoreListEntry scorelist entry.
     *  @return int
     */
	@Override
	public int compareTo(ScoreListEntry sle) {
		// TODO Auto-generated method stub
		try {
		if (this.score < sle.score)
			return 1;
		else if (this.score > sle.score)
			return -1;
		else
			return QryEval.getExternalDocid(this.docid).compareTo(QryEval.getExternalDocid(sle.docid));
		} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return -2;
			}
	}
  }

  List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();

  /**
   *  Append a document score to a score list.
   *  @param docid An internal document id.
   *  @param score The document's score.
   *  @return void
   */
  public void add(int docid, double score) {
    scores.add(new ScoreListEntry(docid, score));
  }

  /**
   *  Get the n'th document id.
   *  @param n The index of the requested document.
   *  @return The internal document id.
   */
  public int getDocid(int n) {
    return this.scores.get(n).docid;
  }

  /**
   *  Get the score of the n'th document.
   *  @param n The index of the requested document score.
   *  @return The document's score.
   */
  public double getDocidScore(int n) {
    return this.scores.get(n).score;
  }

  /**
   *  Sort the scores array
   *  @param void.
   *  @return void.
   */
  public void sort_array() {
	  Collections.sort(this.scores);
  }
}
