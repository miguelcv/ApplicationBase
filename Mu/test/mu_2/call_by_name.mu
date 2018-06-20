// infinite loop
for i in [0..Inf) (
	print i
	if i == 10 (break)
)

for i in [0..-Inf) (
	print i
	if i == -10 (break)
)

// call by name
fun cbn(thunk Real expr) : (
	var i: 66.0
	print expr
)

cbn(sqrt i)

// Inf and NaN as Ints
var q: list(Int)
var r: Bool

var i: Int
i := -Inf
i := NaN
print i

// nullable types
var s: String?
s := nil
print s

// AOP
fun f1(): (
	print "this is the function itself"
) 

fun f2(): (
	throw "error!!"
)
error f2: (
	print "An error occurred: #{$exception}"
)

f2()
before f1: (
	print "BEFORE::"
)
after f1: (
	print "AFTER::"
)
after f1: (
	print "stacked after..."
)
always f1: (
	print "ALWAYS::"
)
around f1: (
	print "AROUND1::"
	$this()
	print "AROUND2::"	
)
around f1: (
	print "next around"
	$this()
	print "next after"
)
f1()

// mixins
class T(): (
	prop val y: 99
	fun foo(): (
		print "call foo"
	)
	print "T constructor"
)

var t: T()
print t.y
t.foo()

class X(): (
	fun foo(): (
		print "call FOO"
	)
	mixin var m: T() where ("bar" => "foo")
)
var x: X()
//print x.y
x.bar()
x.foo()

// "pointers"
var i: 100
var ip: ref(Int)
//var ip: @i
ip := @i
print ip.type
print i
print ip
print ↑ip
