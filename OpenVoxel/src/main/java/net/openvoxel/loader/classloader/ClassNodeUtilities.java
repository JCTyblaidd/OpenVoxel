package net.openvoxel.loader.classloader;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * Created by James on 09/04/2017.
 *
 * Class Node Modification Utilities
 */
public abstract class ClassNodeUtilities {

	public static void RemoveFieldSections(InsnList instructions,String clazz,String field,boolean value) {
		int idx = 0;
		while(idx < instructions.size()) {
			AbstractInsnNode node = instructions.get(idx);
			boolean modify = false;
			if(node instanceof FieldInsnNode) {
				FieldInsnNode fNode = (FieldInsnNode) node;
				if(fNode.name.equals(field) && fNode.owner.equals(clazz) && fNode.getOpcode() == GETSTATIC) {
					modify = performRemoval(instructions,idx,value);
				}
			}
			if(!modify) idx++;
		}
	}

	private static boolean performRemoval(InsnList instructions,int index,boolean value) {
		AbstractInsnNode nextNode = instructions.get(index+1);
		if(nextNode instanceof JumpInsnNode) {
			JumpInsnNode jumpNode = (JumpInsnNode) nextNode;
			if(jumpNode.getOpcode() == IFEQ || jumpNode.getOpcode() == IFNE) {
				boolean jumpFlag = jumpNode.getOpcode() == IFEQ;
				if(jumpFlag == value) {
					//Remove Code//
					//System.out.println("INFO: CODE IF REMOVAL - ");
					//FIXME: correct the removal code
					//while(instructions.get(index) != jumpNode.label) {
					//	instructions.remove(instructions.get(index));
					//}
				}
			}
		}else if(nextNode instanceof InsnNode) {
			if(nextNode.getOpcode() == ICONST_0 || nextNode.getOpcode() == ICONST_1) {
				boolean constFlag = nextNode.getOpcode() == ICONST_1;
				AbstractInsnNode finalNode = instructions.get(index + 2);
				if (finalNode instanceof JumpInsnNode) {
					JumpInsnNode jumpNode = (JumpInsnNode) finalNode;
					if(jumpNode.getOpcode() == IF_ICMPNE || jumpNode.getOpcode() == IF_ICMPEQ) {
						boolean jumpFlag = jumpNode.getOpcode() == IF_ICMPEQ;
						if(constFlag) jumpFlag = !jumpFlag;
						if(jumpFlag == value) {
							//Remove Code//
							/*
							System.out.println("INFO: CODE IF REMOVAL COMPLICATED");
							while(instructions.get(index) != jumpNode.label) {
								instructions.remove(instructions.get(index));
							}
						    */
						}
					}
				}
			}
		}
		return false;
	}

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
