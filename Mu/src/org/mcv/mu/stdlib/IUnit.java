package org.mcv.mu.stdlib;

import org.mcv.uom.Bag;
import org.mcv.uom.Unit;
import org.mcv.uom.UnitValue;
import org.mcv.uom.UoMError;

public class IUnit {
	
	private static final String INCOMPATIBLE_UNITS = "Incompatible units %s %s";

	private static UnitValue adapt(UnitValue a, UnitValue b) {
		try {
			return UnitValue.adapt(a, b);
		} catch(Exception e) {
			throw new UoMError(INCOMPATIBLE_UNITS, a.toString(), b.toString());			
		}
	}
	
	// ORD
	public static Boolean gt(UnitValue a, UnitValue b) {
		a = adapt(a, b);
		b = adapt(b, a);
		
		if(a.matches(b)) {
			if(a.value.isNaN() || b.value.isNaN()) {
				return false;
			}
			return a.value.compareTo(b.value) > 0;
		}
		throw new UoMError(INCOMPATIBLE_UNITS, a.toString(), b.toString());
	}

	public static Boolean ge(UnitValue a, UnitValue b) {
		a = adapt(a, b);
		b = adapt(b, a);
		
		if(a.matches(b)) {
			if(a.value.isNaN() || b.value.isNaN()) {
				return false;
			}
			return a.value.compareTo(b.value) >= 0;
		}
		throw new UoMError(INCOMPATIBLE_UNITS, a.toString(), b.toString());
	}
	
	public static Boolean lt(UnitValue a, UnitValue b) {
		a = adapt(a, b);
		b = adapt(b, a);

		if(a.matches(b)) {
			if(a.value.isNaN() || b.value.isNaN()) {
				return false;
			}
			return a.value.compareTo(b.value) < 0;
		}
		throw new UoMError(INCOMPATIBLE_UNITS, a.toString(), b.toString());
	}
	
	public static Boolean le(UnitValue a, UnitValue b) {
		a = adapt(a, b);
		b = adapt(b, a);

		if(a.matches(b)) {
			if(a.value.isNaN() || b.value.isNaN()) {
				return false;
			}
			return a.value.compareTo(b.value) <= 0;
		}
		throw new UoMError(INCOMPATIBLE_UNITS, a.toString(), b.toString());
	}

	// EQ
	public static Boolean eq(UnitValue a, UnitValue b) {
		a = adapt(a, b);
		b = adapt(b, a);

		if(a.matches(b))
			return a.equals(b);
		throw new UoMError(INCOMPATIBLE_UNITS, a.toString(), b.toString());
	}
	
	public static Boolean neq(UnitValue a, UnitValue b) {
		a = adapt(a, b);
		b = adapt(b, a);

		if(a.matches(b))
			return !eq(a, b);
		throw new UoMError(INCOMPATIBLE_UNITS, a.toString(), b.toString());
	}

	public static Boolean eqeq(UnitValue a, UnitValue b) {
		return eq(a, b);
	}
	
	public static Boolean neqeq(UnitValue a, UnitValue b) {
		return neq(a, b);
	}

	// PLUS/MINUS
	public static UnitValue plus(UnitValue a, UnitValue b) {
		a = adapt(a, b);
		b = adapt(b, a);
		
		if(a.matches(b)) {
			if(a.value.isNaN() || b.value.isNaN()) {
				return new UnitValue(Double.NaN, a.units);
			}
			return new UnitValue(a.value + b.value, a.units); 
		}
		throw new UoMError(INCOMPATIBLE_UNITS, a.toString(), b.toString());
	}

	public static UnitValue minus(UnitValue a, UnitValue b) {
		a = adapt(a, b);
		b = adapt(b, a);

		if(a.matches(b)) {
			if(a.value.isNaN() || b.value.isNaN()) {
				return new UnitValue(Double.NaN, a.units);
			}
			return new UnitValue(a.value - b.value, a.units); 
		}
		throw new UoMError(INCOMPATIBLE_UNITS, a.toString(), b.toString());
	}

	public static UnitValue neg(UnitValue a) {
		return new UnitValue(-a.value, a.units); 
	}

	public static UnitValue inc(UnitValue a) {
		return new UnitValue(a.value+1, a.units); 
	}
	
	public static UnitValue dec(UnitValue a) {
		return new UnitValue(a.value-1, a.units); 
	}

