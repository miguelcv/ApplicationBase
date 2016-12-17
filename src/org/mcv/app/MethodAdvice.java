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

	/*
	private void errorLog(Base base, ProxyTargetInfo info, Throwable t) {
		LogEntry log = new LogEntry();
		StackTraceElement caller = Thread.currentThread().getStackTrace()[4];
		log.setCaller(caller.getMethodName());
		log.setCallerClass(caller.getClassName());
		log.setCallerLine(caller.getLineNumber());
		log.setKind(Kind.ERROR);
		log.setMethod(info.targetMethodName);
		log.setObject1(t.toString());
		log.setObject2(t.getStackTrace());
		log.setThread(Thread.currentThread().getName());
		log.setTimestamp(LocalDateTime.now());
		log.setName(base.getName());
		log.setClazz(base.getClazz().getCanonicalName());
		log.setMessage("Method error");
		base.log(log);
	}
	
	private void exitLog(Base base, ProxyTargetInfo info, Object retval) {
		LogEntry log = new LogEntry();
		StackTraceElement caller = Thread.currentThread().getStackTrace()[4];
		log.setCaller(caller.getMethodName());
		log.setCallerClass(caller.getClassName());
		log.setCallerLine(caller.getLineNumber());
		log.setKind(Kind.EXIT);
		log.setMethod(info.targetMethodName);
		log.setObject1(retval);
		log.setObject2(null);
		log.setThread(Thread.currentThread().getName());
		log.setTimestamp(LocalDateTime.now());
		log.setName(base.getName());
		log.setClazz(base.getClazz().getCanonicalName());
		log.setMessage("Method exit");
		base.log(log);
	}

	private void entryLog(Base base, ProxyTargetInfo info) {
		LogEntry log = new LogEntry();
		StackTraceElement caller = Thread.currentThread().getStackTrace()[4];
		log.setCaller(caller.getMethodName());
		log.setCallerClass(caller.getClassName());
		log.setCallerLine(caller.getLineNumber());
		log.setKind(Kind.ENTRY);
		log.setMethod(info.targetMethodSignature);
		List<Object> args = new ArrayList<>();
		for (int i = 0; i < info.argumentCount; i++) {
			args.add(info.arguments[i]);
		}
		log.setObject1(args);
		log.setObject2(null);
		log.setThread(Thread.currentThread().getName());
		log.setTimestamp(LocalDateTime.now());
		log.setName(base.getName());
		log.setClazz(base.getClazz().getCanonicalName());
		log.setMessage("Method entry");
		base.log(log);
	}
	*/
}
