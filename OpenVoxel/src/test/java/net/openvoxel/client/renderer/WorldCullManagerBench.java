package net.openvoxel.client.renderer;

import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.utility.FrustumTest;
import net.openvoxel.world.client.ClientWorld;
import net.openvoxel.world.generation.DebugWorldGenerator;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WorldCullManagerBench {

	@Test
	void frustumIntersectBenchmark() {
		Options opt = new OptionsBuilder()
				.include(this.getClass().getName()+".*")
				.mode(Mode.AverageTime)
				.warmupTime(TimeValue.seconds(1))
				.warmupIterations(2)
				.measurementTime(TimeValue.seconds(1))
				.measurementIterations(5)
				.threads(2)
				.forks(1)
				.shouldFailOnError(true)
				.shouldDoGC(true)
				.build();
		assertNotNull(opt);
		assertDoesNotThrow(() -> {
			new Runner(opt).run();
		});
	}

	@State (Scope.Thread)
	public static class FrustumState {
		WorldDrawTask drawTask;
		WorldCullManager cullManager;
		FrustumTest intersection;

		@Setup(Level.Trial)
		public void init() {
			drawTask = new WorldDrawTask(new TestGraphicsAPI(),0);
			cullManager = new WorldCullManager(drawTask);
			drawTask.theWorld = new ClientWorld(new DebugWorldGenerator());
			drawTask.thePlayer = new EntityPlayerSP();
			drawTask.thePlayer.currentWorld = drawTask.theWorld;
			intersection = new FrustumTest();
		}

		@Setup(Level.Iteration)
		public void random() {
			Random random = new Random();
			Matrix4f mat = new Matrix4f()
               .perspective((float)Math.toRadians(90),1,0.1F,100,true)
					.rotateX(random.nextFloat()).rotateY(random.nextFloat()).rotateZ(random.nextFloat())
					.translate(random.nextFloat(),random.nextFloat(),random.nextFloat());
			intersection.set(mat);
		}

	}

	//@Benchmark
	public void testDefaultFrustum(FrustumState state,Blackhole bh) {
		//WorldCullManager culler = state.cullManager;
		//culler.runFrustumCull(bh::consume);
	}

	@Benchmark
	public void testFrustumIntersectBase(FrustumState state,Blackhole bh) {
		for(int i = -2500; i < 2500; i++) {
			int _x = i * 4;
			int _y = i * 3;
			int _z = i * 2;
			bh.consume(state.intersection.testAabBase(
					_x,
					_y,
					_z,
					_x+16,
					_y+16,
					_z+16
			));
		}
	}

	@Benchmark
	public void testFrustumIntersectSphere(FrustumState state,Blackhole bh) {
		for(int i = -2500; i < 2500; i++) {
			int _x = i * 4;
			int _y = i * 3;
			int _z = i * 2;
			bh.consume(state.intersection.testSphereBase(
					_x + 8,
					_y + 8,
					_z + 8,
					8
			));
		}
	}

	@Benchmark
	public void testFrustumIntersectAabbNoBranch(FrustumState state,Blackhole bh) {
		for(int i = -2500; i < 2500; i++) {
			int _x = i * 4;
			int _y = i * 3;
			int _z = i * 2;
			bh.consume(state.intersection.testAabNoBranch(
					_x,
					_y,
					_z,
					_x+16,
					_y+16,
					_z+16
			));
		}
	}

	@Benchmark
	public void testFrustumIntersectSphereNoBranch(FrustumState state,Blackhole bh) {
		for(int i = -2500; i < 2500; i++) {
			int _x = i * 4;
			int _y = i * 3;
			int _z = i * 2;
			bh.consume(state.intersection.testSphereNoBranch(
					_x + 8,
					_y + 8,
					_z + 8,
					8
			));
		}
	}

}
