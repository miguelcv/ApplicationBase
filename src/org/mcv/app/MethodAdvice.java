package org.mcv.app;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.mcv.app.LogEntry.Kind;

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
			entryLog(base, info);
			// INVOKE
			Object retval = ProxyTarget.invoke();			
			// EXIT LOG
			exitLog(base, info, retval);
			return retval;
		} catch (Throwable t) {
			// ERROR LOG
			Throwable e = WrapperException.unwrap(t);
			errorLog(base, info, e);
			throw new WrapperException(e);
		}
	}

	private void errorLog(Base base, ProxyTargetInfo info, Throwable t) {
		LogEntry log = new LogEntry();
		/* 0 = Thread.getStackTrace */
		/* 1 = Proxetta.log$0 */
		/* 2 = Proxetta.setXXX$0 */
		/* 3 = Proxetta.setXXX */
		/* 4 = actual caller */
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
		/* 0 = Thread.getStackTrace */
		/* 1 = Proxetta.log$0 */
		/* 2 = Proxetta.setXXX$0 */
		/* 3 = Proxetta.setXXX */
		/* 4 = actual caller */
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
		/* 0 = Thread.getStackTrace */
		/* 1 = Proxetta.log$0 */
		/* 2 = Proxetta.setXXX$0 */
		/* 3 = Proxetta.setXXX */
		/* 4 = actual caller */
		StackTraceElement caller = Thread.currentThread().getStackTrace()[4];
		log.setCaller(caller.getMethodName());
		log.setCallerClass(caller.getClassName());
		log.setCallerLine(caller.getLineNumber());
		log.setKind(Kind.ENTRY);
		log.setMethod(info.targetMethodName);
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
}
