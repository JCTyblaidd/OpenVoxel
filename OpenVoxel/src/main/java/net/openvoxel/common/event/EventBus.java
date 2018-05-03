package net.openvoxel.common.event;


import net.openvoxel.api.logger.Logger;
import net.openvoxel.utility.CrashReport;
import net.openvoxel.utility.debug.Validate;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by James on 31/07/2016.
 *
 * Event Calling Bus
 */
public class EventBus {

	private Map<Class<? extends AbstractEvent>,Set<WrappedHandler>> HandlerList;

	private static Logger eventBusLogger;
	static {
		eventBusLogger = Logger.getLogger("Event Bus");
	}

	public EventBus() {
		HandlerList = new HashMap<>();
	}

	public void push(AbstractEvent event) {
		Validate.NotNull(event);
		Class clazz = event.getClass();
		while(AbstractEvent.class.isAssignableFrom(clazz)) {
			_push(event,clazz);
			clazz = clazz.getSuperclass();
		}
	}
	private void _push(AbstractEvent event, Class<? extends AbstractEvent> clazz) {
		if(HandlerList.containsKey(clazz)) {
			HandlerList.get(clazz).forEach(handler -> handler.push(event));
		}
	}

	@SuppressWarnings({"unchecked"})
	public void register(EventListener listener) {
		Validate.NotNull(listener);
		Method[] methods = listener.getClass().getMethods();
		for(Method m : methods) {
			SubscribeEvents annotation = m.getAnnotation(SubscribeEvents.class);
			if(annotation != null) {
				//Valid Handle//
				EventOrdering ordering = annotation.value();
				WrappedHandler handler = new WrappedHandler(listener,m,ordering);
				Class<?>[] params = m.getParameterTypes();
				if(params.length == 1 && AbstractEvent.class.isAssignableFrom(params[0]) && m.getReturnType() == void.class) {
					//Valid! accept and add
					Class<? extends AbstractEvent> clazzType = (Class<? extends AbstractEvent>)params[0];
					if(!HandlerList.containsKey(clazzType)) {
						HandlerList.put(clazzType,new HashSet<>());
					}
					HandlerList.get(clazzType).add(handler);
				}else{
					eventBusLogger.Warning("Method with @SubscribeEvents that doesn't fit the required signature!");
				}
			}
		}
	}
	public void unregister(EventListener listener,Class<? extends AbstractEvent> type) {
		HandlerList.get(type).remove(listener);
	}
	public void unregisterAll(EventListener listener) {
		HandlerList.values().forEach(handler -> handler.remove(listener));
	}

	private static class WrappedHandler {
		EventListener listener;
		Method method;
		EventOrdering order;

		private WrappedHandler(EventListener listener, Method method, EventOrdering order) {
			this.listener = listener;
			this.method = method;
			this.order = order;
			method.setAccessible(true);
		}

		void push(AbstractEvent event) {
			try {
				method.invoke(listener,event);
			}catch(Exception e) {
				String eventName = event == null ? "null" : event.getClass().getSimpleName();
				CrashReport crash = new CrashReport("Error calling event: "+eventName);
				crash.caughtException(e);
				crash.getThrowable().printStackTrace();
			}
		}

		@Override
		public int hashCode() {//TODO: double-check that the ordering is correct
			return (order.ordinal() << 24) | (listener.hashCode() & 0xFFFFFF);
		}

		@Override
		public boolean equals(Object obj) {
			return listener.equals(obj);
		}
	}

}
