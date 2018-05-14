package nl.novadoc.tools;

import java.util.Set;

import lombok.Data;

@Data
public class Signature {
	final String name;
	final Set<Type> typeArgs;
	final Type returnType;
	final Params valueArgs;
}
