package nl.novadoc.events;

import lombok.Getter;
import lombok.Setter;
import nl.novadoc.utils.Config;
import nl.novadoc.utils.Configuration;

import com.filenet.api.engine.EventActionHandler;
import com.filenet.api.events.ObjectChangeEvent;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.util.Id;

public class MyEventHandler extends Configuration implements EventActionHandler {

    @Getter @Setter private Config cfg;
    
	public void onEvent(ObjectChangeEvent ev, Id sub) throws EngineRuntimeException {
		try {
		    cfg = init(ev.getObjectStore(), "@myEvent");
		    log = setupLogging(cfg, "@myEvent");
			log.info("onEvent called");
			// do your stuff
			log.info("onEvent done");
		} catch (Throwable e) {
			log.error(e, e);
			throw new RuntimeException(e);
		}
	}

}
