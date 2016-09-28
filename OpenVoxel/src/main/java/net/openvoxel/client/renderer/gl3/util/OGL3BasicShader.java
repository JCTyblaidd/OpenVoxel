package net.openvoxel.client.renderer.gl3.util;

import net.openvoxel.client.renderer.gl3.OGL3Renderer;
import org.lwjgl.BufferUtils;

import javax.vecmath.Matrix4f;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;
import static org.lwjgl.opengl.GL40.GL_TESS_CONTROL_SHADER;
import static org.lwjgl.opengl.GL40.GL_TESS_EVALUATION_SHADER;

/**
 * Created by James on 25/08/2016.
 */
public class OGL3BasicShader {

	protected int program_ID  = -1;
	protected int vert_ID     = -1;
	protected int frag_ID     = -1;
	protected int tess_c_ID   = -1;
	protected int tess_e_ID   = -1;
	protected int geom_ID     = -1;
	protected String vert_source      = null;
	protected String frag_source      = null;
	protected String tess_c_source    = null;
	protected String tess_e_source    = null;
	protected String geom_source      = null;

	private static final String PRE_FLAG = "/**[";
	private static final String POST_FLAG = "]**/";

	protected final String DEBUG;

	public OGL3BasicShader(String shaderSource,String debugID) {
		DEBUG = debugID;
		String[] lines = shaderSource.split("\n");
		int last_index = 0;
		int last_loading = -1;
		for(int i = 0; i < lines.length; i++) {
			if(lines[i].startsWith(PRE_FLAG+"Fragment"+POST_FLAG)) {
				_store(last_loading,last_index,i,lines);
				last_index = i;
				last_loading = 0;
			}else if(lines[i].startsWith(PRE_FLAG+"Vertex"+POST_FLAG)) {
				_store(last_loading,last_index,i,lines);
				last_index = i;
				last_loading = 1;
			}else if(lines[i].startsWith(PRE_FLAG+"Tess Control"+POST_FLAG)) {
				_store(last_loading,last_index,i,lines);
				last_index = i;
				last_loading = 2;
			}else if(lines[i].startsWith(PRE_FLAG+"Tess Eval]"+POST_FLAG)) {
				_store(last_loading,last_index,i,lines);
				last_index = i;
				last_loading = 3;
			}else if(lines[i].startsWith(PRE_FLAG+"Geometry"+POST_FLAG)) {
				_store(last_loading,last_index,i,lines);
				last_index = i;
				last_loading = 4;
			}else if(lines[i].startsWith(PRE_FLAG+"End"+POST_FLAG)) {
				_store(last_loading,last_index,i,lines);
				last_index = i;
				last_loading = -1;
			}else{
				//DEBUG//
			}
		}
		try {
			//Build//
			program_ID = glCreateProgram();
			if (vert_source != null) {
				vert_ID = glCreateShader(GL_VERTEX_SHADER);
				_storeShaderSource(vert_ID, vert_source);
			}
			if (frag_source != null) {
				frag_ID = glCreateShader(GL_FRAGMENT_SHADER);
				_storeShaderSource(frag_ID, frag_source);
			}
			if (tess_c_source != null) {
				tess_c_ID = glCreateShader(GL_TESS_CONTROL_SHADER);
				_storeShaderSource(tess_c_ID, tess_c_source);
			}
			if (tess_e_source != null) {
				tess_e_ID = glCreateShader(GL_TESS_EVALUATION_SHADER);
				_storeShaderSource(tess_e_ID, tess_e_source);
			}
			if (geom_source != null) {
				geom_ID = glCreateShader(GL_GEOMETRY_SHADER);
				_storeShaderSource(geom_ID, geom_source);
			}
			//Build the program//
			if(vert_ID != -1) {glAttachShader(program_ID,vert_ID);}
			if(frag_ID != -1) {glAttachShader(program_ID,frag_ID);}
			if(tess_c_ID != -1) {glAttachShader(program_ID,tess_c_ID);}
			if(tess_e_ID != -1) {glAttachShader(program_ID,tess_e_ID);}
			if(geom_ID != -1) {glAttachShader(program_ID,geom_ID);}

			glLinkProgram(program_ID);
			glValidateProgram(program_ID);

			deleteShaders();
		}catch(Exception e) {
			deleteProgram();
			deleteShaders();
			OGL3Renderer.gl3Log.Severe("Failed to compile shaderProgram: " + debugID);
		}
	}

	public void Use() {
		glUseProgram(program_ID);
	}

	public void Forget() {
		glUseProgram(0);
	}

	public void deleteShaders() {
		if(vert_ID != -1) {glDeleteShader(vert_ID);}
		if(frag_ID != -1) {glDeleteShader(frag_ID);}
		if(tess_c_ID != -1) {glDeleteShader(tess_c_ID);}
		if(tess_e_ID != -1) {glDeleteShader(tess_e_ID);}
		if(geom_ID != -1) {glDeleteShader(geom_ID);}
		vert_ID = -1;
		frag_ID = -1;
		tess_c_ID = -1;
		tess_e_ID = -1;
		geom_ID = -1;
	}
	public void deleteProgram() {
		glDeleteProgram(program_ID);
		program_ID = -1;
	}


	protected void _storeShaderSource(int id, String source) {
		glShaderSource(id,source);
		glCompileShader(id);
		IntBuffer buffer = BufferUtils.createIntBuffer(1);
		buffer.position(0);
		glGetShaderiv(id,GL_COMPILE_STATUS,buffer);
		if(buffer.get(0) == GL_FALSE) {
			String reason = glGetShaderInfoLog(id);
			OGL3Renderer.gl3Log.Severe("Shader Failed to Compile["+DEBUG+"]: ");
			String[] lines = reason.split("\n");
			for(String line : lines) {
				OGL3Renderer.gl3Log.Severe("\t" + line);
			}
			throw new RuntimeException();
		}
	}

	protected void _store(int last_id,int last_index,int curr_index,String[] lines) {
		if(last_id == -1) return;
		StringBuilder builder = new StringBuilder();
		for(int i = last_index+1; i < curr_index; i++) {
			builder.append(lines[i]);
			builder.append("\n");
		}
		switch(last_id) {
			case 0:
				frag_source = builder.toString();
				break;
			case 1:
				vert_source = builder.toString();
				break;
			case 2:
				tess_c_source = builder.toString();
				break;
			case 3:
				tess_e_source = builder.toString();
				break;
			case 4:
				geom_source = builder.toString();
				break;
		}
	}


	protected final void setUniformMat4(int Uniform, Matrix4f mat) {
		float[] Data = new float[]{ mat.m00,mat.m10,mat.m20,mat.m30,
									mat.m01,mat.m11,mat.m21,mat.m31,
									mat.m02,mat.m12,mat.m22,mat.m32,
									mat.m03,mat.m13,mat.m23,mat.m33};
		glUniformMatrix4fv(Uniform,false,Data);
	}

}
