package fl.engine.utils;

import java.io.ByteArrayOutputStream;

import java.io.PrintStream;

public class StringLogger extends PrintLogger {

	public StringLogger() {
		super.out = new PrintStream(byte_stream);
		super.show_position = false;
		super.show_stamina = false;
	}

	
	public String GetString() {
		return byte_stream.toString();
	}
	
	public void Clear() {
		byte_stream.reset();
	}

	private ByteArrayOutputStream byte_stream = new ByteArrayOutputStream();
}
