package org.mcv.uom;

import java.util.HashMap;
import java.util.Map;
import static org.mcv.mu.stdlib.IUnit.*
;

public class Main {

	static final String[] SIPREFIXES = { "d", "c", "m", "µ", "n", "p", "f", "a", "z", "y", "da", "h", "k", "M", "G",
			"T", "P", "E", "Z", "Y" };

	public static Map<String, UnitDef> symtable = new HashMap<>();
	public static Map<String, String> catUnit = new HashMap<>();

	static {
		// empty unit
		unitDef("Unit", "1");

		// SI base units
		unitDefs("Time", "s");
		genTimeUnits();
		unitDefs("Distance", "m");
		unitDefs("Temperature", "K");
		unitDefs("ElectricCurrent", "A");
		unitDefs("LuminousIntensity", "cd");
		unitDefs("Mass", "kg");
		unitDefs("Mass", "g", 0, 1.0 / 1000);
		unitDefs("AmountOfSubstance", "mol");
		unitDef("Angle", "rad");
		unitDef("SolidAngle", "sr");

		// temperature
		unitDef("Temperature", "°C", 273.15, 1.0);
		unitDef("Temperature", "°F", 459.67, 5.0/9.0);
		
		// ???
		unitDef("Percent", "%", 0.0, 1.0 / 100);

		// derived
		unitDefs("Frequency", "Hz", "1/s");
		unitDefs("Force", "N", "m*kg/s^2");
		unitDefs("Pressure", "Pa", "N/m^2");
		unitDefs("Energy", "J", "N*m");
		unitDefs("Power", "W", "J/s");
		unitDefs("ElectricCharge", "C", "s*A");
		unitDefs("ElectricPotential", "V", "W/A");
		unitDefs("ElectricCapacitance", "F", "C/V");
		unitDefs("ElectricResistance", "Ω", "V/A");
		unitDefs("ElectricConductance", "S", "A/V");
		unitDefs("MagneticFlux", "Wb", "V*s");
		unitDefs("MagenticFluxDensity", "T", "Wb/m^2");
		unitDefs("ElectricInductance", "H", "Wb/A");
		unitDefs("LuminousFlux", "lm", "cd*sr");
		unitDefs("Illuminance", "lx", "lm/m^2");
		unitDefs("Radioactivity", "Bq", "1/s");
		unitDefs("RadiationDoseAbsorbed", "Gy", "J/kg");
		unitDefs("RadiationDoseEffective", "Sv", "J/kg");
		unitDefs("CatalyticActivity", "kat", "mol/s");
		unitDefs("Speed", "m/s", "m/s");
		unitDefs("Acceleration", "m/s²", "m/s^2");
		unitDefs("Area", "m²", "m^2");
		unitDefs("Volume", "m³", "m^3");
		unitDefs("Speed", "km/h", "km/h");
	}

	private static void unitDef(String category, String unit) {
		UnitDef def = new UnitDef(category, unit, 0.0, 1.0);
		symtable.put(unit, def);
		catUnit.put(category, unit);
	}

	private static void unitDefs(String category, String unit, String unitspec) {

		UnitDef def = new UnitDef(category, unit, UnitValue.unitSpec(unitspec));
		symtable.put(unit, def);
		for (String pfx : SIPREFIXES) {
			def = new UnitDef(category, pfx + unit, 0.0, UnitValue.factor(pfx));
			symtable.put(pfx + unit, def);
		}
	}

	private static void unitDef(String category, String unit, double offset, double factor) {
		UnitDef def = new UnitDef(category, unit, offset, factor);
		symtable.put(unit, def);
	}

	private static void unitDefs(String category, String unit, double offset, double factor) {
		UnitDef def = new UnitDef(category, unit, offset, factor);
		symtable.put(unit, def);
		for (String pfx : SIPREFIXES) {
			def = new UnitDef(category, pfx + unit, offset, factor * UnitValue.factor(pfx));
			symtable.put(pfx + unit, def);
		}
	}

	private static void unitDefs(String category, String unit) {
		UnitDef def = new UnitDef(category, unit, 0.0, 1.0);
		symtable.put(unit, def);
		catUnit.put(category, unit);
		for (String pfx : SIPREFIXES) {
			def = new UnitDef(category, pfx + unit, 0.0, UnitValue.factor(pfx));
			symtable.put(pfx + unit, def);
		}
	}

