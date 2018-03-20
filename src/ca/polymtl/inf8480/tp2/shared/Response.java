package ca.polymtl.inf8480.tp2.shared;

public class Response implements java.io.Serializable {
	public String message = null;
	public int[] results;

	public Response(){
		message = "";
		results = null;
	}
}