	// MULT/DIV
	public static UnitValue mul(UnitValue a, UnitValue b) {
		a = a.expand();
		b = b.expand();
		Bag units = new Bag();
		for(int i=0; i < a.units.size(); i++) {
			Unit u = a.units.get(i);
			boolean found = false;
			for(int j=0; j < b.units.size(); j++) {
				Unit u2 = b.units.get(j);
				if(u2.unit.equals(u.unit)) {
					units.add(new Unit(u.unit, u.pow + u2.pow));
					found = true;
					break;
				} else if(u2.unit.matches(u.unit)) {
					a = a.toBaseUnit();
					b = b.toBaseUnit();
					return mul(a, b);
				}
			}
			if(!found) {
				units.add(new Unit(u.unit, u.pow));
			}
		}
		for(int i=0; i < b.units.size(); i++) {
			Unit u = b.units.get(i);
			boolean found = false;
			for(int j=0; j < units.size(); j++) {
				Unit u2 = units.get(j);
				if(u2.unit.equals(u.unit)) {
					found = true;
					break;
				} else if(u2.unit.matches(u.unit)) {
					a = a.toBaseUnit();
					b = b.toBaseUnit();
					return mul(a, b);					
				}
			}
			if(!found) {
				units.add(new Unit(u.unit, u.pow));
			}
		}
		Double value = a.value * b.value;
		return new UnitValue(value, units);
	}
	
	public static UnitValue div(UnitValue a, UnitValue b)
	{	
		a = a.expand();
		b = b.expand();
		Bag units = new Bag();
		for(int i=0; i < a.units.size(); i++) {
			Unit u = a.units.get(i);
			boolean found = false;
			for(int j=0; j < b.units.size(); j++) {
				Unit u2 = b.units.get(j);
				if(u2.unit.equals(u.unit)) {
					units.add(new Unit(u.unit, u.pow - u2.pow));
					found = true;
					break;
				} else if(u2.unit.matches(u.unit)) {
					a = a.toBaseUnit();
					b = b.toBaseUnit();
					return div(a, b);
				}
			}
			if(!found) {
				units.add(new Unit(u.unit, u.pow));
			}
		}
		for(int i=0; i < b.units.size(); i++) {
			Unit u = b.units.get(i);
			boolean found = false;
			for(int j=0; j < units.size(); j++) {
				Unit u2 = units.get(j);
				if(u2.unit.equals(u.unit)) {
					found = true;
					break;
				} else if(u2.unit.matches(u.unit)) {
					a = a.toBaseUnit();
					b = b.toBaseUnit();
					return div(a, b);					
				}
			}
			if(!found) {
				units.add(new Unit(u.unit, u.pow));
			}
		}
		Double value = a.value / b.value;
		return new UnitValue(value, units);
	}
	
	public static UnitValue rem(UnitValue a, UnitValue b)
	{
		a = a.expand();
		b = b.expand();
		Bag units = new Bag();
		for(int i=0; i < a.units.size(); i++) {
			Unit u = a.units.get(i);
			boolean found = false;
			for(int j=0; j < b.units.size(); j++) {
				Unit u2 = b.units.get(j);
				if(u2.unit.equals(u.unit)) {
					units.add(new Unit(u.unit, u.pow + u2.pow));
					found = true;
					break;
				} else if(u2.unit.matches(u.unit)) {
					a = a.toBaseUnit();
					b = b.toBaseUnit();
					return mul(a, b);
				}
			}
			if(!found) {
				units.add(new Unit(u.unit, u.pow));
			}
		}
		for(int i=0; i < b.units.size(); i++) {
			Unit u = b.units.get(i);
			boolean found = false;
			for(int j=0; j < units.size(); j++) {
				Unit u2 = units.get(j);
				if(u2.unit.equals(u.unit)) {
					found = true;
					break;
				} else if(u2.unit.matches(u.unit)) {
					a = a.toBaseUnit();
					b = b.toBaseUnit();
					return mul(a, b);					
				}
			}
			if(!found) {
				units.add(new Unit(u.unit, u.pow));
			}
		}
		Double value = a.value % b.value;
		return new UnitValue(value, units);
	}

	// POW / SQRT
	public static UnitValue pow(UnitValue a, Integer b) {
		a = a.expand();
		Bag units = new Bag();
		for(int i=0; i < a.units.size(); i++) {
			Unit u = a.units.get(i);
			units.add(new Unit(u.unit, u.pow * Math.abs(b)));
		}
		Double value = Math.pow(a.value, b);
		return new UnitValue(value, units);
	}

	// OTHER
	public static UnitValue abs(UnitValue a) {
		return new UnitValue(Math.abs(a.value),a.units);
	}
	
	public static String toString(UnitValue a) {
		return a.value + " " + a.units.toString();
	}
	
}
