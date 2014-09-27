import java.io.IOException;

import org.apache.lucene.index.IndexReader;


public class RetrievalModelBM25 extends RetrievalModel {
	
	 
	 /**
	   * Set a retrieval model parameter.
	   * @param parameterName
	   * @param parametervalue
	   * @return Always true because this retrieval model has parameters.
	   */
	
	public boolean setParameter(String parameterName, double value) {
		 System.err.println ("Error: Unknown parameter name for retrieval model " +
					"BM25: " + parameterName);		
		 return true;
	}

	 /**
	   * Set a retrieval model parameter.
	   * @param parameterName
	   * @param parametervalue
	   * @return Always true because this retrieval model has parameters.
	   */
	
	public boolean setParameter(String parameterName, String value) {
		 if (parameterName.equals("k_1")) {
			 k_1 = Double.parseDouble(value);
		 } else if (parameterName.equals("b")) {
			 b = Double.parseDouble(value);
		 } else if (parameterName.equals("k_3")) {
			 k_3 = Double.parseDouble(value);
		 } else {
			 return false;
		 }
		 
		 return true;
	}
}
