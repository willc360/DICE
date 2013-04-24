package remoting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Locale;

public class PrintWriterWrapper extends PrintWriter implements Serializable, ReferenceObject {

	private PrintWriter pw;
	
	public PrintWriterWrapper(PrintWriter pw) {
		super(new ByteArrayOutputStream(0));
		this.pw = pw;
	}

	public PrintWriter append(char c) {
		return pw.append(c);
	}

	public PrintWriter append(CharSequence csq, int start, int end) {
		return pw.append(csq, start, end);
	}

	public PrintWriter append(CharSequence csq) {
		return pw.append(csq);
	}

	public boolean checkError() {
		return pw.checkError();
	}

	public void close() {
		pw.close();
	}

	public boolean equals(Object obj) {
		return pw.equals(obj);
	}

	public void flush() {
		pw.flush();
	}

	public PrintWriter format(Locale l, String format, Object... args) {
		return pw.format(l, format, args);
	}

	public PrintWriter format(String format, Object... args) {
		return pw.format(format, args);
	}

	public int hashCode() {
		return pw.hashCode();
	}

	public void print(boolean b) {
		pw.print(b);
	}

	public void print(char c) {
		pw.print(c);
	}

	public void print(char[] s) {
		pw.print(s);
	}

	public void print(double d) {
		pw.print(d);
	}

	public void print(float f) {
		pw.print(f);
	}

	public void print(int i) {
		pw.print(i);
	}

	public void print(long l) {
		pw.print(l);
	}

	public void print(Object obj) {
		pw.print(obj);
	}

	public void print(String s) {
		pw.print(s);
	}

	public PrintWriter printf(Locale l, String format, Object... args) {
		return pw.printf(l, format, args);
	}

	public PrintWriter printf(String format, Object... args) {
		return pw.printf(format, args);
	}

	public void println() {
		pw.println();
	}

	public void println(boolean x) {
		pw.println(x);
	}

	public void println(char x) {
		pw.println(x);
	}

	public void println(char[] x) {
		pw.println(x);
	}

	public void println(double x) {
		pw.println(x);
	}

	public void println(float x) {
		pw.println(x);
	}

	public void println(int x) {
		pw.println(x);
	}

	public void println(long x) {
		pw.println(x);
	}

	public void println(Object x) {
		pw.println(x);
	}

	public void println(String x) {
		pw.println(x);
	}

	public String toString() {
		return pw.toString();
	}

	public void write(char[] buf, int off, int len) {
		pw.write(buf, off, len);
	}

	public void write(char[] buf) {
		pw.write(buf);
	}

	public void write(int c) {
		pw.write(c);
	}

	public void write(String s, int off, int len) {
		pw.write(s, off, len);
	}

	public void write(String s) {
		pw.write(s);
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		
	}
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		//Reference ref = RemotingManager.getInstance().addReference(this);
		//stream.writeObject(ref);
	}

	public void returnReference(Reference ref) {
		/*String id = ref.getID();
		int size = stream.readInt();
		byte[] buf = new byte[size];
		stream.read(buf, 0, size);
		Reference reference = RemotingManager.getInstance().pullReference(id);
		PrintWriterWrapper pww = (PrintWriterWrapper) reference.getReference();
		pww.pw.print(new String(buf).toCharArray());*/
	}
	
}
