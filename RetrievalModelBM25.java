
public class RetrievalModelBM25 extends RetrievalModel {
	
	  double k_1 = 0.0, b = 0.0, k_3 = 0.0;
	  

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
		 if (parameterName.equals("BM:k_1")) {
			 k_1 = Double.parseDouble(value);
		 } else if (parameterName.equals("BM:b")) {
			 b = Double.parseDouble(value);
		 } else if (parameterName.equals("BM:k_3")) {
			 k_3 = Double.parseDouble(value);
		 } else {
			 return false;
		 }
		 
		 return true;
	}

}
