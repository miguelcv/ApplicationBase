package org.mcv.app;

import java.util.ArrayList;
import java.util.List;

import jodd.proxetta.ProxyAdvice;
import jodd.proxetta.ProxyTarget;
import jodd.proxetta.ProxyTargetInfo;

/**
 * @author Miguelc
 *
 */
public class MethodAdvice implements ProxyAdvice {

	/* 
	 * Log entry, exit/errorExit for a public method.
	 */
	@Override
	public Object execute() throws Exception {
		ProxyTargetInfo info = ProxyTarget.info();
		Base base = (Base)ProxyTarget.target();
		try {
			// ENTRY LOG
			if(check(info)) {
				List<Object> args = new ArrayList<>();
				for (int i = 0; i < info.argumentCount; i++) {
					args.add(info.arguments[i]);
				}
				base.entry(args);
			}
			// INVOKE
			Object retval = ProxyTarget.invoke();			
			// EXIT LOG
			if(check(info)) {
				base.exit(retval);
			}
			return retval;
		} catch (Throwable t) {
			// ERROR LOG
			base.error(t, "Error in method %s", info.targetMethodName);
			throw new WrapperException(t);
		}
	}

	private boolean check(ProxyTargetInfo info) {
		// filter out Lombok generated
		if(info.targetMethodName.equals("toString") || 
				info.targetMethodName.equals("equals") ||
				info.targetMethodName.equals("hashCode") ||
				info.targetMethodName.equals("canEqual"))
			return false;
		return true;
	}

}
