package net.openvoxel.client.audio;

import net.openvoxel.common.resources.ResourceHandle;

/**
 * Created by James on 10/09/2016.
 *
 *
 */
public class AudioHandle {

	public static AudioHandle from(ResourceHandle handle) {
		return null;
	}

	private AudioHandle(byte[] Data) {
		//STBVorbis.stb_vorbis_decode_memory()
	}

	/**
	 * Start Playback of an instance
	 */
	public void Play() {

	}

	/**
	 * Start Playback of an instance, stop any previous instances
	 */
	public void PlayNoRepeat() {
		StopAll();
		Play();
	}

	/**
	 * Stop All Current Playbacks
	 */
	public void StopAll() {

	}
}
