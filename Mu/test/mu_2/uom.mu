var speed: 100_km/h
var speed2: 100_km/h
print speed * speed2 * speed * speed2

siunit Memory: "b"
siunit Memory: "B" [0, 8]

var x: 3_MB * 6
print x in "b"

// NEG ABS
var value: 5_km/h
print -value
print \value\
		
// PLUS MINUS: units must match
var value2: 2_km/h
print value + value2
print value - value2
// EQ NEQ EQEQ NEQEQ
print value = value2
print value ~= value2
// GT GE LT LE
print value > value2
print value < value2
// MUL DIV REM
var sec: 10_s
var meter: 100_m
print sec * meter
print sec / meter
print sec % meter
print meter * sec
print meter / sec
print meter % sec
	
var kelvin: 0_K
var celsius: 0_°C
var fahr: 100_°F
print celsius + fahr	// 584.077
print kelvin + celsius	// 273.15
		
print "***"
print celsius in "°F"  // 32
print celsius in "°C"  // 0 
print celsius in "K"   // 273.15
print "***"
		
var ampere: 100_A
var candela: 2_kcd
print ampere * candela
print ampere / candela

var gram: 2_g
var mole: 22_mol
print gram * mole

print gram.str & " and " & gram in "kg"

var kilo: 2_kg
print gram + kilo
print gram * kilo
print gram / kilo
		
// derived
var newton: 10_N
var pascal: 10_Pa
var joule: 10_J
var watt: 10_W
var volt: 1_V
var ohm: 10_Ω

print "10N x 10N = " & newton * newton 	// 100 m2 kg2 / s4
print "10N x 10Pa = " & newton * pascal	// 100 kg2 / s4
print "10N x 10J = " & newton * joule	// 100 m3 kg2 / s4
print "10N x 10W = " & newton * watt	// 100 m3 kg2 / s5
print "10N x 1V = " &  newton * volt	// 10 m3 kg2 / s5 / A
print "10N x 10Ω = " & newton * ohm		// 100 m3 kg2 / s5 / A2
		
print "10N / 10N = " & newton / newton
print "10N / 10Pa = " & newton /pascal
print "10N / 10J = " & newton / joule
print "10N / 10W = " & newton / watt
print "10N / 1V = " & newton / volt
print "10N / 10Ω = " & newton / ohm

print "10N ^ 2 = " & newton ^ 2
print "10Pa ^ 2 = " & pascal ^ 2
print "10J ^ 2 = " & joule ^ 2
print "10W ^ 2 = " & watt ^ 2
print "1V ^ 2 = " & volt ^ 2
print "10Ω ^ 2 = " & ohm ^ 2

var accel: 10_m/s^2
print accel in "km/s^2"
