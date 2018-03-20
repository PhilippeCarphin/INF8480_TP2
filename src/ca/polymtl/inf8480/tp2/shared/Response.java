package ca.polymtl.inf8480.tp2.shared;

/**
 * General purpose class for communication between parts of 
 * the system
 * @author pcarphin
 *
 */
public class Response implements java.io.Serializable {
	public enum Code {
		NO_ERROR, AUTH_FAILURE, CAPACITY_FAILURE, OTHER_FAILURE
	}
	
	public String message = null;
	public int[] results;
	public Code code;


	public Response(){
		code = Code.NO_ERROR;
		message = "";
		results = null;
	}
}
