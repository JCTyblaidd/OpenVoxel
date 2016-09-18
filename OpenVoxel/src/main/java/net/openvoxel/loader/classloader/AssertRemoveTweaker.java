package net.openvoxel.loader.classloader;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

/**
 * Created by James on 08/09/2016.
 *
 * Remove assert(X) code sections
 */
public class AssertRemoveTweaker extends ClassNodeTweaker{
	@Override
	public void modify(ClassNode node) {
		((List<MethodNode>) node.methods).forEach(this::removeAsserts);
	}

	private void removeAsserts(MethodNode node) {

	}
}
