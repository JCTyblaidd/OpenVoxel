package com.jc.util.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class FileHandle {
	
	private File handle;
	
	private FileOutputStream out;
	
	public FileHandle(File f) {
		handle = f;
	}
	public FileHandle(String s) {
		this(new File(s));
	}
	public FileHandle(File f, String s) {
		this(new File(f,s));
	}
	public FileHandle(FileHandle f, String s) {
		this(new File(f.handle,s));
	}
	
	public byte[] getBytes() {
		if(!handle.exists()) return null;
		try{
			FileInputStream fin = new FileInputStream(handle);
			byte[] arr = new byte[fin.available()];
			fin.read(arr);
			fin.close();
			return arr;
		}catch(Exception e) {
			return null;
		}
	}
	
	public String getString() {
		byte[] arr = getBytes();
		return arr == null ? null : new String(arr);
	}
	
	public String getString(Charset cset) {
		byte[] arr = getBytes();
		return arr == null ? null : new String(arr,cset);
	}
	
	public void delete() {
		handle.delete();
	}
	
	public void startWrite() {
		try{
			out = new FileOutputStream(handle);
		}catch(Exception e) {}
	}
	
	public void startAppended() {
		try{
			out = new FileOutputStream(handle, true);
		}catch(Exception e) {}
	}
	
	public void write(byte[] arr) {
		if(out != null) {
			try {
				out.write(arr);
			} catch (IOException e) {}
		}
	}
	public void write(String str) {
		write(str.getBytes());
	}
	public void write(String s, Charset cset) {
		write(s.getBytes(cset));
	}
	
	public void stopWrite() {
		if(out != null) {
			try {
				out.close();
			} catch (IOException e) {}
		}
	}
	
}
