package net.openvoxel.api.logger;

import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by James on 02/08/2016.
 *
 * Smart OutputStream that relays information to a logger
 */
public class LoggerOutputStream extends OutputStream{

	private Logger log;
	private ByteBuffer currentLine;
	private String _trim;

	LoggerOutputStream(Logger logger) {
		log = logger;
		currentLine = ByteBuffer.allocate(2048);
	}

	LoggerOutputStream setTrim(String trim) {
		_trim = trim;
		return this;
	}

	@Override
	public void write(int b) {
		currentLine.put((byte)b);
		_pushIfRequired();
	}

	private void _pushIfRequired() {
		int l = currentLine.position();
		currentLine.position(0);
		byte[] arr = new byte[l];
		currentLine.get(arr);
		if(currentLine.position() != l) {
			log.Info("Err:" + currentLine.position() + " >> " + l);
		}
		String str = new String(arr);
		if(str.charAt(str.length() - 1) == '\n') {
			//Reset
			String write = str.substring(0,str.length()-1);
			if(write.startsWith(_trim)) {
				write = write.substring(_trim.length());
			}
			_log(write);
			currentLine.position(0);
		}
	}

	private void _log(String str) {
		log.Debug(str);
	}

}
