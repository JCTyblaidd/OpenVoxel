package net.openvoxel.client.renderer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class WorldCullManagerTest {


	@Test
	void runFrustumCull() {
		WorldDrawTask _drawTask = new WorldDrawTask(new TestGraphicsAPI(),0);
		WorldCullManager _manager = new WorldCullManager(_drawTask);
		assertDoesNotThrow(() -> {
			_manager.runFrustumCull(Assertions::assertNotNull);
		});
	}
}