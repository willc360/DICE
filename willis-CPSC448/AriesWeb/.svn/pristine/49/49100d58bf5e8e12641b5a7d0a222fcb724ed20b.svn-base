package remoting;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface TypeSerializer<T> {
	void write(T t, ObjectOutputStream out);
	T read(ObjectInputStream in);
}