	private static void genTimeUnits() {
		// min
		UnitDef def = new UnitDef("Time", "min", 0.0, 60.0);
		symtable.put("min", def);
		// hour
		def = new UnitDef("Time", "h", 0.0, 60.0 * 60.0);
		symtable.put("h", def);
		// day
		def = new UnitDef("Time", "d", 0.0, 60.0 * 60.0 * 24.0);
		symtable.put("d", def);
		// week
		def = new UnitDef("Time", "wk", 0.0, 60.0 * 60.0 * 24.0 * 7.0);
		symtable.put("wk", def);
		// year
		def = new UnitDef("Time", "yr", 0.0, 60.0 * 60.0 * 24.0 * 365.254);
		symtable.put("yr", def);
	}

	public static void main(String[] args) {

		System.out.println(Main.symtable.keySet());
		
		// NEG INC DEC ABS
		UnitValue value = new UnitValue("5km/h");
		System.out.println(neg(value));
		System.out.println(inc(value));
		System.out.println(dec(value));
		System.out.println(abs(value));
		
		// PLUS MINUS: units must match
		UnitValue value2 = new UnitValue("2km/h");
		System.out.println(plus(value, value2));
		System.out.println(minus(value, value2));
		// EQ NEQ EQEQ NEQEQ
		System.out.println(eq(value, value2));
		System.out.println(neq(value, value2));
		// GT GE LT LE
		System.out.println(gt(value, value2));
		System.out.println(lt(value, value2));
		
		// MUL DIV REM
		
		UnitValue sec = new UnitValue("10s");
		UnitValue meter = new UnitValue("100m");
		System.out.println(mul(sec,meter));
		System.out.println(div(sec,meter));
		System.out.println(rem(sec,meter));
		System.out.println(mul(meter,sec));
		System.out.println(div(meter,sec));
		System.out.println(rem(meter,sec));
	
		UnitValue kelvin = new UnitValue("0K");
		UnitValue celsius = new UnitValue("0°C");
		UnitValue fahr = new UnitValue("100°F");
		System.out.println(plus(celsius,fahr));		// 584.077 
		System.out.println(plus(kelvin,celsius));	// 273.15
		
		System.out.println("***");
		System.out.println(celsius.in("°F"));  // 32
		System.out.println(celsius.in("°C"));  // 0 
		System.out.println(celsius.in("K"));   // 273.15
		System.out.println("***");
		
		UnitValue ampere = new UnitValue("100A");
		UnitValue candela = new UnitValue("2kcd");
		System.out.println(mul(ampere, candela));
		System.out.println(div(ampere, candela));
		
		UnitValue gram = new UnitValue("2g");
		UnitValue mole = new UnitValue("22mol");
		System.out.println(mul(gram, mole));

		System.out.println(gram + " " + gram.in(new Unit("kg", 1)));
		UnitValue kilo = new UnitValue("2kg");
		System.out.println(plus(gram, kilo));
		System.out.println(mul(gram, kilo));
		System.out.println(div(gram, kilo));
		
		
		System.out.println();

		// derived
		UnitValue newton = new UnitValue("10N");
		UnitValue pascal = new UnitValue("10Pa");
		UnitValue joule = new UnitValue("10J");
		UnitValue watt = new UnitValue("10W");
		UnitValue volt = new UnitValue("1V");
		UnitValue ohm = new UnitValue("10Ω");
		System.out.println("10N x 10N = " + mul(newton, newton)); 	// 100 m2 kg2 / s4
		System.out.println("10N x 10Pa = " + mul(newton, pascal));	// 100 kg2 / s4
		System.out.println("10N x 10J = " + mul(newton, joule));	// 100 m3 kg2 / s4
		System.out.println("10N x 10W = " + mul(newton, watt));		// 100 m3 kg2 / s5
		System.out.println("10N x 1V = " + mul(newton, volt));		// 10 m3 kg2 / s5 / A
		System.out.println("10N x 10Ω = " + mul(newton, ohm));		// 100 m3 kg2 / s5 / A2
		
		System.out.println("10N / 10N = " + div(newton, newton));
		System.out.println("10N / 10Pa = " + div(newton, pascal));
		System.out.println("10N / 10J = " + div(newton, joule));
		System.out.println("10N / 10W = " + div(newton, watt));
		System.out.println("10N / 1V = " + div(newton, volt));
		System.out.println("10N / 10Ω = " + div(newton, ohm));

		System.out.println("10N ^ 2 = " + pow(newton, 2));
		System.out.println("10Pa ^ 2 = " + pow(pascal, 2));
		System.out.println("10J ^ 2 = " + pow(joule, 2));
		System.out.println("10W ^ 2 = " + pow(watt, 2));
		System.out.println("1V ^ 2 = " + pow(volt, 2));
		System.out.println("10Ω ^ 2 = " + pow(ohm, 2));

		UnitValue accel = new UnitValue("10m/s^2");
		System.out.println(accel.in("km/s^2"));
	}

}
