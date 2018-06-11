// assignops
var s: "String"
var i: 10
var c: 'c'
var b: true
var r: 6.7

var s2: "String"
var i2: 10
var c2: 'c'
var b2: true
var r2: 6.7

//PLUSIS:
print i += i2
print c += c2
print r += r2
print r += i2

//MINIS:
print i -= i2
print c -= c2
print r -= r2
// int -> real
print r -= i2

//STARIS:
print i *= i2
print r *= r2
// int -> real
print r *= i2

//SLASHIS:
print i /= i2
print r /= r2
// int -> real
print r /= i2

//PERCENTIS:
print i %= i2
print r %= r2
// int -> real
print r %= i2

//POWIS:
print i ^= i2
// XOR!
print b ^= b2
print r ^= r2
// int -> real
print r ^= i2

//ANDIS:
print s &= s2
print i &= i2
print b &= b2
// char -> string
print s &= c2

//ORIS:
print i |= i2
print b |= b2

//LSHIFTIS:
print i <<= i2

//RSHIFTIS:
print i >>= i2

// INC
print "inc i " & i
print i++
print ++i
print i

print "inc c " & c
print c++
print ++c
print c

print "inc b " & b
print b++
print ++b
print b

r := 3.5
print "inc r " & r
print r++
print ++r
print r

// DEC
print "inc i " & i
print i--
print --i
print i

print "inc c " & c
print c--
print --c
print c

print "inc b " & b
print b--
print --b
print b

print "inc r " & r
print r--
print --r
print r
