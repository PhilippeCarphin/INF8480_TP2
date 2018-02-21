package ca.polymtl.inf8480.tp1.shared;

public class Response implements java.io.Serializable {
	public boolean retval;
	public String message = null;
	public int clientID;

	public Response(){
		retval = false;
		message = "";
		clientID = -1;
	}
}
