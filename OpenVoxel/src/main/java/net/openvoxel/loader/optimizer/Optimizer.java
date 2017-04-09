package net.openvoxel.loader.optimizer;

import com.jc.util.utils.ArgumentParser;
import net.openvoxel.loader.classloader.ClassNodeTweaker;
import net.openvoxel.loader.classloader.ClassNodeUtilities;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

/**
 * Created by James on 09/04/2017.
 *
 * Run Optimization Transform Passes on the code as it is loading
 */
public class Optimizer extends ClassNodeTweaker{

	private boolean configRemoveValidation;

	public Optimizer(ArgumentParser parser) {
		configRemoveValidation = parser.hasFlag("noValidate");
	}

	@Override
	public void modify(ClassNode node) {
		if(configRemoveValidation) {
			passRemoveValidation(node);
		}
	}

	@SuppressWarnings("unchecked")
	private void passRemoveValidation(ClassNode node) {
		methods:
		for(MethodNode methodNode : (List<MethodNode>)node.methods) {
			for(AnnotationNode annotation : (List<AnnotationNode>)node.visibleAnnotations) {
				if("Lnet.openvoxel.loader.optimizer.tags.Validation;".equals(annotation.desc)) {
					ClassNodeUtilities.MakeMethodUseless(methodNode);
					continue methods;
				}
			}
		}
	}
}
