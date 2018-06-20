// JVM Import to map
val log: jvm import "Logger" from "log4j:log4j"
val om: jvm import "ObjectMapper" from "com.fasterxml.jackson.core:jackson-databind"

var logger: log.Logger.getLogger("mu")
logger.debug("Hi there")

var i: 7
var mapper: om.ObjectMapper()
print mapper.writeValueAsString(i)

// JVOM import to environment
jvm import "Logger" from "log4j:log4j"
jvm import "ObjectMapper" from "com.fasterxml.jackson.core:jackson-databind"

var logger: Logger.getLogger("mu")
logger.debug("Hi there")

var i: 7
var mapper: ObjectMapper()
print mapper.writeValueAsString(i)


// Mu import local file
import "../test.mu"
test.xyz()
// shouldn't work!
//test.abc()
test()()

// Mu Git import
//import "Mu/test/mu_2/aggregates.mu" from "miguelcv:ApplicationBase:nostmt"
import (aggr => "Mu/test/mu_2/aggregates.mu") from "miguelcv:ApplicationBase:nostmt"

aggr()
