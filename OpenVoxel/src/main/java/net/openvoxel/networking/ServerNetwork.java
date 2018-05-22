package net.openvoxel.networking;

import java.io.Closeable;
import java.io.IOException;

public interface ServerNetwork extends Closeable {

	@Override
	void close() throws IOException;

	boolean isLocal();

}
