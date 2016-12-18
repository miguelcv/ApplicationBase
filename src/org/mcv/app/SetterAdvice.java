package org.mcv.app;

import java.lang.reflect.Method;
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
public class SetterAdvice implements ProxyAdvice {

	/* 
	 * Log setter call, auto-persist.
	 */
	@Override
	public Object execute() throws Exception {

		ProxyTargetInfo info = ProxyTarget.info();
		Base base = (Base) ProxyTarget.target();
		if(!base.isCurrent()) {
			base.warn("No changes allowed: object %s is not current", base.getName());
			return null;
		}
		if(base.isDeleted()) {
			base.warn("No changes allowed: object %s is deleted", base.getName());
			return null;
		}

		// GET OLD VALUE
		Object oldValue = getOldValue(base, info);
		base.debug("oldvalue = " + oldValue);
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
		
		StackTraceElement[] elts = Thread.currentThread().getStackTrace();
		int[] indices = SteUtils.findCalleeAndCaller(elts);
		StackTraceElement caller = elts[indices[1]];
		StackTraceElement callee = elts[indices[0]];
		log.setCaller(SteUtils.cleanup(caller.getMethodName()));
		log.setCallerClass(SteUtils.cleanup(caller.getClassName()));
		log.setCallerLine(caller.getLineNumber());
		log.setKind(Kind.SETTER);
		log.setMethod(SteUtils.cleanup(callee.getMethodName()));
		log.setMethodClass(SteUtils.cleanup(callee.getClassName()));
		log.setMethodLine(callee.getLineNumber());
		Object newValue = info.arguments[0];
		base.debug("newvalue = " + newValue);
		List<Object> list = new ArrayList<>();
		list.add(oldValue);
		list.add(newValue);
		log.setObjList(list);
		log.setThread(Thread.currentThread().getName());
		log.setTimestamp(LocalDateTime.now());
		log.setName(base.getName());
		log.setClazz(base.getClazz().getCanonicalName());
		base.log(log);
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
