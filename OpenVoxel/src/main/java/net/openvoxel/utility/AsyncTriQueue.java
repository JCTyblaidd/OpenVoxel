package net.openvoxel.utility;

import com.lmax.disruptor.*;

/**
 * Created by James on 15/04/2017.
 *
 * Asynchronous Queue from thread to thread : Single Input, Single Output
 */
public class AsyncTriQueue<A,B,C> {
	private class TriObjectRef {
		A a;
		B b;
		C c;
	}
	private RingBuffer<TriObjectRef> buffer;
	private EventTranslatorThreeArg<TriObjectRef,A,B,C> TRANSLATE = (event, sequence, a,b,c) -> {
		event.a = a;
		event.b = b;
		event.c = c;
	};
	private SequenceBarrier barrier;
	private Sequence sequence;

	/**
	 * Initialize The Queue
	 */
	public AsyncTriQueue(int size) {
		buffer = RingBuffer.createSingleProducer(TriObjectRef::new,size,new BlockingWaitStrategy());
		barrier = buffer.newBarrier();
		sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);
	}

	/**
	 * Add an object to the asynchronous queue
	 */
	public void add(A a,B b,C c) {
		buffer.publishEvent(TRANSLATE,a,b,c);
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
	public TriObjectRef attemptNext() {
		long nextSequence, writeSequence;
		nextSequence = sequence.get() + 1L;
		writeSequence = buffer.getCursor();
		if(nextSequence == writeSequence) return null;
		sequence.set(nextSequence);
		return buffer.get(nextSequence);
	}

	/**
	 * Wait For An Object From The Queue
	 */
	public TriObjectRef awaitNext() {
		long nextSequence, writeSequence;
		nextSequence = sequence.get() + 1L;
		writeSequence = buffer.getCursor();
		if(nextSequence == writeSequence) return null;
		sequence.set(nextSequence);
		try {
			barrier.waitFor(nextSequence);
		}catch (Exception ignored) {
			//NO OP//
		}
		return buffer.get(nextSequence);
	}
}
