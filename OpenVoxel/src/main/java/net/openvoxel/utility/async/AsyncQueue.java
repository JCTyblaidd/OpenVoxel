package net.openvoxel.utility.async;

import com.lmax.disruptor.*;

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
	public AsyncQueue(int size) {
		this(size,false);
	}

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
	public void add(T t) {
		buffer.publishEvent(TRANSLATE,t);
	}

	/**
	 * @return the current size on request [no sync-guarantees]
	 */
	public long snapshotSize() {
		long writeHead = buffer.getCursor();
		long readHead = sequence.get();
		return writeHead - readHead;
	}

	/**
	 * Check if the queue is currently empty
	 */
	public boolean isEmpty() {
		long writeHead = buffer.getCursor();
		long readHead = sequence.get();
		return writeHead <= readHead;
	}

	/**
	 * Get and Object or Null From The Queue
	 */
	public T attemptNext() {
		long nextSequence, writeSequence;
		nextSequence = sequence.get() + 1L;
		writeSequence = buffer.getCursor();
		if(nextSequence > writeSequence) return null;
		sequence.set(nextSequence);
		return buffer.get(nextSequence).obj;
	}

	/**
	 * Wait For An Object From The Queue
	 */
	public T awaitNext() {
		long nextSequence;
		nextSequence = sequence.get() + 1L;
		sequence.set(nextSequence);
		while(true) {
			try {
				barrier.waitFor(nextSequence);
			} catch (AlertException ignored) {
				return buffer.get(nextSequence).obj;
			} catch (TimeoutException ignored) {
				return null;
			} catch (InterruptedException ignored) {
				//NO OP//
			}
		}
	}
}
