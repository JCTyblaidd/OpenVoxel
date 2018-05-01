package net.openvoxel.client.renderer.vk;

import org.lwjgl.system.NativeType;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;

import java.lang.reflect.Method;

public class VulkanDebug {

	private static String Internal_ErrorString(String id,String prefix,Method method) {
		StringBuilder builder = new StringBuilder();
		builder.append(prefix).append(id).append(':');
		for(int i = id.length(); i < 32; i++) {
			builder.append(' ');
		}
		builder.append(method.getReturnType().getSimpleName());
		builder.append('\n');
		return builder.toString();
	}

	private static String Fallback_ToString(String id,String prefix,String type,Object obj) {
		StringBuilder builder = new StringBuilder();
		builder.append(prefix).append(id).append(':');
		for(int i = id.length(); i < 32; i++) {
			builder.append(' ');
		}
		builder.append(type);
		builder.append(" = ").append(obj.toString());
		builder.append('\n');
		return builder.toString();
	}

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
					if(ret instanceof Struct) {
						builder.append(Internal_ToString(param,prefix2,(Struct)ret));
					}else if(ret instanceof StructBuffer) {
						builder.append(Internal_ToString(param,prefix2,(StructBuffer)ret));
					}else{
						builder.append(Fallback_ToString(param,prefix2,_native.value(),ret));
					}
				}catch(Exception ignored) {
					builder.append(Internal_ErrorString(param,prefix2,method));
				}
			}
		}
		builder.append('\n');
		return builder.toString();
	}

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

	public static <STRUCT extends Struct> String ToString(STRUCT struct) {
		return Internal_ToString("Struct","",struct);
	}

	public static <BUFFER extends StructBuffer> String ToString(BUFFER buffer) {
		return Internal_ToString("Buffer","",buffer);
	}

}
