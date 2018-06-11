var $a: 9
print $a

var bf: false
var bt: true

var nn: nil
var nnn: 7

fun ff(): (
	throw "error"
)
fun fs(): (
	22
)

var se: ""
var sf: "Hello"

fun test(): (
	/* booleans */
	if{"null", "fail"} bf (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"null", "fail"} bt (
		print "truthy"
	) else (
		print "falsy"
	)
	/* empty */
	if{"null", "fail"} se (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"null", "fail"} sf (
		print "truthy"
	) else (
		print "falsy"
	)
	/* NULLs */
	if{"null", "fail"} nnn (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"null", "fail"} nn (
		print "truthy"
	) else (
		print "falsy"
	)
	/* fail */
	if{"null", "fail"} fs() (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"null", "fail"} ff() (
		print "truthy"
	) else (
		print "falsy"
	)
)

test()