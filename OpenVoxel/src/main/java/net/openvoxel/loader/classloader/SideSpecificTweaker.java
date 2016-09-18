package net.openvoxel.loader.classloader;

import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by James on 31/07/2016.
 *
 * Applies the side specific tweaks to functions
 */
public class SideSpecificTweaker extends ClassNodeTweaker {

	public boolean ClientSide;

	public SideSpecificTweaker(boolean Client) {
		ClientSide = Client;
	}


	@SuppressWarnings("all")
	public void modify(ClassNode node) {
		//Entire Class//
		if(shouldClassShell(node)) {
			node.fields.clear();
			for(MethodNode methodNode : (List<MethodNode>)node.methods) {
				makeMethodUseless(methodNode);
			}
			return;
		}
		//Methods//
		List<MethodNode> removeMethods = new ArrayList<>();
		for(MethodNode methodNode : (List<MethodNode>)node.methods) {
			boolean flag = modifyMethod(methodNode);
			if(flag) {
				removeMethods.add(methodNode);
			}
		}
		node.methods.removeAll(removeMethods);
		//Fields//
		List<FieldNode> removedFields = new ArrayList<>();
		for(FieldNode fieldNode : (List<FieldNode>)node.fields) {
			boolean flag = modifyField(fieldNode);
			if(flag) {
				removedFields.add(fieldNode);
			}
		}
		node.fields.remove(removedFields);
	}

	private boolean shouldClassShell(ClassNode node) {
		if(node.visibleAnnotations == null) return false;
		for(AnnotationNode annotation : (List<AnnotationNode>)node.visibleAnnotations) {
			if("Lnet/openvoxel/api/side/SideOnly;".equals(annotation.desc)) {
				//Contains @SideOnly!!!
				if(!"side".equals(annotation.values.get(0))) throw new RuntimeException("Invalid @SideOnly!");
				String[] val_side = (String[])annotation.values.get(1);
				String[] val_op;
				if(annotation.values.size() >= 4) {
					if (!"operation".equals(annotation.values.get(2))) throw new RuntimeException("Invalid @SideOnly!");
					val_op = (String[]) annotation.values.get(3);
				}else {
					val_op = new String[]{"Lnet/openvoxel/api/side/SideOnly$SideOperation;","REMOVE_STRUCTURE"};
				}
				///Parse Types//
				if(!"Lnet/openvoxel/api/side/Side;".equals(val_side[0])) throw new RuntimeException("Invalid @SideOnly!");
				if(!"Lnet/openvoxel/api/side/SideOnly$SideOperation;".equals(val_op[0])) throw new RuntimeException("Invalid @SideOnly!");
				Side side = Side.valueOf(val_side[1]);
				SideOnly.SideOperation operation = SideOnly.SideOperation.valueOf(val_op[1]);
				if(operation != SideOnly.SideOperation.REMOVE_STRUCTURE) throw new RuntimeException("Invalid @SideOnly on Class, Remove_Code not Valid!");
				if(side == Side.CLIENT) {
					if(!ClientSide) {
						return true;
					}
				}else {
					if(ClientSide) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**@return should remove**/
	@SuppressWarnings("all")
	private boolean modifyMethod(MethodNode node) {
		if(node.visibleAnnotations == null) return false;
		for(AnnotationNode annotation : (List<AnnotationNode>)node.visibleAnnotations) {
			if("Lnet/openvoxel/api/side/SideOnly;".equals(annotation.desc)) {
				//Contains @SideOnly!!!
				if(!"side".equals(annotation.values.get(0))) throw new RuntimeException("Invalid @SideOnly!");
				String[] val_side = (String[])annotation.values.get(1);
				String[] val_op;
				if(annotation.values.size() >= 4) {
					if (!"operation".equals(annotation.values.get(2))) throw new RuntimeException("Invalid @SideOnly!");
					val_op = (String[]) annotation.values.get(3);
				}else {
					val_op = new String[]{"Lnet/openvoxel/api/side/SideOnly$SideOperation;","REMOVE_STRUCTURE"};
				}
				///Parse Types//
				if(!"Lnet/openvoxel/api/side/Side;".equals(val_side[0])) throw new RuntimeException("Invalid @SideOnly!");
				if(!"Lnet/openvoxel/api/side/SideOnly$SideOperation;".equals(val_op[0])) throw new RuntimeException("Invalid @SideOnly!");
				Side side = Side.valueOf(val_side[1]);
				SideOnly.SideOperation operation = SideOnly.SideOperation.valueOf(val_op[1]);
				if(side == Side.CLIENT) {
					if(!ClientSide) {
						if(operation == SideOnly.SideOperation.REMOVE_CODE) {
							makeMethodUseless(node);
							return false;
						}else {
							return true;
						}
					}
				}else {
					if(ClientSide) {
						if(operation == SideOnly.SideOperation.REMOVE_CODE) {
							makeMethodUseless(node);
							return false;
						}else {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**@return should remove**/
	@SuppressWarnings("all")
	private boolean modifyField(FieldNode node) {
		if(node.visibleAnnotations == null) return false;
		for(AnnotationNode annotation : (List<AnnotationNode>)node.visibleAnnotations) {
			if("Lnet/openvoxel/api/side/SideOnly;".equals(annotation.desc)) {
				//Contains @SideOnly!!!
				if(!"side".equals(annotation.values.get(0))) throw new RuntimeException("Invalid @SideOnly!");
				String[] val_side = (String[])annotation.values.get(1);
				String[] val_op;
				if(annotation.values.size() >= 4) {
					if (!"operation".equals(annotation.values.get(2))) throw new RuntimeException("Invalid @SideOnly!");
					val_op = (String[]) annotation.values.get(3);
				}else {
					val_op = new String[]{"Lnet/openvoxel/api/side/SideOnly$SideOperation;","REMOVE_STRUCTURE"};
				}
				///Parse Types//
				if(!"Lnet/openvoxel/api/side/Side;".equals(val_side[0])) throw new RuntimeException("Invalid @SideOnly!");
				if(!"Lnet/openvoxel/api/side/SideOnly$SideOperation;".equals(val_op[0])) throw new RuntimeException("Invalid @SideOnly!");
				Side side = Side.valueOf(val_side[1]);
				SideOnly.SideOperation operation = SideOnly.SideOperation.valueOf(val_op[1]);
				if(operation != SideOnly.SideOperation.REMOVE_STRUCTURE) throw new RuntimeException("Invalid @SideOnly on Field, Remove_Code not Valid!");
				if(side == Side.CLIENT) {
					if(!ClientSide) {
						return true;
					}
				}else {
					if(ClientSide) {
						return true;
					}
				}
			}
		}
		return false;
	}


	private void makeMethodUseless(MethodNode node) {
		Type return_type = Type.getReturnType(node.desc);
		node.instructions.clear();
		if(return_type == Type.VOID_TYPE) {
			node.instructions.add(new InsnNode(Opcodes.RETURN));
			return;
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
			return;
		}
	}
}
