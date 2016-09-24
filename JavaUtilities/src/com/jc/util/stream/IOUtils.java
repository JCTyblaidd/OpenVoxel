package com.jc.util.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by James on 23/09/2016.
 */
public class IOUtils {

	public static void copyStream(InputStream input, OutputStream output,int batchSize) throws IOException {
		byte[] buffer = new byte[batchSize];
		int readAmount;
		while ((readAmount = input.read(buffer)) != -1) {
			output.write(buffer, 0, readAmount);
		}
	}
}
