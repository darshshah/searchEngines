
public class RetrievalModelIndri extends RetrievalModel {

	/**
	   * Set a retrieval model parameter.
	   * @param parameterName
	   * @param parametervalue
	   * @return True because this retrieval model has parameters.
	   */
	
	public boolean setParameter(String parameterName, double value) {
		 System.err.println ("Error: Unknown parameter name for retrieval model " +
					"Indri: " + parameterName);
		return false;
	}

	 /**
	   * Set a retrieval model parameter.
	   * @param parameterName
	   * @param parametervalue
	   * @return True because this retrieval model has parameters.
	   */
	
	public boolean setParameter(String parameterName, String value) {
		
		 if (parameterName.equals("mu")) {
			 mu = Double.parseDouble(value);
		 } else if (parameterName.equals("lambda")) {
			 lambda = Double.parseDouble(value);
		 } else {
			return false;
		 }
		 
		return true;
	}

}