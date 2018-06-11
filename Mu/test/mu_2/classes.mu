// CLASSES
class Test1(Int arg1, String arg2, Real arg3) : (
	"I was called with: #{arg1} @{QUOTE}#{arg2}@{QUOTE} #{arg3}"
)

class Test2(Int arg1: 1, Int arg2: 10, Int arg3: 100) : (
	arg1 + arg2 + arg3
)

class Test3(Int arg1, String arg2, Real arg3, *) : (
	print "I was called with: #{arg1} #{arg2} #{arg3}"
	if $rest (print "extra params #{$rest}")
	else (print "no extra params")
)

// call: positional args
print Test1(3, "hello", 7.9)
print Test2(1, 2, 3)
Test3(1, "hi", 8.9, "vararg")

// call: keyword args
print Test1(arg3: 8.7, arg2: "out of order", arg1: 88)
print Test2(arg2: 4, arg3: 5, arg1: 6)
Test3(arg1: 4, arg3: 8.8, arg2: "xxxx")

// call: varargs
Test3(1, "hi", 8.9, "rest1", "rest2")

// call: mixed args
print Test1(arg3: 9.9, 1, "ok")
Test3(4, arg3: 8.8, arg2: "xxxx")

// call: default values
print Test2()
print Test2(arg3: 1000)

// nested, closure
class Outer(Int x): (
	var y: x
	class Inner(): (
		++y
	)
)

val f: Outer(3)
print f.Inner()

class Lam(Int x) : (
	var y: x
	fun func() => Int: (
		++y
	)
)

val lam: Lam(3)
print lam.func() // expect 4

var __x: 0

class Oo(Int x): (
	class Ii(prop Int y): (
		print y * 2
	)
)

print Oo(5).Ii(6).y
//return "ok"

// local
class Ooo(Int x) : (
	local fun iii(Int y) => Int: (
		y * 2
	)
)
//print Ooo(1).iii(3)

// own variables
class X(): (
	prop var instanceVar: 10
	own prop var staticVar: 10
)
print "class own " & X.staticVar
//print "class instance " & X.instanceVar // error
val x: X()
print "instance class " & x.staticVar;
print "instance instance " & x.instanceVar;
x.staticVar := 11
x.instanceVar := 11
print "updated class var " & x.staticVar;
val y: X()
print "class var now: " & y.staticVar;
print "instance var now: " & y.instanceVar;

class Func(): (
	own var v: 0
	v++
	print v
)

Func()
Func()
Func()
Func()
