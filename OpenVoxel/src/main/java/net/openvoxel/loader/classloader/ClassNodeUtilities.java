package net.openvoxel.loader.classloader;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Created by James on 09/04/2017.
 *
 * Class Node Modification Utilities
 */
public abstract class ClassNodeUtilities {

	public static void MakeMethodUseless(MethodNode node) {
		Type return_type = Type.getReturnType(node.desc);
		node.instructions.clear();
		if(return_type == Type.VOID_TYPE) {
			node.instructions.add(new InsnNode(Opcodes.RETURN));
		}else if(return_type == Type.BOOLEAN_TYPE) {
			node.instructions.add(new InsnNode(Opcodes.ICONST_0));
			node.instructions.add(new InsnNode(Opcodes.IRETURN));
		}else if(return_type == Type.BYTE_TYPE) {
			node.instructions.add(new InsnNode(Opcodes.ICONST_0));
			node.instructions.add(new InsnNode(Opcodes.IRETURN));
		}else if(return_type == Type.CHAR_TYPE) {
			node.instructions.add(new InsnNode(Opcodes.ICONST_0));
			node.instructions.add(new InsnNode(Opcodes.IRETURN));
		}else if(return_type == Type.SHORT_TYPE) {
			node.instructions.add(new InsnNode(Opcodes.ICONST_0));
			node.instructions.add(new InsnNode(Opcodes.IRETURN));
		}else if(return_type == Type.INT_TYPE) {
			node.instructions.add(new InsnNode(Opcodes.ICONST_0));
			node.instructions.add(new InsnNode(Opcodes.IRETURN));
		}else if(return_type == Type.LONG_TYPE) {
			node.instructions.add(new InsnNode(Opcodes.LCONST_0));
			node.instructions.add(new InsnNode(Opcodes.LRETURN));
		}else if(return_type == Type.FLOAT_TYPE) {
			node.instructions.add(new InsnNode(Opcodes.FCONST_0));
			node.instructions.add(new InsnNode(Opcodes.FRETURN));
		}else if(return_type == Type.DOUBLE_TYPE) {
			node.instructions.add(new InsnNode(Opcodes.DCONST_0));
			node.instructions.add(new InsnNode(Opcodes.DRETURN));
		}else {
			node.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
			node.instructions.add(new InsnNode(Opcodes.ARETURN));
		}
	}
}
