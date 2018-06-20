// type alias
type Double: Real

var x: Double

x := 6.9
print x

print x.type

// list type
type IntVector: list(Int)
var vec: IntVector
vec := [2, 4, 5, 99]
print vec
print vec.type

var vec2: [2, 4.7, true]
print vec2
print vec2.type
 
// set type
type RealSet: set(Real)
var rset: RealSet
rset := {2.9, 4.0, 5.8, 99.0}
print rset

// map type
type StringMap: map(String)
var smap: StringMap
smap := ("a" => "z", "z" => "a")
print smap

// ref type
type IntPtr: ref(Int)
var i: 0
var ip: @i
print ip
print ↑ip
 
// fun type
type Predicate: fun(Any) => Bool

// class type
type Point: class(Int, Int y)

// intersection type
type Num: Int & Real

// union type
type U: Bool|Int|Real
var u: U
u := 6
print u
u := true
print u
u := 6.9
print u

type NullableString: String|Void
var s: NullableString
s := nil
s := "hi"
print s

type Lowercase: enum['a'..'z']
var lc: Lowercase
lc := 'y'
print lc
print lc.type

type Rng: enum[1..100]
var r: Rng
//r := 0
r := 1
//r := 200

//enum{} type
type Suits: enum{hearts, spades, diamonds, clubs}
var suit: Suits
suit := spades
print suit

// enum[] type
type Maybe: enum[nothing, something]
var perhaps: Maybe
perhaps := something
print perhaps
