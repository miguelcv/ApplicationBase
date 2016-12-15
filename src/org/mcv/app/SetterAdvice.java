package org.mcv.app;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import org.mcv.app.LogEntry.Kind;

import jodd.proxetta.ProxyAdvice;
import jodd.proxetta.ProxyTarget;
import jodd.proxetta.ProxyTargetInfo;

/**
 * @author Miguelc
 *
 */
public class SetterAdvice implements ProxyAdvice {

	/* 
	 * Log setter call, auto-persist.
	 */
	@Override
	public Object execute() throws Exception {

		ProxyTargetInfo info = ProxyTarget.info();
		Base base = (Base) ProxyTarget.target();
		if(!base.isCurrent()) {
			base.warn("No changes allowed: object is not current", null);
			return null;
		}
		if(base.isDeleted()) {
			base.warn("No changes allowed: object is deleted", null);
			return null;
		}

		// GET OLD VALUE
		Object oldValue = getOldValue(base, info);
		
		// LOGGING
		log(base, info, oldValue);

		// INVOKE
		ProxyTarget.invoke();

		// AUTO-STORE
		if (!base.isInBatch()) {
			base.getApp().store(base);
		}

		return null;
	}

	private void log(Base base, ProxyTargetInfo info, Object oldValue) {
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
		log.setKind(Kind.SETTER);
		log.setMethod(info.targetMethodName);
		log.setObject1(oldValue);
		Object newValue = info.arguments[0];
		log.setObject2(newValue);
		log.setThread(Thread.currentThread().getName());
		log.setTimestamp(LocalDateTime.now());
		log.setName(base.getName());
		log.setClazz(base.getClazz().getCanonicalName());
		if (base.getApp() != null) {
			base.getApp().storeLogEntry(log);
		}
	}

	private Object getOldValue(Base base, ProxyTargetInfo info) {
		try {
			Method getterMethod = null;
			String getter = "get" + info.targetMethodName.substring(3);
			try {
				getterMethod = ((Class<?>) info.targetClass).getMethod(getter);
			} catch (NoSuchMethodException e) {
				getter = "is" + info.targetMethodName.substring(2);
				getterMethod = ((Class<?>) info.targetClass).getMethod(getter);
			}
			return getterMethod.invoke(base);
		} catch (Exception e) {
			return null;
		}
	}
}
