package net.openvoxel.utility;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;

/**
 * Created by James on 15/04/2017.
 *
 * Asynchronous Queue from thread to thread : Single Input
 */
public class AsyncQueue<T> {
	private class ObjectRef {
		T obj;
	}
	private RingBuffer<ObjectRef> buffer;

	private EventTranslatorOneArg<ObjectRef,T> TRANSLATE = (event, sequence, type) -> event.obj = type;
	private SequenceBarrier barrier;

	public AsyncQueue(int size) {
		buffer = RingBuffer.createSingleProducer(ObjectRef::new,size,new BlockingWaitStrategy());
		barrier = buffer.newBarrier();
	}

	/**
	 * Add an object to the asynchronous queue
	 */
	public void add(T t) {
		buffer.publishEvent(TRANSLATE,t);
	}

	/**
	 * Check if the queue is currently empty
	 */
	public void isEmpty() {

	}

	public T attemptNext() {
		return null;
	}

	public T awaitNext() {
		return null;
	}
}
