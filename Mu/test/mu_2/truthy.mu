// booleans
var b: true

if b (print "OK") else (print "NOK")
unless b (print "NOK") else (print "OK")

if ~b (print "NOK") else (print "OK")
unless ~b (print "OK") else (print "NOK")

b := false

if b (print "NOK") else (print "OK")
unless b (print "OK") else (print "NOK")

if ~b (print "OK") else (print "NOK")
unless ~b (print "NOK") else (print "OK")

// nils
var v: nil
if v (print "NOK") else (print "OK")
unless v (print "OK") else (print "NOK")

if ~v (print "OK") else (print "NOK")
unless ~v (print "NOK") else (print "OK")


// exceptions
fun ff(): (
	throw "error"
)
if ff() (print "NOK") else (print "OK")
unless ff() (print "OK") else (print "NOK")

if ~ff() (print "OK") else (print "NOK")
unless ~ff() (print "NOK") else (print "OK")

// futures
jvm class Thread(): "java.lang.Thread"

fun async() => Int: (
	Thread.sleep(1000)
	33
)
var task: start async
var future: task()

if future (print "NOK") else (print "OK")
future:^
if future (print "OK") else (print "NOK")

// aggregates
var s: ""
var lst: []
var mp: map(Bool)
var st: {}
if s (print "NOK") else (print "OK")
if s (print "NOK") else (print "OK")
if s (print "NOK") else (print "OK")
if s (print "NOK") else (print "OK")

s := "Hello"
lst := [1, 2, 3]
mp := (p-> true, q -> false)
st := {"x", "y", "z"}
unless s (print "NOK") else (print "OK")
unless s (print "NOK") else (print "OK")
unless s (print "NOK") else (print "OK")
unless s (print "NOK") else (print "OK")

var $a: 9
print $a

val bool_false: false
val bool_true: true

val void_nil: nil
var non_nil: 7

fun ffail(): (
	throw "error"
)
fun fsucceed(): (
	22
)

var string_empty: ""
var string_full: "Hello"

fun testStrict(): (
	/* booleans */
	if{"strict"} bool_false (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"strict"} bool_true (
		print "truthy"
	) else (
		print "falsy"
	)
	/* empty */
	if{"strict"} string_empty (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"strict"} string_full (
		print "truthy"
	) else (
		print "falsy"
	)
	/* NULLs */
	if{"srict"} non_nil (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"strict"} void_nil (
		print "truthy"
	) else (
		print "falsy"
	)
	/* fail */
	if{"strict"} fsucceed() (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"strict"} ffail() (
		print "truthy"
	) else (
		print "falsy"
	)
)
fun testStrict(): (
	/* booleans */
	if{"strict"} bool_false (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"strict"} bool_true (
		print "truthy"
	) else (
		print "falsy"
	)
	/* empty */
	if{"strict"} string_empty (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"strict"} string_full (
		print "truthy"
	) else (
		print "falsy"
	)
	/* NULLs */
	if{"srict"} non_nil (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"strict"} void_nil (
		print "truthy"
	) else (
		print "falsy"
	)
	/* fail */
	if{"strict"} fsucceed() (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"strict"} ffail() (
		print "truthy"
	) else (
		print "falsy"
	)
)

fun testEmpty(): (
	/* booleans */
	if{"empty"} bool_false (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"empty"} bool_true (
		print "truthy"
	) else (
		print "falsy"
	)
	/* empty */
	if{"empty"} string_empty (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"empty"} string_full (
		print "truthy"
	) else (
		print "falsy"
	)
	/* NULLs */
	if{"empty"} non_nil (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"empty"} void_nil (
		print "truthy"
	) else (
		print "falsy"
	)
	/* fail */
	if{"empty"} fsucceed() (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"empty"} ffail() (
		print "truthy"
	) else (
		print "falsy"
	)
)

fun testFail(): (
	/* booleans */
	if{"fail"} bool_false (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"fail"} bool_true (
		print "truthy"
	) else (
		print "falsy"
	)
	/* empty */
	if{"fail"} string_empty (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"fail"} string_full (
		print "truthy"
	) else (
		print "falsy"
	)
	/* NULLs */
	if{"fail"} non_nil (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"fail"} void_nil (
		print "truthy"
	) else (
		print "falsy"
	)
	/* fail */
	if{"fail"} fsucceed() (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"fail"} ffail() (
		print "truthy"
	) else (
		print "falsy"
	)
)

fun testNull(): (
	/* booleans */
	if{"null"} bool_false (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"null"} bool_true (
		print "truthy"
	) else (
		print "falsy"
	)
	/* empty */
	if{"null"} string_empty (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"null"} string_full (
		print "truthy"
	) else (
		print "falsy"
	)
	/* NULLs */
	if{"null"} non_nil (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"null"} void_nil (
		print "truthy"
	) else (
		print "falsy"
	)
	/* fail */
	if{"null"} fsucceed() (
		print "truthy"
	) else (
		print "falsy"
	)
	if{"null"} ffail() (
		print "truthy"
	) else (
		print "falsy"
	)
)

error testStrict: (
	print "Caught: #{$exception}"
)
error testEmpty: (
	print "Caught: #{$exception}"
)
error testFail: (
	print "Caught: #{$exception}"
)
error testNull: (
	print "Caught: #{$exception}"
)

print "strict"
testStrict()
print "empty"
testEmpty()
print "fail"
testFail()
print "null"
testNull()
