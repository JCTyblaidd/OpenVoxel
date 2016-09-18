package net.openvoxel.loader.classloader;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.api.side.Side;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 04/08/2016.
 */
public class SideOnlyConditionalTweaker extends ClassNodeTweaker{

	private boolean client;

	public SideOnlyConditionalTweaker(boolean isClient) {
		client = isClient;
	}

	@Override
	public void modify(ClassNode node) {
		for(MethodNode methodNode : (List<MethodNode>)node.methods) {
			//_optimizeInstructionList(methodNode.instructions);
		}
	}

	private void _optimizeInstructionList(InsnList list) {
		//First Scan for GET_STATIC net/openvoxel/util/Side.isClient : Z
		List<FieldInsnNode> nodes = new ArrayList<>();
		for(int i = 0; i < list.size(); i++) {
			AbstractInsnNode node = list.get(i);
			if(node instanceof FieldInsnNode) {
				FieldInsnNode _node = (FieldInsnNode) node;
				//_node.desc = "Z"
				//_node.name
				//_node.owner
				Logger.INSTANCE.Info("Found GETSTATIC");
				Logger.INSTANCE.Info(_node.desc);
				Logger.INSTANCE.Info(_node.name);
				Logger.INSTANCE.Info(_node.owner);
			}
		}
	}

	//
	public void fun() {
		if(Side.isClient) {
			//Section A//
			modify(null);
		}else {
			modify(null);
			modify(null);
			//Section B//
		}
		modify(null);
		if(!Side.isClient) {
			modify(null);
		}
	}


}
