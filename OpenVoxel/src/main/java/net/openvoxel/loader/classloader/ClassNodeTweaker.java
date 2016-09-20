package net.openvoxel.loader.classloader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

/**
 * Created by James on 01/08/2016.
 *
 * Generic ClassLoader Modifier that uses the ASM ClassNode
 */
public abstract class ClassNodeTweaker implements TweakableClassLoader.IASMTransformer{

	@Override
	public byte[] transform(byte[] values, String classID) {
		ClassReader reader = new ClassReader(values);
		ClassNode classNode =new ClassNode();
		reader.accept(classNode, 0);
		modify(classNode);
		ClassWriter writer =new ClassWriter(ClassWriter.COMPUTE_MAXS| ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	public abstract void modify(ClassNode node);
}
