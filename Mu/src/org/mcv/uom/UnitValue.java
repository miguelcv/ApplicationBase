package org.mcv.uom;

import java.io.StringReader;

public class UnitValue {
	
	public final Double value;
	public Bag units = new Bag();

	public UnitValue(Double value, Bag units) {
		this.value = value;
		this.units = new Bag(units);
	}

	public UnitValue(String spec) {
		// value{prefixunit[^power][(/|*)prefixunit[^power]]...}
		try {
			spec = spec.replace(" ", "");
			StringReader r = new StringReader(spec);
			Bag units = new Bag();

			StringBuilder sb = new StringBuilder();
			for (;;) {
				r.mark(0);
				int c = r.read();
				if (c == -1) {
					throw new UoMError("Bad unit literal: expect value, got EOF");
				}
				if (isDoubleLiteral((char) c)) {
					sb.append((char)c);
				} else {
					r.reset();
					break;
				}
			}
			Double value = Double.parseDouble(sb.toString());

			Unit unit = parseUnit(r);
			units.add(unit);

			loop: for (;;) {

				int c = r.read();
				switch (c) {
				case '/':
					unit = parseUnit(r);
					unit.pow = -unit.pow;
					units.add(unit);
					break;
				case '*':
					unit = parseUnit(r);
					units.add(unit);
					break;
				case ' ':
				case '\r':
				case '\n':
				case -1:
					break loop;
				default:
					throw new UoMError("Bad unit literal: expect * or /, got " + (char)c);
				}
			}
			this.value = value;
			this.units = units;			
		} catch (Exception e) {
			throw new UoMError("Bad unit literal: " + e);
		}
	}

	public static Bag unitSpec(String spec) {
		// prefixunit[^power][(/|*)prefixunit[^power]]...
		try {
			spec = spec.replace(" ", "");
			StringReader r = new StringReader(spec);
			Bag units = new Bag();

			Unit unit = parseUnit(r);
			units.add(unit);

			loop: for (;;) {

				int c = r.read();
				switch (c) {
				case '/':
					unit = parseUnit(r);
					unit.pow = -unit.pow;
					units.add(unit);
					break;
				case '*':
					unit = parseUnit(r);
					units.add(unit);
					break;
				case -1:
					break loop;
				default:
					throw new UoMError("Bad unit literal: expect * or /, got " + (char)c);
				}
			}			
			return units;
			
		} catch (Exception e) {
			throw new UoMError("Bad unit literal: %s", e);
		}
	}

	static Unit parseUnit(StringReader r) {
		try {
			StringBuilder sb = new StringBuilder();
			for (;;) {
				r.mark(0);
				int c = r.read();
				//25..28
				if (Character.isAlphabetic(c) || Character.isDigit(c) || 
						// Unicode SYMBOLs
						Character.getType(c) >= 25 && Character.getType(c) <= 28) {
					sb.append((char)c);
				} else {
					r.reset();
					break;
				}
			}
			String prefixedUnit = sb.toString();
			int pow = 1;
			if(prefixedUnit.contains("^")) {
				String[] ss = prefixedUnit.split("\\^");
				prefixedUnit = ss[0];
				pow = Integer.parseInt(ss[1]);
			}
			return new Unit(prefixedUnit, pow);
		} catch (Exception e) {
			throw new UoMError("Bad unit literal: " + e);
		}
	}

	private boolean isDoubleLiteral(char c) {
		// digits
		if (Character.isDigit(c))
			return true;
		// + -
		if (c == '+' || c == '-')
			return true;
		// e E
		if (c == 'e' || c == 'E')
			return true;
		// . _
		if (c == '.' || c == '_')
			return true;
		return false;
	}

	public static double factor(String pfx) {
		switch (pfx) {
		case "d":
			return 1 / 10d;
		case "c":
			return 1 / 100d;
		case "m":
			return 1 / 1000d;
		case "µ":
			return 1 / 1_000_000d;
		case "n":
			return 1 / 1_000_000_000d;
		case "p":
			return 1 / 1_000_000_000_000d;
		case "f":
			return 1 / 1_000_000_000_000_000d;
		case "a":
			return 1 / 1_000_000_000_000_000_000d;
		case "z":
			return 1 / 1_000_000_000_000_000_000_000d;
		case "y":
			return 1 / 1_000_000_000_000_000_000_000_000d;
		case "da":
			return 10d;
		case "h":
			return 100d;
		case "k":
			return 1000d;
		case "M":
			return 1_000_000d;
		case "G":
			return 1_000_000_000d;
		case "T":
			return 1_000_000_000_000d;
		case "P":
			return 1_000_000_000_000_000d;
		case "E":
			return 1_000_000_000_000_000_000d;
		case "Z":
			return 1_000_000_000_000_000_000_000d;
		case "Y":
			return 1_000_000_000_000_000_000_000_000d;
		default:
			return 1;
		}
	}

