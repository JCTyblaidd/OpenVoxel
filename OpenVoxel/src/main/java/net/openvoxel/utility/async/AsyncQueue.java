package net.openvoxel.utility.async;

import com.lmax.disruptor.*;
import net.openvoxel.api.PublicAPI;

/**
 * Created by James on 15/04/2017.
 *
 * Asynchronous Queue from thread to thread : Single Input, Single Output
 */
public class AsyncQueue<T> {
	private class ObjectRef {
		T obj;
	}
	private RingBuffer<ObjectRef> buffer;
	private EventTranslatorOneArg<ObjectRef,T> TRANSLATE = (event, sequence, type) -> event.obj = type;
	private SequenceBarrier barrier;
	private Sequence sequence;

	/**
	 * Initialize The Queue
	 */
	@PublicAPI
	public AsyncQueue(int size) {
		this(size,false);
	}

	@PublicAPI
	public AsyncQueue(int size,boolean multiProducer) {
		if(multiProducer) {
			buffer = RingBuffer.createMultiProducer(ObjectRef::new,size,new BlockingWaitStrategy());
		}else{
			buffer = RingBuffer.createSingleProducer(ObjectRef::new,size,new BlockingWaitStrategy());
		}
		barrier = buffer.newBarrier();
		sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);
	}

	/**
	 * Add an object to the asynchronous queue
	 */
	@PublicAPI
	public void add(T t) {
		buffer.publishEvent(TRANSLATE,t);
	}

	/**
	 * @return the current size on request [no sync-guarantees]
	 */
	@PublicAPI
	public long snapshotSize() {
		long writeHead = buffer.getCursor();
		long readHead = sequence.get();
		return writeHead - readHead;
	}

	/**
	 * Check if the queue is currently empty
	 */
	@PublicAPI
	public boolean isEmpty() {
		long writeHead = buffer.getCursor();
		long readHead = sequence.get();
		return writeHead <= readHead;
	}

	/**
	 * Get and Object or Null From The Queue
	 */
	@PublicAPI
	public T attemptNext() {
		long nextSequence, writeSequence, currentSequence;
		currentSequence = sequence.get();
		nextSequence = currentSequence + 1L;
		writeSequence = buffer.getCursor();
		if(nextSequence > writeSequence) return null;
		if(!sequence.compareAndSet(currentSequence,nextSequence)) return null;
		return buffer.get(nextSequence).obj;
	}

	/**
	 * Wait For An Object From The Queue
	 */
	@PublicAPI
	public T awaitNext() throws InterruptedException{
		long nextSequence, writeSequence, currentSequence;
		while(true) {
			currentSequence = sequence.get();
			nextSequence = currentSequence + 1L;
			writeSequence = buffer.getCursor();
			if(nextSequence > writeSequence) {
				try{
					barrier.waitFor(nextSequence);
				}catch(InterruptedException interrupt) {
					throw interrupt;
				}catch(Exception ignored) {
					//NO OP
				}
			}else if(sequence.compareAndSet(currentSequence,nextSequence)) {
				return buffer.get(nextSequence).obj;
			}
		}
	}
}
