// userdefined operators:
// ascii PREFIX
op(":>>>>>>>") fun prefix(Real right) => Real: (
	right * 100
)
// ascii POSTFIX
op(":<<<<<<<") fun postfix(Real left) => Real: (
	left / 100
)

print :>>>>>>>5.8 
print 500.7:<<<<<<<

// ascii RANDOM CHARS
op(":~!@#$%^&*") fun gibberish(Int left, Int right) => Int : (
	left * right - left + right
)

print gibberish(2,3)
print gibberish(2,6)
print 2 :~!@#$%^&* 3
print 2 :~!@#$%^&* 6

// Unicode Symbols
op("≈") fun almosteq(Int left, Int right) => Bool : (
	return abs (left-right) < 2
)

print almosteq(2,3)
print almosteq(2,6)
print 2 ≈ 3
print 2 ≈ 6

// ASCII KEYWORDS
op(":eqv") fun eqv(Int left, Int right) => Bool : (
	abs (left-right) < 2
)

print 2 :eqv 3
print 2 :eqv 6

// redefine existing operator
class Point(Int x, Int y): (
	fun toString(Point pt) => String: (
		"(#{pt.x},#{pt.y})"
	)
)

op("+") fun plus(Point left, Point right) => Point : (
	Point(left.x + right.x, left.y + right.y)
)

var p1: Point(0,5)
var p2: Point(5,0)
print "" & (p1 + p2)

// type params in aggregate types
fun product(Type T, list(T) lst) => T: (
	var ret: 1
	for i in lst (
		ret *= i
	)
	ret
)
print product(Int)([3,6,8,90])

// divide by zero:
error $this: (
	print "Top level error handler: #{$exception}"
)
var z: 7/0

// BINOPS
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

var i: 10
var r: 6.7
var i2: 10
var r2: 6.7

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
