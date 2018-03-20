package ca.polymtl.inf8480.tp2.dispatcher;

import java.util.Arrays;

public class Util {
    public static String[][] splitChunks(String[] operations, int chunkLength){
		int nbChunks = operations.length / chunkLength;
		if( operations.length % chunkLength != 0) {
			nbChunks += 1;
		}
		String[][] chunks = new String[nbChunks][];
		for(int i = 0; i < nbChunks ; ++i) {
			chunks[i] = Arrays.copyOfRange(operations, i * chunkLength, Math.min((i+1)*chunkLength, operations.length));
		}
		return chunks;
    }

	public static int[] combineResults(int[][] resultParts) {
		int nbParts = resultParts.length;
		int nbResults = 0;
	
		// Need to know how many results we have to allocate memory for
		// our concatenated array.
		for(int i = 0; i < nbParts ; ++i) {
			nbResults += resultParts[i].length;
		}
	
		int[] results = new int[nbResults];
		int offset = 0;
		for(int i = 0; i < nbParts ; ++i) {
			nbResults += resultParts[i].length;
			System.arraycopy(resultParts[i], 0, results, offset, resultParts[i].length);
			offset += resultParts[i].length;
		}
	
		return results;
	}
}
