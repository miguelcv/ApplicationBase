package org.mcv.uom;

public class Unit {
	
	public Unit(String unit, int pow) {
		this.unit = Main.symtable.get(unit);
		this.pow = pow;
	}
	
	public Unit(UnitDef unit, int pow) {
		this.unit = unit;
		this.pow = pow;
	}
	
	public UnitDef unit;
	public int pow;
	
	@Override public String toString() {
		if(pow == 0) return "";
		if(unit.category.equals("Unit")) return "";
		else if(pow == 1) return unit.name;
		else return unit.name + "^" + pow;
	}
	
	@Override public boolean equals(Object other) {
		if(other instanceof Unit) {
			return this.unit.equals(((Unit)other).unit);
		}
		return false;
	}
	
	public boolean matches(Object other) {
		if(other instanceof Unit) {
			return this.unit.matches(((Unit)other).unit);
		}
		return false;
	}
}

