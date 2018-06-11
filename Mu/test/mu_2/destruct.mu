var a: Int
var b: Int
var c: Int|Void
var vec: [1,2]
print vec.type
(a,b,c) := vec
print a
print b
print c

// JS examples
(a, b) := [1, 2]
assert a=1
assert b=2
(a, b) := (a=>1, b=>2)
assert a = 1
assert b = 2
 
var x: [1, 2, 3, 4, 5]
var y, z: x
(y, z) := x;
assert y = 1
assert z = 2

var foo: ["one", "two", "three"]
var one, two, three: foo;
(one, two, three) := foo;
assert one = "one"
assert two = "two"
assert three = "three"

(a, b) := [1, 2]
assert a = 1
assert b = 2

//(a=>5, b=>7) := [1]
//assert a = 1
//assert b = 7

a := 1
b := 3
(a, b) := [b, a]
print "swap 1 > 3 " & a // 3
print "swap 3 > 1 " & b // 1

fun f() => list(Int): (
	[1, 2]
)

(a, b) := f()
assert a = 1
assert b = 2


var o: (p => 42, q => true)
var p: Int
var q: Bool
(p, q) := o
assert p = 42
assert q

var a, b: (b=>1, a=>2)
assert a=2
assert b=1

var people: [
	(
    	name=> "Mike Smith",
	    family=> (
			mother=> "Jane Smith",
	 	 	father=> "Harry Smith",
			sister=> "Samantha Smith"
	    ),
	    age=> 35
	), (
		name=> "Tom Jones",
	    family=> (
			mother=> "Norah Jones",
			father=> "Richard Jones",
			brother=> "Howard Jones"
		),
	    age=> 25
	)
]

print people
