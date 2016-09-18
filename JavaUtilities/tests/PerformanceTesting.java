import com.jc.util.type.Primitive;
import com.jc.util.type.PrimitiveList;

import java.nio.LongBuffer;
import java.util.ArrayList;

/**
 * Created by James on 29/08/2016.
 */
public class PerformanceTesting {

	final static int TEST_SIZE = 5000;

	public static void main(String[] args) {
		test();
	}

	private static long val = 0;

	private static void startTimer() {
		val = System.nanoTime();
	}
	private static void stopTimer() {
		long v2 = System.nanoTime();
		long vDiff = v2 - val;
		System.out.println("Measurement: " + vDiff);
	}

	public static void test() {
		//Test long[]//
		startTimer();
		long[] arr = new long[TEST_SIZE];
		for(int i = 0; i < TEST_SIZE; i++) {
			arr[i] = (i % 256);
		}
		for(int i = 0; i < TEST_SIZE; i++) {
			arr[i] = (arr[i] * 20);
		}
		stopTimer();

		//Test ByteBuffer//
		startTimer();
		LongBuffer buff = LongBuffer.allocate(TEST_SIZE);
		for(int i = 0; i < TEST_SIZE; i++) {
			buff.put(i,(i % 256));
		}
		for(int i = 0; i < TEST_SIZE; i++) {
			buff.put(i,buff.get(i) * 20);
		}
		stopTimer();

		//Test PrimitiveArray//
		startTimer();
		PrimitiveList<Long> primList = new PrimitiveList<>(Primitive.LONG,TEST_SIZE);
		for(int i = 0; i < TEST_SIZE; i++) {
			//primList.set(i,(byte)(i % 256));
			primList.add((long)i % 256);
		}
		for(int i = 0; i < TEST_SIZE; i++) {
			primList.set(i,(primList.get(i) * 20));
		}
		stopTimer();

		//Test ArrayList//
		startTimer();
		ArrayList<Long> arrayList = new ArrayList<>(TEST_SIZE);
		for(int i = 0; i < TEST_SIZE; i++) {
			//primList.set(i,(byte)(i % 256));
			primList.add((long)i % 256);
		}
		for(int i = 0; i < TEST_SIZE; i++) {
			primList.set(i,(primList.get(i) * 20));
		}
		stopTimer();
	}

}
