package net.openvoxel.lauch.gui;

import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by James on 20/09/2016.
 */
public class RichFormattedArea extends ScrollPane{
	private TextFlow flow;
	public final OutputStream OUTPUT_HANDLE;

	public RichFormattedArea() {
		OUTPUT_HANDLE = new TextStream(this);
		this.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		this.setHbarPolicy(ScrollBarPolicy.ALWAYS);
		flow = new TextFlow();
		this.setContent(flow);
		this.setFitToWidth(true);
		this.setFitToHeight(true);
		updateColor(0);
		//
		//writeLine("HI");
		//writeLine("NOPE\n");
		//String str = (char)27 + "[36m[TRACE]"+(char)27+"[0m TRACE\n";
		//writeLine(str);
	}

	private Color currentColor;

	private static final char splitChar = (char)27;
	private static final String splitStr = "" + splitChar;

	/**
	 * Write Line
	 * @param text MUST INCLUDE \n at the end already
	 */
	public synchronized void writeLine(String text) {
		try {
			String[] strs = text.split(splitStr);
			if (strs.length == 1) {
				addText(new Text(text));
			} else {
				int startID;
				if (text.charAt(0) == splitChar) {
					//FORMAT FIRST//
					startID = 0;
				} else {
					//FORMAT AFTER//
					addText(new Text(strs[0]));
					startID = 1;
				}
				for (int i = startID; i < strs.length; i++) {
					String val = strs[i];
					if(val.length() == 0) {
						continue;
					}
					int index = val.indexOf('m');
					String prefix = val.substring(0, index);
					String key = prefix.substring(1);
					int KEY = Integer.valueOf(key);
					String toPrint = val.substring(index+1);
					updateColor(KEY);
					addText(new Text(toPrint));
				}
			}
		}catch(Exception e) {
			addText(new Text("#FORMATTING-ERROR# " + text));
		}
	}

	private void updateColor(int key) {
		switch (key) {
			case 30:
				currentColor = Color.BLACK;
				return;
			case 31:
				currentColor = Color.DARKRED;
				return;
			case 32:
				currentColor = Color.GREEN;
				return;
			case 33:
				currentColor = Color.YELLOW;
				return;
			case 34:
				currentColor = Color.BLUE;
				return;
			case 35:
				currentColor = Color.DARKMAGENTA;
				return;
			case 36:
				currentColor = Color.CYAN;
				return;
			case 37:
				currentColor = Color.LIGHTGRAY;
				return;
			case 90:
				currentColor = Color.DARKGRAY;
				return;
			case 91:
				currentColor = Color.RED;
				return;
			case 92:
				currentColor = Color.LIGHTGREEN;
				return;
			case 93:
				currentColor = Color.LIGHTYELLOW;
				return;
			case 94:
				currentColor = Color.LIGHTBLUE;
				return;
			case 95:
				currentColor = Color.MAGENTA;
				return;
			case 96:
				currentColor = Color.LIGHTCYAN;
				return;
			case 97:
				currentColor = Color.WHITE;
				return;
			case 0:
			default:
				currentColor = Color.BLACK;
				return;
		}
	}

	private void addText(Text text) {
		text.setFill(currentColor);
		flow.getChildren().add(text);
	}

	private static class TextStream extends OutputStream {
		private StringBuilder str = new StringBuilder();
		private RichFormattedArea console;
		public TextStream(RichFormattedArea v) {
			console = v;
		}
		@Override
		public void write(int b) throws IOException {
			str.append((char)(b));
			_attemptWrite(b);
		}

		private void _attemptWrite(int b) {
			if(b == '\n') {
				console.writeLine(str.toString());
				str.setLength(0);
			}
		}
	}

}
