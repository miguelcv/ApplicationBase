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
//print s += s2
print i += i2
print c += c2
//print b += b2
print r += r2
// int -> real
//print i += r2
print r += i2

//MINIS:
//print s -= s2
print i -= i2
print c -= c2
//print b -= b2
print r -= r2
// int -> real
//print i -= r2
print r -= i2

//STARIS:
//print s *= s2
print i *= i2
//print c *= c2
//print b *= b2
print r *= r2
// int -> real
//print i *= r2
print r *= i2

//SLASHIS:
//print s /= s2
print i /= i2
//print c /= c2
//print b /= b2
print r /= r2
// int -> real
//print i /= r2
print r /= i2

//PERCENTIS:
//print s %= s2
print i %= i2
//print c %= c2
//print b %= b2
print r %= r2
// int -> real
//print i %= r2
print r %= i2

//POWIS:
//print s ^= s2
print i ^= i2
//print c ^= c2
// XOR!
print b ^= b2
print r ^= r2
// int -> real
//print i ^= r2
print r ^= i2

//ANDIS:
print s &= s2
print i &= i2
//print c &= c2
print b &= b2
//print r &= r2
// char -> string
print s &= c2
//print c &= s2

//ORIS:
//print s |= s2
print i |= i2
//print c |= c2
print b |= b2
//print r |= r2

//LSHIFTIS:
//print s <<= s2
print i <<= i2
//print c <<= c2
//print b <<= b2
//print r <<= r2

//RSHIFTIS:
//print s >>= s2
print i >>= i2
//print c >>= c2
//print b >>= b2
//print r >>= r2

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
