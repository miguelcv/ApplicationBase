package nl.novadoc.tools;

import java.util.Set;

import lombok.Data;
import lombok.experimental.Delegate;

@Data
public class Function {
	
	public Function(String name, Set<Type> typeArgs, Type returnType, Params valueArgs) {
		sig = new Signature(name, typeArgs, returnType, valueArgs);
		body = new Expression(returnType);
	}
	
	@Delegate final Signature sig;
	Expression body;
	
}
