fun curry(Int a, Int b, Int c) => Int: (
	a + b + c
)

print curry(1)(2)(3)

var f: curry(c: 9)

print f(2, 3)
