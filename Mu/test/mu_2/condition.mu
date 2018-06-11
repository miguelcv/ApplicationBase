/* conditions */

var a: 10
var b : 100

if a < b (
	print "yes"
)

if a < b (
	print "yes"
) else (
	print "no"
)

var b: if a<b (6) else (7)

unless a < b (
	print "no"
) else (
	print "yes"
)

val i: 3

var x: select i
	when 0 (1)
	when 1 (2)
	when 2 (3)
	when 3 (4)
	when 4 (5)
	when 5 (6)
	else (0)

print x

var z: nil

if z (
	print "true"
) else (
	print "false"
)
