var i: 0

var w: while i < 10
	++i
print "while-loop value: " & w

until i = 0 
	--i
print "done: " & i
i <- 6
for i in [0..10)
	print i
print i
do
	print i++
while i < 10


// break continue after
print "fake break"
i := 0
while i < 10
	print ++i
	if i = 11 (break)
after
	print "after"

print "break"
i := 0
while i < 10
	++i
	if i = 3 (break)
	print i
after
	print "won't print"

print "continue"
i := 0
while i < 10
	++i
	if i = 3 (continue)
	print i
print i
