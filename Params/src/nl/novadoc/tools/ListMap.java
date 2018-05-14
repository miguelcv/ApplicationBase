package nl.novadoc.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import lombok.experimental.Delegate;

public class ListMap<K,V> {
	@Delegate LinkedHashMap<K,V> self = new LinkedHashMap<>();
	
	ArrayList<V> list() {
		return new ArrayList<>(self.values());
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
		return new ArrayList<V>(list());
	}
	
	/* we don't support mutating operations, use the Map or copy asList() */
	// 	add 2x, addAll, set
	//	remove 2x, removeAll, removeIf
	//	replaceAll, retainAll
	//	sort, spliterator, trimToSize
}