	public boolean matches(UnitValue other) {
		if (units.size() != other.units.size())
			return false;
		for (int i = 0; i < units.size(); i++) {
			if (!units.get(i).matches(other.units.get(i))) {
				return false;
			}
		}
		return true;
	}
	
	@Override public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(value);
		String u = unitsToString();
		if(!u.isEmpty()) {
			sb.append("_");
			sb.append(u);
		}
		return sb.toString();
	}

	public String unitsToString() {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i < units.size(); i++) {
			Unit u = units.get(i);
			sb.append(u.toString());
			if(i==0 && u.unit.category.equals("Unit")) {
				sb.append("1");
			}
			if(i < units.size()-1) {
				if(!units.get(i+1).toString().isEmpty()) {
					if(units.get(i+1).pow < 0) {
						sb.append("/");
					} else {
						sb.append("*");
					}
				}
			}
		}
		return sb.toString();
	}

	public UnitValue in(String unit) {
		Bag bag = unitSpec(unit);
		UnitValue uv = new UnitValue(0.0, bag).expand();
		
		return in(uv);
	}
	
	public UnitValue in(Unit unit) {
		Bag newUnits = new Bag();
		newUnits.add(unit);
		UnitValue other = new UnitValue(0.0, newUnits).expand();
		return in(other);
	}
	
	private UnitValue in(UnitValue other) {
		UnitValue thiz = this.toBaseUnit();
		Double val = thiz.value;
		if(thiz.units.size() != other.units.size()) {
			throw new UoMError("Cannot express " + this + " in " + other.unitsToString());
		}
		for(int i=0; i < thiz.units.size(); i++) {
			Unit u1 = thiz.units.get(i);
			Unit u2 = other.units.get(i);
			if(u1.pow != u2.pow || !u1.unit.category.equals(u2.unit.category)) {
				throw new UoMError("Cannot express " + this + " in " + other.unitsToString());
			}
			if(u1.pow > 0) {
				val /= Math.pow(u2.unit.factor, u1.pow);
			} else {
				val *= Math.pow(u2.unit.factor, Math.abs(u1.pow));
			}
			val -= u2.unit.offset;
		}
		return new UnitValue(val, other.units);
	}

	public UnitValue toBaseUnit() {
		Double val = value;
		Bag newUnits = new Bag();
		for(Unit u : units) {
			val += u.unit.offset;
			if(u.pow > 0) {
				val *= Math.pow(u.unit.factor, u.pow);
			} else {
				val /= Math.pow(u.unit.factor, Math.abs(u.pow));
			}
			String name = Main.catUnit.get(u.unit.category);
			newUnits.add(new Unit(new UnitDef(u.unit.category, name, 0.0, 1.0), u.pow));
		}
		return new UnitValue(val, newUnits);
	}

	public UnitValue expand() {
		Double val = value;
		Bag newUnits = new Bag(units);
		boolean expanded = false;
		do {
			expanded = false;
			Bag tmp = new Bag();
			for(Unit u : newUnits) {
				if(!u.unit.units.isEmpty()) {
					expanded = true;
					tmp.addAll(u.unit.units);
				} else {
					tmp.add(u);
				}			
			}
			newUnits = new Bag(tmp);
		} while(expanded);
		return new UnitValue(val, newUnits);
	}

	public static UnitValue adapt(UnitValue uvFrom, UnitValue uvTo) {
		if(uvFrom.units.size() == uvTo.units.size()) {
			boolean allMatch = true;
			for(int i=0; i < uvFrom.units.size(); i++) {
				Unit u1 = uvFrom.units.get(i);
				Unit u2 = uvTo.units.get(i);
				if(u1.unit.name.equals(u2.unit.name)) {
					// same unit
				} else if(u1.unit.category.equals(u2.unit.category)) {
					// same category
					uvFrom = uvFrom.toBaseUnit();
					uvTo = uvTo.toBaseUnit();
					return adapt(uvFrom, uvTo);
				} else {
					allMatch = false; 
				}
			}
			if(allMatch) {
				return uvFrom;
			}
		}
		boolean expandable = false;
		for(int i=0; i < uvFrom.units.size(); i++) {
			Unit u1 = uvFrom.units.get(i);
			Unit u2 = uvTo.units.get(i);
			if(!u1.unit.units.isEmpty()) {
				expandable = true;
			}
			if(!u2.unit.units.isEmpty()) {
				expandable = true;
			}
		}
		if(expandable) {
			return adapt(uvFrom.expand(), uvTo.expand());
		}
		throw new UoMError("Incompatible units %s %s", uvFrom, uvTo);
	}
}
