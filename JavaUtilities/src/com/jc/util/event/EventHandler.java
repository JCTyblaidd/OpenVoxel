package com.jc.util.event;

import com.jc.util.core.Dual;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventHandler {
	
	private Map<Class<? extends BaseEvent>,Dual<List<Dual<EventListener,EventWrappedFunction<EventListener,BaseEvent>>>,List<EventFunctionHandle<BaseEvent>>>> Data;
	
	public EventHandler() {
		Data = new HashMap<>();
	}
	
	public void callEvent(BaseEvent e) {
		Dual<List<Dual<EventListener,EventWrappedFunction<EventListener,BaseEvent>>>,List<EventFunctionHandle<BaseEvent>>> handle = aquireForClass(e.getClass());
		handle.val1.forEach(eDual -> {
			eDual.val2.push(eDual.val1, e);
		});
		handle.val2.forEach(eHandle -> {
			eHandle.acceptEvent(e);
		});
	}
	
	private Dual<List<Dual<EventListener,EventWrappedFunction<EventListener,BaseEvent>>>,List<EventFunctionHandle<BaseEvent>>> aquireForClass(Class<? extends BaseEvent> clazz) {
		Dual<List<Dual<EventListener,EventWrappedFunction<EventListener,BaseEvent>>>,List<EventFunctionHandle<BaseEvent>>> obj = Data.get(clazz);
		if(obj == null) {
			obj = new Dual<>();
			obj.val1 = new ArrayList<>();
			obj.val2 = new ArrayList<>();
			Data.put(clazz, obj);
		}
		return obj;
	}
	
	
	public void registerForEvents(EventListener listener) {
		Class<?> clz = listener.getClass();
		for(Method m : clz.getMethods()) {
			if(m.getAnnotationsByType(EventSubscriber.class) != null) {
				//Handles Event//
				if(m.getParameterTypes().length == 0) {
					Class<?> type = m.getParameterTypes()[0].getClass();
					if(BaseEvent.class.isAssignableFrom(type)) {
						
					}else {
						throw new RuntimeException("Parameter in eventFunction invalid!");
					}
				}else {
					throw new RuntimeException("Error registering for events: invaid param count!");
				}
			}
		}
	}
	
	
}
