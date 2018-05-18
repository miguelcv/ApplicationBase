var a: "global"

(
  fun assign() => String (
    a = "assigned"
  )

  var a: "inner"
  assign()
  print a // expect: inner
)

print a // expect: assigned
