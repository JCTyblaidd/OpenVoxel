package net.openvoxel.client.renderer.common;

public interface GraphicsAPI {

	default void acquireNextFrame() {}

	default void submitNextFrame() {}

	default void close() {}

}
