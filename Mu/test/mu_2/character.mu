/* UNICODE names */
print "@{LATIN SMALL LETTER A WITH OGONEK}"
print "@{FACE WITH STUCK-OUT TONGUE AND TIGHTLY-CLOSED EYES}"

/* UNICODE hex */
print "@{283}"
print "@{1F441}"

/* HTML entities */
print "@{&planck}"

/* [La]TeX entities*/
print "@{\Sigma}"

// as char literals
/* UNICODE names */
print '@{LATIN SMALL LETTER A WITH OGONEK}'
print '@{FACE WITH STUCK-OUT TONGUE AND TIGHTLY-CLOSED EYES}'

/* UNICODE hex */
print '@{283}'
print '@{1F441}'

/* HTML entities */
print '@{&planck}'

/* [La]TeX entities*/
print '@{\Sigma}'

/* Unicode operators etc. */
var empty: Ø
print empty
empty ← {1, 2, 3}

var mp: (a → 2, b → 4)
for i in mp
	print i

var d: ∞
print d
print 9 ÷ 3
print 3 × 9
var b: true ⊕ true
print b
if ¬b (print "hello")
print √9
if 3 ≤ 3 (print "yes")
if 10 ≥ 5 (print "yes")
