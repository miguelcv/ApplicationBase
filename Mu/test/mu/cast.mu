type Num: Int|Real
var x: Num
x := 4
print x 
print x.type
print x.type.type
print x.type.type.type

if x.type = Real (print "real") else (print "not real")
if x.type = Int (print "int") else (print "not int")
if x.type == Real (print "real") else (print "not real")
if x.type == Int (print "int") else (print "not int")

assert x.type.type == Type

//x := 7.8
//print x
var y: x as Real
print y
y := 6.2
print y
//y := 8

if y.type = Real (print "real")

print $line
print $file
print $func
print $date
print $time

class Point(Int x, Int y): (
)

val pt1: Point(5,6)
val pt2: Point(0,0)

print pt1 = pt1
print pt1 = pt2
print pt1
 

