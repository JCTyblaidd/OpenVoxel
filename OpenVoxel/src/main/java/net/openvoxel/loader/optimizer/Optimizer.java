package net.openvoxel.loader.optimizer;

import com.jc.util.utils.ArgumentParser;
import net.openvoxel.loader.classloader.ClassNodeTweaker;
import org.objectweb.asm.tree.ClassNode;

/**
 * Created by James on 09/04/2017.
 *
 * Run Optimization Transform Passes on the code as it is loading
 */
public class Optimizer extends ClassNodeTweaker{

	public Optimizer(ArgumentParser parser) {

	}


	@Override
	public void modify(ClassNode node) {

	}
}
