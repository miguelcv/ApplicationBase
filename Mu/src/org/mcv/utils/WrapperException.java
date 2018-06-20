package org.mcv.utils;

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Miguelc
 *
 */
@Slf4j
public class WrapperException extends RuntimeException {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * Stacktrace, message, cause and log.
     * 
     * @param t
     */
    public WrapperException(Throwable t) {
        super(t);
        log.error("{}", t, t);
    }
    
    /**
     * Stacktrace, message and log.
     * 
     * @param ss
     */
    public WrapperException(String... ss) {
        super(StringUtils.join(Arrays.asList(ss), "\n"));
        for(String s : ss) {
            log.error(s);
        }
    }
    
	public static Throwable unwrap(Throwable e) {
		if(e == null) return new Exception("null");
		Throwable ret = e;
		while(ret instanceof WrapperException) {
			if(ret.getCause() == null) break;
			ret = ret.getCause();
		}
		return ret;
	}

	@Override
    public String getMessage() {
		return unwrap(this).getMessage();
	}

	@Override
    public StackTraceElement[] getStackTrace() {
		return unwrap(this).getStackTrace();
	}

}
