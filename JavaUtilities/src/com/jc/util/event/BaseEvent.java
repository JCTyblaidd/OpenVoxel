package com.jc.util.event;

public abstract class BaseEvent {
	
	public abstract boolean canCancel();
	public abstract void setCancelled(boolean state);
	
//	public abstract void stopPropegation();//Stop other handlers getting the event/
}
