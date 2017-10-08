package net.openvoxel.client.renderer.vk.util;

import net.openvoxel.api.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Created by James on 11/05/2017.
 *
 * Compile GLSL Shader to SPIR-V
 *
 * TODO: convert to shaderc binding when it is added to lwjgl
 */
public class VkShaderCompiler {

	private static void fallback_write_source(String source) {
		try {
			FileOutputStream fout = new FileOutputStream("temp_shaderc_compile.tmp");
			fout.write(source.getBytes());
			fout.close();
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void fallback_clear_source() {
		File file = new File("temp_shaderc_compile.tmp");
		if(!file.delete()) {
			System.out.println("warn: did not clear source");
		}
	}

	private static void fallback_call_cmd() {
		ProcessBuilder _builder = new ProcessBuilder(
				List.of("./glslc","--target-env=vulkan","temp_shaderc_compile.tmp",
						"-o","temp_shaderc_result.tmp.spivasm"));
		_builder.directory(new File("temp.tmp").getAbsoluteFile().getParentFile());
		_builder.redirectErrorStream(true);
		try {
			Process proc = _builder.start();
			InputStream is = proc.getInputStream();
			boolean print_compile_output = true;
			int value;
			while ((value = is.read()) != -1) {
				if(print_compile_output) {
					print_compile_output = false;
					System.out.println("compile_output:");
				}
				System.out.print((char)value);
			}
			proc.waitFor();
			is.close();
			if(!print_compile_output) {
				System.out.print('\n');
			}
		}catch(Exception ex) {ex.printStackTrace();}
	}

	private static byte[] fallback_read_source() {
		File file = new File("temp_shaderc_result.tmp.spivasm");
		FileInputStream fileInputStream = null;
		byte[] data = null;
		try{
			fileInputStream = new FileInputStream(file);
			data = fileInputStream.readAllBytes();
			fileInputStream.close();
		}catch(Exception ex) {ex.printStackTrace();}
		if(!file.delete()) {
			return null;
		}
		return data;
	}

	public static byte[] compileSpiv(String source) {
		Logger.getLogger("Vk Spiv Compiler").Warning("Compiling using glslc CMD");
		fallback_write_source(source);
		fallback_call_cmd();
		fallback_clear_source();
		return fallback_read_source();
	}
}
