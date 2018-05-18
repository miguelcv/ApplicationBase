package org.mcv.mu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ListMap<V> extends LinkedHashMap<String, V> {
	
	private static final long serialVersionUID = 1L;
	private static class Gensym {
		
		private String prefix;
		private long id;
		private String fmt;

		public Gensym() {
			this("G_");
		}
		
		public Gensym(String pref) {
			prefix = pref;
			id = (long) 0;
			setDigits(9);
		}

		public void setDigits(int d) {
			fmt = String.format("%%s%%0%dd", d);
		}

		public String nextSymbol() {
			return String.format(fmt, prefix, id++);
		}

	}

	private static Gensym gensym = new Gensym();
	
	@Override
	public V put(String key, V value) {
		return super.put(key==null? gensym.nextSymbol() : key, value);
	}
	
	ArrayList<V> list() {
		return new ArrayList<>(values());
	}
	
	V get(int index) {
		return list().get(index);
	}
	
	Iterator<V> iterator() {
		return list().iterator();
	}
	
	V[] toArray(V[] a) {
		return list().toArray(a);
	}
	
	Object[] toArray() {
		return list().toArray();
	}
	
	boolean contains(Object o) {
		return list().contains(o);
	}
	
	boolean containsAll(Collection<V> c) {
		return list().containsAll(c);
	}
	
	int indexOf(Object o) {
		return list().indexOf(o);
	}
	
	int lastIndexOf(Object o) {
		return list().lastIndexOf(o);
	}
	
	void forEach(Consumer<V> action) {
		list().forEach(action);
	}
	
	void ensureCapacity(int minCapacity) {
		list().ensureCapacity(minCapacity);
	}
	
	ListIterator<V> listIterator(int index) {
		return list().listIterator(index);
	}
	
	List<V>	sublist(int fromIndex, int toIndex) {
		return list().subList(fromIndex, toIndex);
	}
	
	Stream<V> parallelStream() {
		return list().parallelStream();
	}
	
	Stream<V> stream() {
		return list().stream();
	}

	List<V> asList() {
		return new ArrayList<>(list());
	}
	
	/* we don't support mutating operations, use the Map or copy asList() */
	// 	add [2x], addAll, set
	//	remove [2x], removeAll, removeIf
	//	replaceAll, retainAll
	//	sort, spliterator, trimToSize
}
