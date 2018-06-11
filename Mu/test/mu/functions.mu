// escaped identifiers
var `map: (a => "b", b => "c")
print `map

// functions:
// definition
fun test1(Int arg1, String arg2, Real arg3) => String : (
	"I was called with: #{arg1} @{QUOTE}#{arg2}@{QUOTE} #{arg3}"
)

fun test2(Int arg1: 1, Int arg2: 10, Int arg3: 100) => Int : (
	arg1 + arg2 + arg3
)

fun test3(Int arg1, String arg2, Real arg3, *) : (
	print "I was called with: #{arg1} #{arg2} #{arg3}"
	if $rest (print "extra params #{$rest}")
	else (print "no extra params")
)

// call: positional args
print test1(3, "hello", 7.9)
print test2(1, 2, 3)
test3(1, "hi", 8.9, "vararg")

// call: keyword args
print test1(arg3: 8.7, arg2: "out of order", arg1: 88)
print test2(arg2: 4, arg3: 5, arg1: 6)
test3(arg1: 4, arg3: 8.8, arg2: "xxxx")

// call: varargs
test3(1, "hi", 8.9, "rest1", "rest2")

// call: mixed args
print test1(arg3: 9.9, 1, "ok")
test3(4, arg3: 8.8, arg2: "xxxx")

// call: default values
print test2()
print test2(arg3: 1000)

// nested, closure
fun outer(Int x) => Int: (
	var y: x
	fun inner() => Int: (
		++y
	)
	inner
)

val f: outer(3)
print f()	// expect 4

fun lam(Int x) => fun() => Int : (
	var y: x
	return fun() => Int: (
		++y
	)
)

print lam(3)() // expect 4

// local
fun ooo(Int x) => Int: (
	local fun iii(Int y) => Int: (
		y * 2
	)
	x / 2
)
//print ooo.iii(3)

// own variables
fun x(): (
	own prop var staticVar: 10
)
print x.staticVar

fun func(): (
	own var v: 0
	v++
	print v
)

func()
func()
func()
func()

// return statement
fun withReturn(Int x) => String: (
	if x = 0 ( 
		return "Zero"
	)
	return "Non-zero"
)
print withReturn(0)
print withReturn(1)

var a: "global"
(
	fun showA(): (
	    print a
	)

	showA()
	var a: "block"
	showA()
)

// curry => see curry.mu
// aop => see aop.mu
// recursion => see fib.mu
