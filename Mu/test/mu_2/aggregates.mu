// lists
var l: [1, 2, 3, 4]
// TODO make this OK with union types
//var badList: [1, "b"]
print l
print "expect 2: " & l[1]
//print l[11]
l[0] := 100
print l[0]
print l

// sets
var s: {1, 2, 3, 4}
// TODO make this OK with union types
//var badSet: {"a", 4, 4.8}
print s
//print "expect error: " & s[0]

// maps
var m: (a => 9, b => 11, z=>33)
// TODO make this work with union types [currently: Type.Any]
var m2: (a => 9, b => 11, z => "string")
print m
print m2
print "expect 9: " & m["a"]
//print "expect 9: " & m[a]
//print m["x"]
print m.b
m.a := 1000
print m
print m.a
m["a"] := 3000
print m
print m["a"]

// slices
print l[0..0]
print l[0..1]
print l[0..2]
print l[0..3]
print l[0..4]
//print l[0..5]

// negative subscripts
print l[-1]
print l[-2]
//print l[-5]

// multidimensional arrays
var matrix: [
	[1, 2, 3, 4],
	[5, 6, 7, 8]
]
print matrix
print matrix[0][1]  // expect 2

// nested sets:
var sets: {
	{1, 2, 3, 4},
	{5, 6, 7, 8}
}
print sets
for i in sets (
	for j in i (
		print j
	)
)

// nested maps
var nested: (a => 9, nest => (x => 1, y => 6, z => 8), b => 11, c=>33)
print nested
print nested["nest"]["z"]
print nested.nest.z
