package remoting;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Locale;
import java.util.UUID;

public class PrintWriter  implements Serializable, Identifiable, Returnable  {
	
	private transient java.io.PrintWriter pw;
	private transient ByteArrayOutputStream bytes;
	private transient String id;
	private transient CharArrayWriter chars;
	
	CharArrayWriter getChars() {
		return chars;
	}
	
	public PrintWriter() {
	}
	
	public PrintWriter(java.io.PrintWriter pw) {
		this.id = UUID.randomUUID().toString();
		this.pw = pw;
	}

	public java.io.PrintWriter append(char c) {
		return pw.append(c);
	}

	public java.io.PrintWriter append(CharSequence csq, int start, int end) {
		return pw.append(csq, start, end);
	}

	public java.io.PrintWriter append(CharSequence csq) {
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

	public java.io.PrintWriter format(Locale l, String format, Object... args) {
		return pw.format(l, format, args);
	}

	public java.io.PrintWriter format(String format, Object... args) {
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

	public java.io.PrintWriter printf(Locale l, String format, Object... args) {
		return pw.printf(l, format, args);
	}

	public java.io.PrintWriter printf(String format, Object... args) {
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
		this.id = (String) stream.readObject();
		this.chars = new CharArrayWriter();
		this.pw = new java.io.PrintWriter(this.chars);
		RemotingManager.getInstance().addServerReference(this);
	}
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeObject(id);
		RemotingManager.getInstance().addClientReference(id, this);
	}

	@Override
	public String getID() {
		return id;
	}

	@Override
	public void returnToClient(ObjectOutputStream stream) throws IOException {
		byte[] bytes = new String(chars.toCharArray()).getBytes();
		PrintWriterReturnProxy p = new PrintWriterReturnProxy(id, bytes);
		stream.writeObject(p);
	}
	
}
