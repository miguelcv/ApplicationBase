package org.mcv.uom;

public class UnitDef {

	public UnitDef(String category, String name, Double offset, Double factor) {
		this.category = category;
		this.name = name;
		this.offset = offset;
		this.factor = factor;
	}

	public UnitDef(String category, String name, Bag units) {
		this.category = category;
		this.name = name;
		this.units = units;
		this.offset = 0.0;
		this.factor = 1.0;
	}

	public UnitDef(String category, String name, String units) {
		this.category = category;
		this.name = name;
		this.units = UnitValue.unitSpec(units);
		this.offset = 0.0;
		this.factor = 1.0;
	}

	String category;
	String name;
	Double offset;
	Double factor;
	Bag units = new Bag();
	
	public String toFullString() {
		return category+":"+name+"+"+offset+ "*" + factor + "(" + units + ")";
	}

	@Override public String toString() {
		return name;
	}

	public boolean matches(Object other) {
		if(other instanceof UnitDef) {
			return this.category.equals(((UnitDef)other).category);
		}
		return false;
	}
	
	@Override public boolean equals(Object o) {
		if(o instanceof UnitDef) {
			UnitDef other = (UnitDef)o; 
			return this.category.equals(other.category) && this.name.equals(other.name);
		}
		return false;
	}

}
