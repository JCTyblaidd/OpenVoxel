package net.openvoxel.client.renderer.generic;

/**
 * Created by James on 25/08/2016.
 *
 * Reference to the display device (window)
 */
public interface DisplayHandle {

	void pollEvents();

	int getRefreshRate();

	void setRefreshRate(int hz);
}
