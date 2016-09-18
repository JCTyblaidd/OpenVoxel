package net.openvoxel.loader.mods;

import net.openvoxel.loader.classloader.TweakableClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by James on 25/08/2016.
 */
public class ModDataHandle implements TweakableClassLoader.IBytecodeSource{

	private File f;
	private List<String> asm;
	private List<String> mod;

	public ModDataHandle(File modV) {
		f = modV;
		asm = new ArrayList<>();
		mod = new ArrayList<>();
	}

	public void ScanModFile() {
		try {
			JarFile jarFile = new JarFile(f);
			Enumeration<JarEntry> entries = jarFile.entries();
			while(entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				InputStream in = jarFile.getInputStream(entry);
				byte[] data = new byte[(int)entry.getSize()];
				in.read(data);
				in.close();
				ClassReader reader = new ClassReader(data);
				ClassNode classNode =new ClassNode();
				reader.accept(classNode, 0);
				//ClassNode Loaded - Scan//
				if(classNode.visibleAnnotations.size() != 0) {
					for(AnnotationNode annotation : (List<AnnotationNode>)classNode.visibleAnnotations) {
						if("Lnet/openvoxel/api/mods/Mod;".equals(annotation.desc)) {
							//@Mod Class//
							mod.add(entry.getName());
						}else if("Lnet/openvoxel/api/mods/ASMHandler;".equals(annotation.desc)) {
							//@ASMHandler Class//
							asm.add(entry.getName());
						}
					}
				}
			}
		}catch(IOException e) {}
	}

	public List<String> asmHandlers() {
		return asm;
	}
	public List<String> modHandlers() {
		return mod;
	}




	/*********************************** Act As A ByteCode Source For The Mod File Object ****************************************/

	@Override
	public boolean contains(String classID) {
		try{
			JarFile jar = new JarFile(f);
			JarEntry e = jar.getJarEntry(classID);
			if(e != null) return true;
		}catch(Exception e) {}
		try{
			JarFile jar = new JarFile(f);
			JarEntry e = jar.getJarEntry(classID.replace(".","/")+".class");
			if(e != null) return true;
		}catch(Exception e) {}
		return false;
	}


	@Override
	public InputStream getStream(String classID) throws IOException {
		try{
			JarFile jar = new JarFile(f);
			JarEntry e = jar.getJarEntry(classID);
			if(e != null) {
				return jar.getInputStream(e);
			}
		}catch(Exception e) {}
		try{
			JarFile jar = new JarFile(f);
			JarEntry e = jar.getJarEntry(classID.replace(".","/")+".class");
			if(e != null) {
				return jar.getInputStream(e);
			}
		}catch(Exception e) {}
		return null;
	}
}
