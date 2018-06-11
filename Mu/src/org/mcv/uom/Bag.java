package org.mcv.uom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Bag implements Iterable<Unit> {

	List<Unit> list = new ArrayList<>();
	
	public Bag(Bag bag) {
		for(Unit unit : bag) {
			list.add(new Unit(unit.unit, unit.pow));
		}
	}

	public Bag() {
	}

	public void add(Unit unit) {
		for(Unit u : list) {
			if(u.unit.name.equals(unit.unit.name)) {
				int pow = u.pow += unit.pow;
				list.remove(u);
				list.add(new Unit(u.unit, pow));
				return;
			} else if(u.unit.category.equals(unit.unit.category)) {
				throw new UoMError("Units are not the same scale!");
			}
		}
		list.add(new Unit(unit.unit, unit.pow));
	}

	public int size() {
		return list.size();
	}

	public Unit get(int i) {
		return list.get(i);
	}

	@Override
	public Iterator<Unit> iterator() {
		return list.iterator();
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public void addAll(Bag units) {
		for(Unit u : units) {
			add(u);
		}
	}
	
	@Override public String toString() {
		return list.toString();
	}
}
