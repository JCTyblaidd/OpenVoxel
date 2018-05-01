package net.openvoxel.client.renderer.vk.pipeline;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import net.openvoxel.utility.CrashReport;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

final class VulkanSpirVCompiler {


	private static ByteBuffer compileGLSL_NV(String src,List<String> defines) {
		StringBuilder new_src = new StringBuilder();
		for(String define : defines) {
			new_src.append("#define ").append(define).append('\n');
		}
		new_src.append(src);
		return MemoryUtil.memUTF8(new_src.toString());
	}


	private static ByteBuffer compileFallback(String src,List<String> defines) {
		VulkanUtility.LogWarn("Using ./glslc command Fallback SpirV Compiler");
		try {
			byte[] data = compileSpiv(src, defines);
			ByteBuffer DATA = MemoryUtil.memAlloc(data.length);
			DATA.put(data);
			DATA.position(0);
			return DATA;
		}catch(Exception ex) {
			CrashReport report = new CrashReport("Failed to compile SpirV");
			report.caughtException(ex);
			OpenVoxel.reportCrash(report);
			return null;
		}
	}

	static ByteBuffer compileShader(boolean has_glsl_extensions,String src, List<String> defines) {
		if(has_glsl_extensions) {
			return compileGLSL_NV(src,defines);
		}else{
			return compileFallback(src,defines);
		}
	}


	///
	/// Old Hacky Fallback Compiler
	///


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

	private static void fallback_call_cmd(List<String> defines) {
		List<String> cmd_list = new ArrayList<>();
		cmd_list.addAll(List.of("./glslc","--target-env=vulkan"));
		for(String define : defines) {
			cmd_list.add("-D"+define);
		}
		cmd_list.addAll(List.of("temp_shaderc_compile.tmp",
				"-o","temp_shaderc_result.tmp.spivasm"));
		ProcessBuilder _builder = new ProcessBuilder(cmd_list);
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
		byte[] data = null;
		try(FileInputStream fileInputStream = new FileInputStream(file)){
			data = fileInputStream.readAllBytes();
		}catch(Exception ex) {ex.printStackTrace();}
		if(!file.delete()) {
			return null;
		}
		return data;
	}

	public static byte[] compileSpiv(String source,List<String> defines) {
		Logger.getLogger("Vulkan").getSubLogger("SPIR-V Compiler").Warning("Compiling using glslc CMD");
		fallback_write_source(source);
		fallback_call_cmd(defines);
		fallback_clear_source();
		return fallback_read_source();
	}

}
