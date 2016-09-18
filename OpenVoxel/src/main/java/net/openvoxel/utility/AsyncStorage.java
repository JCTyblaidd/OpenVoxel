package net.openvoxel.utility;

import com.lmax.disruptor.*;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Created by James on 15/09/2016.
 *
 * Asynchronous Storage of data from 1 thread for another to handle later
 */
public class AsyncStorage<ID,T> {

	private static class StorageRef<ID,T> {
		ID bonusRef = null;
		T reference = null;
	}

	RingBuffer<StorageRef> ringBuffer;
	Sequence sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);
	SequenceBarrier barrier;
	int size;

	public AsyncStorage(int size) {
		this.size = size;
		ringBuffer = RingBuffer.createMultiProducer(StorageRef::new,size,new PhasedBackoffWaitStrategy(0,100, TimeUnit.NANOSECONDS,new BlockingWaitStrategy()));
		barrier = ringBuffer.newBarrier();
	}

	private EventTranslatorTwoArg<StorageRef,ID,T> TRANSLATOR = (event, sequence1, idRef, objRef) -> {
		event.bonusRef = idRef;
		event.reference = objRef;
	};

	/**
	 * Asynchronous Storing of Data, context info + object_info
	 * @param t
	 */
	public void store(ID id,T t) {
		ringBuffer.publishEvent(TRANSLATOR,id,t);
	}


	/**
	 * For Iteratively Handling all data in 1 swoop
	 * @param consumer
	 */
	public void forEachHandle(BiConsumer<ID,T> consumer) {
		/**
		long nextSequence = sequence.get() + 1L;
		long writeSequence = ringBuffer.getCursor();
		long difference = (writeSequence - nextSequence) % size;
		try{
			barrier.waitFor(nextSequence);
		}catch (Exception e) {

		}
		StorageRef<ID,T> ref;
		for(int i = 0; i <= difference; i++) {
			ref = ringBuffer.get(nextSequence+i);
			consumer.accept(ref.bonusRef,ref.reference);
		}
		try {
			sequence.set(writeSequence);
			barrier.waitFor(writeSequence);
		}catch(Exception e) {
			e.printStackTrace();
		}
		 **/
		StorageRef<ID,T> ref;
		long nextSequence, writeSequence;
		while(true) {
			nextSequence = sequence.get() + 1L;
			writeSequence = ringBuffer.getCursor();
			if(nextSequence == writeSequence) {
				break;
			}
			try {
				sequence.set(nextSequence);
				barrier.waitFor(nextSequence);
			} catch (Exception e) {}
			ref = ringBuffer.get(nextSequence);
			consumer.accept(ref.bonusRef,ref.reference);
		}
	}

	private void test() {
		for(int i = 0; i < 500; i++) {
			this.store((ID)(Integer)i,(T)(Long)(long)i);
		}
	}

	public static void main(String[] args) {
		//TEST://
		AsyncStorage<Integer,Long> storage = new AsyncStorage<>(32);
		Thread t = new Thread(storage::test);
		t.start();
		for(int i = 0; i < 50; i++) {
			storage.forEachHandle((a, b) -> System.out.println(a));
		}
		System.out.print("END");
	}
}
