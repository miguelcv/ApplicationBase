// funcdef assign/init
val f: fun (Int x) => Int: (
	4 * x
)
print f(4)

// doc-comment
doc("This is a doc comment")
fun foo() => Int: (
	44
)
print foo.doc

// params dependent on other params
fun testparams(Int a, Int b: a, Int c: a) => Int: (
	a + b + c
)
print testparams(5)  // expect 15

// type params + currying
fun typeparams(Type T, T a, T b) => T: (
	a + b
)
print typeparams(T: Int, 5, 7)  // expect 12
print typeparams(Int)(5,7)  // expect 12

// immediate function call
print (fun (Int x) => Int: (
	4 * x
)(4))

// safe navigation ?. ?[ ?(
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
	    family=> nil,
	    age=> 25
	), nil
]

print people[0]?.family?.mother
print people[1]?.family?.mother
print people[2]?.family?.mother

// system properties
print system.user
print system.currentDirectory
print system.unicodeNormalization
print system.os

// program arguments
print $arguments.len

// object properties
// str
var a: 99.9
print a.str
print system.str
print people.type

print true.ord
print Bool.minval
print Bool.maxval
print Bool.values

type Months: enum[January, February, March, April, May, June, July, August, September, October, November, December]
print March.ord
print Months.minval
print Months.maxval
print Months.values

print 'c'.name
print Bool.interfaces
print [1,2,3].eltType
print foo.returnType
print foo.paramTypes
print foo.attributes
