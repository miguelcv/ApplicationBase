package org.mcv.mu;

public class Pointer implements Comparable<Pointer> {
	
	Object ref;
	Type type;
	
	public Pointer(Object ref) {
		this.ref = ref;
		this.type = Interpreter.typeFromClass(ref.getClass());
	}
	
	public Object getRef() {
		return ref;
	}

	public void setRef(Object a) {
		ref = a;
		this.type = Interpreter.typeFromClass(ref.getClass());
	}
	
	@Override public String toString() {
		return Integer.toHexString(System.identityHashCode(ref));
	}

	@Override
	public int compareTo(Pointer p) {
		return toString().compareTo(p.toString());
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof Pointer) {
			return toString().equals(((Pointer)o).toString());
		}
		return false;
	}

}
