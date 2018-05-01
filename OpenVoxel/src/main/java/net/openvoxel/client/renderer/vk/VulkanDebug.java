package net.openvoxel.client.renderer.vk;

import net.openvoxel.loader.classloader.Validation;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeType;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;

import java.lang.reflect.Method;
import java.nio.*;

/**
 * Utility: Dump state to try debug crashes
 */
public class VulkanDebug {

	@Validation
	private static String Internal_ErrorString(String id,String prefix,Method method) {
		StringBuilder builder = new StringBuilder();
		builder.append(prefix).append(id).append(':');
		for(int i = id.length(); i < 32; i++) {
			builder.append(' ');
		}
		builder.append(method.getReturnType().getSimpleName());
		builder.append(" = ERROR!!");
		builder.append('\n');
		return builder.toString();
	}

	@Validation
	private static String Fallback_ToString(String id,String prefix,String type,Object obj) {
		StringBuilder builder = new StringBuilder();
		builder.append(prefix).append(id).append(':');
		for(int i = id.length(); i < 32; i++) {
			builder.append(' ');
		}
		builder.append(type);
		builder.append(" = ").append(obj == null ? "null" : obj.toString());
		builder.append('\n');
		return builder.toString();
	}

	@Validation
	private static String RawBuffer_ToString(String id,String prefix,String type,Buffer buffer) {
		StringBuilder builder = new StringBuilder();
		builder.append(prefix).append(id).append(':');
		for(int i = id.length(); i < 32; i++) {
			builder.append(' ');
		}
		builder.append(type);
		if(buffer == null) {
			builder.append(" = NULL");
		}else if(buffer instanceof ByteBuffer) {
			ByteBuffer bytes = (ByteBuffer) buffer;
			builder.append(" = 0x").append(MemoryUtil.memAddress(bytes)).append('\n');
			String prefix2 = prefix + "    ";
			for (int i = 0; i < buffer.capacity(); i++) {
				String new_id = id + "[" + i + "]";
				byte val = bytes.get(i);
				builder.append(Fallback_ToString(new_id, prefix2, type, val));
			}
		}else if(buffer instanceof CharBuffer) {
			CharBuffer bytes = (CharBuffer) buffer;
			builder.append(" = 0x").append(MemoryUtil.memAddress(bytes)).append('\n');
			String prefix2 = prefix + "    ";
			for (int i = 0; i < buffer.capacity(); i++) {
				String new_id = id + "[" + i + "]";
				char val = bytes.get(i);
				builder.append(Fallback_ToString(new_id, prefix2, type, val));
			}
		}else if(buffer instanceof ShortBuffer) {
			ShortBuffer bytes = (ShortBuffer) buffer;
			builder.append(" = 0x").append(MemoryUtil.memAddress(bytes)).append('\n');
			String prefix2 = prefix + "    ";
			for (int i = 0; i < buffer.capacity(); i++) {
				String new_id = id + "[" + i + "]";
				short val = bytes.get(i);
				builder.append(Fallback_ToString(new_id, prefix2, type, val));
			}
		}else if(buffer instanceof IntBuffer) {
			IntBuffer bytes = (IntBuffer) buffer;
			builder.append(" = 0x").append(MemoryUtil.memAddress(bytes)).append('\n');
			String prefix2 = prefix + "    ";
			for (int i = 0; i < buffer.capacity(); i++) {
				String new_id = id + "[" + i + "]";
				int val = bytes.get(i);
				builder.append(Fallback_ToString(new_id, prefix2, type, val));
			}
		}else if(buffer instanceof FloatBuffer) {
			FloatBuffer bytes = (FloatBuffer) buffer;
			builder.append(" = 0x").append(MemoryUtil.memAddress(bytes)).append('\n');
			String prefix2 = prefix + "    ";
			for (int i = 0; i < buffer.capacity(); i++) {
				String new_id = id + "[" + i + "]";
				float val = bytes.get(i);
				builder.append(Fallback_ToString(new_id, prefix2, type, val));
			}
		}else if(buffer instanceof LongBuffer) {
			LongBuffer bytes = (LongBuffer) buffer;
			builder.append(" = 0x").append(MemoryUtil.memAddress(bytes)).append('\n');
			String prefix2 = prefix + "    ";
			for (int i = 0; i < buffer.capacity(); i++) {
				String new_id = id + "[" + i + "]";
				long val = bytes.get(i);
				builder.append(Fallback_ToString(new_id, prefix2, type, val));
			}
		}else if(buffer instanceof DoubleBuffer) {
			DoubleBuffer bytes = (DoubleBuffer) buffer;
			builder.append(" = 0x").append(MemoryUtil.memAddress(bytes)).append('\n');
			String prefix2 = prefix + "    ";
			for (int i = 0; i < buffer.capacity(); i++) {
				String new_id = id + "[" + i + "]";
				double val = bytes.get(i);
				builder.append(Fallback_ToString(new_id, prefix2, type, val));
			}
		}else{
			throw new RuntimeException("Unknown Buffer Type!");
		}
		builder.append('\n');
		return builder.toString();
	}

	@Validation
	private static <OBJ extends Struct> String Internal_ToString(String id,String prefix, OBJ obj) {
		StringBuilder builder = new StringBuilder();
		builder.append(prefix).append(id).append(':');
		for(int i = id.length(); i < 32; i++) {
			builder.append(' ');
		}
		builder.append(obj.getClass().getSimpleName());
		builder.append(" = 0x").append(Long.toHexString(obj.address())).append('\n');
		String prefix2 = prefix + "    ";
		for(Method method : obj.getClass().getDeclaredMethods()) {
			NativeType _native = method.getAnnotation(NativeType.class);
			if(_native != null) {
				String param = method.getName();
				try {
					Object ret = method.invoke(obj);
					if(ret instanceof Buffer) {
						builder.append(RawBuffer_ToString(param,prefix2,_native.value(),(Buffer)ret));
					}else if(ret instanceof Struct) {
						builder.append(Internal_ToString(param,prefix2,(Struct)ret));
					}else if(ret instanceof StructBuffer) {
						builder.append(Internal_ToString(param,prefix2,(StructBuffer)ret));
					}else{
						builder.append(Fallback_ToString(param,prefix2,_native.value(),ret));
					}
				}catch(Exception ignored) {
					ignored.printStackTrace();
					builder.append(Internal_ErrorString(param,prefix2,method));
				}
			}
		}
		builder.append('\n');
		return builder.toString();
	}

	@Validation
	private static <BUFFER extends StructBuffer> String Internal_ToString(String id, String prefix, BUFFER buf) {
		StringBuilder builder = new StringBuilder();
		builder.append(prefix).append(id).append(':');
		for(int i = id.length(); i < 32; i++) {
			builder.append(' ');
		}
		builder.append(buf.getClass().getSimpleName()).append("[]");
		builder.append(" = 0x").append(Long.toHexString(buf.address())).append('\n');
		String prefix2 = prefix + "    ";
		for(int i = 0; i < buf.capacity(); i++) {
			String new_id = id+"["+i+"]";
			builder.append(Internal_ToString(new_id,prefix2,buf.get(i)));
		}
		builder.append('\n');
		return builder.toString();
	}

	@Validation
	public static <STRUCT extends Struct> String ToString(STRUCT struct) {
		return Internal_ToString("Struct","",struct);
	}

	@Validation
	public static <BUFFER extends StructBuffer> String ToString(BUFFER buffer) {
		return Internal_ToString("Buffer","",buffer);
	}

}
