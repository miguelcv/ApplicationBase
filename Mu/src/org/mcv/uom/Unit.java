package org.mcv.uom;

import org.mcv.mu.MuException;

public class Unit {
	
	public Unit(String unit, int pow) {
		this.unit = UnitRepo.symtable.get(unit);
		this.unitName = unit;
		this.pow = pow;
	}
	
	public Unit(UnitDef unit, int pow) {
		this.unit = unit;
		this.unitName = unit.name;
		this.pow = pow;
	}
	
	public void resolve() {
		this.unit = UnitRepo.symtable.get(unitName);
		if(this.unit == null) {
			throw new MuException("Unknown unit %s", unitName);
		}		
	}
	
	public String unitName;
	public UnitDef unit;
	public int pow;
	
	@Override public String toString() {
		if(pow == 0) return "";
		if(unit.category.equals("Unit")) return "";
		else if(Math.abs(pow) == 1) return unit.name;
		else return unit.name + superscript(Math.abs(pow));
	}
	
	private String superscript(int num) {
		String s = String.valueOf(num);
		StringBuilder sb = new StringBuilder();
		for(int i=0; i < s.length(); i++) {
			char c = s.charAt(i);
			if(c == '2') {
				sb.appendCodePoint(0xB2);
			} else if(c == '3') {
				sb.appendCodePoint(0xB3);
			} else {
				sb.appendCodePoint(0x2070 + (c-'0'));
			}
		}
		return sb.toString();
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

