import net.openvoxel.utility.AsyncQueue;

/**
 * Created by James on 25/08/2016.
 *
 * Test Client Rendering Code
 *
 * */
public class TestClient {


	public static void main(String[] args) {
		AsyncQueue<Integer> asyncQueue = new AsyncQueue<>(1024);
		int count = 0;
		for(int i = 0; i < 500; i++) {
			System.out.println(count + ":" + asyncQueue.snapshotSize());
			System.out.println(asyncQueue.isEmpty());
			asyncQueue.add(i);
			count++;
		}
		for(int j = 0; j < 500; j++) {
			System.out.println(count + ":" + asyncQueue.snapshotSize());
			Integer _val = asyncQueue.attemptNext();
			System.out.println("-----"+_val+":"+j);
			count--;
		}
		System.out.println(asyncQueue.isEmpty());
		//asyncQueue.awaitNext();
		System.out.println(count + ":" + asyncQueue.snapshotSize());
	}
}
