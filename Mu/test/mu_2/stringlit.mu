var a: "interpolated"

// plain string literal
print "This is a plain string."

// multiline
print "This is a 
multiline 
string."

// raw
print "<{EOS}This is a raw string.{EOS}"

print "<{EOS}This is a 
multiline 
raw
string.{EOS}"

// bad raw
//print "<{END}his is not a raw string."		// Error unterminated string
print "<{1234567890}This is not a raw string.{1234567890}"

// no interpolation
print "<{EOS}This is #{a} raw string.{EOS}"

// no char escapes
print "<{EOS}This @{NL} is a raw string.{EOS}"

// interpolation
print "This is an #{a} string."
// charescapes
print "This @{NL} is a string."
// leftmargin
print "This is a
       multiline 
       string."
