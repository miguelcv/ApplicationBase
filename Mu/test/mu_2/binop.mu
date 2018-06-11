// divide by zero:
error $this: (
	print "Top level error handler: #{$exception}"
)
var z: 7/0

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

//AND
print s & s2
print i & i2
print b & b2
// char -> string
print s & c2
print c & s2

//OR
print i | i2
print b | b2

//GREATER
print s > s2
print i > i2
print c > c2
print b > b2
print r > r2

//GREATER_EQUAL
print s >= s2
print i >= i2
print c >= c2
print b >= b2
print r >= r2

//LESS
print s < s2
print i < i2
print c < c2
print b < b2
print r < r2

//LESS_EQUAL
print s <= s2
print i <= i2
print c <= c2
print b <= b2
print r <= r2

//MINUS
print i - i2
print c - c2
print r - r2
// int -> real
print i - r2
print r - i2

//SLASH
print i / i2
print r / r2
// int -> real
print i / r2
print r / i2

//PERCENT
print i % i2
print r % r2
// int -> real
print i % r2
print r % i2

//STAR
print i * i2
print r * r2
// int -> real
print i * r2
print r * i2

//PLUS
print i + i2
print c + c2
print r + r2
// int -> real
print i + r2
print r + i2

//NOT_EQUAL
print s ~= s2
print i ~= i2
print c ~= c2
print b ~= b2
print r ~= r2

//EQUAL
print s = s2
print i = i2
print c = c2
print b = b2
print r = r2

//EQEQ
print s == s2
print i == i2
print c == c2
print b == b2
print r == r2

//NEQEQ
print s ~== s2
print i ~== i2
print c ~== c2
print b ~== b2
print r ~== r2

//POW
print i ^ i2
print r ^ r2
print i ^ r2
print r ^ i2

//LEFTSHIFT
print i << i2

//RIGHTSHIFT
print i >> i2

//XOR
print i xor i2
print b xor b2
