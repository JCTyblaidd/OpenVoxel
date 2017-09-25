package net.openvoxel.loader.classloader;

import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by James on 31/07/2016.
 *
 * Applies the side specific tweaks to functions, include code removal of entire function removal
 */
@SuppressWarnings("SpellCheckingInspection")
public class SideSpecificTweaker extends ClassNodeTweaker {

	private boolean ClientSide;

	public SideSpecificTweaker(boolean Client) {
		ClientSide = Client;
	}


	@SuppressWarnings("unchecked")
	public void modify(ClassNode node) {
		//Entire Class//
		if(shouldClassShell(node)) {
			node.fields.clear();
			for(MethodNode methodNode : (List<MethodNode>)node.methods) {
				ClassNodeUtilities.MakeMethodUseless(methodNode);
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
		for(MethodNode methodNode : (List<MethodNode>)node.methods) {
			ClassNodeUtilities.RemoveFieldSections(methodNode.instructions,"net/openvoxel/api/side/Side","isClient",ClientSide);
		}
	}

	@SuppressWarnings("unchecked")
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
	@SuppressWarnings("unchecked")
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
							ClassNodeUtilities.MakeMethodUseless(node);
							return false;
						}else {
							return true;
						}
					}
				}else {
					if(ClientSide) {
						if(operation == SideOnly.SideOperation.REMOVE_CODE) {
							ClassNodeUtilities.MakeMethodUseless(node);
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
	@SuppressWarnings("unchecked")
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


}
