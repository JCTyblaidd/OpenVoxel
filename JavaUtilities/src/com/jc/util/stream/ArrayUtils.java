package com.jc.util.stream;

import java.util.Iterator;

/**
 * Created by James on 05/08/2016.
 */
public class ArrayUtils {

	public static Iterable<Boolean> Iterate(boolean[] arr) {
		return new Iterable<Boolean>() {
			private int loc = -1;
			@Override
			public Iterator<Boolean> iterator() {
				return new Iterator<Boolean>() {
					@Override
					public boolean hasNext() {
						return (loc + 1) != arr.length;
					}

					@Override
					public Boolean next() {
						return arr[loc++];
					}
				};
			}
		};
	}

	public static Iterable<Byte> Iterate(byte[] arr) {
		return new Iterable<Byte>() {
			private int loc = -1;
			@Override
			public Iterator<Byte> iterator() {
				return new Iterator<Byte>() {
					@Override
					public boolean hasNext() {
						return (loc + 1) != arr.length;
					}

					@Override
					public Byte next() {
						return arr[loc++];
					}
				};
			}
		};
	}

	public static Iterable<Character> Iterate(char[] arr) {
		return new Iterable<Character>() {
			private int loc = -1;
			@Override
			public Iterator<Character> iterator() {
				return new Iterator<Character>() {
					@Override
					public boolean hasNext() {
						return (loc + 1) != arr.length;
					}

					@Override
					public Character next() {
						return arr[loc++];
					}
				};
			}
		};
	}

	public static Iterable<Short> Iterate(short[] arr) {
		return new Iterable<Short>() {
			private int loc = -1;
			@Override
			public Iterator<Short> iterator() {
				return new Iterator<Short>() {
					@Override
					public boolean hasNext() {
						return (loc + 1) != arr.length;
					}

					@Override
					public Short next() {
						return arr[loc++];
					}
				};
			}
		};
	}

	public static Iterable<Integer> Iterate(int[] arr) {
		return new Iterable<Integer>() {
			private int loc = -1;
			@Override
			public Iterator<Integer> iterator() {
				return new Iterator<Integer>() {
					@Override
					public boolean hasNext() {
						return (loc + 1) != arr.length;
					}

					@Override
					public Integer next() {
						return arr[loc++];
					}
				};
			}
		};
	}

	public static Iterable<Long> Iterate(long[] arr) {
		return new Iterable<Long>() {
			private int loc = -1;
			@Override
			public Iterator<Long> iterator() {
				return new Iterator<Long>() {
					@Override
					public boolean hasNext() {
						return (loc + 1) != arr.length;
					}

					@Override
					public Long next() {
						return arr[loc++];
					}
				};
			}
		};
	}

	public static Iterable<Float> Iterate(float[] arr) {
		return new Iterable<Float>() {
			private int loc = -1;
			@Override
			public Iterator<Float> iterator() {
				return new Iterator<Float>() {
					@Override
					public boolean hasNext() {
						return (loc + 1) != arr.length;
					}

					@Override
					public Float next() {
						return arr[loc++];
					}
				};
			}
		};
	}

	public static Iterable<Double> Iterate(double[] arr) {
		return new Iterable<Double>() {
			private int loc = -1;
			@Override
			public Iterator<Double> iterator() {
				return new Iterator<Double>() {
					@Override
					public boolean hasNext() {
						return (loc + 1) != arr.length;
					}

					@Override
					public Double next() {
						return arr[loc++];
					}
				};
			}
		};
	}

	public static Iterable<Character> Iterate(String str) {
		return new Iterable<Character>() {
			private int loc = -1;
			@Override
			public Iterator<Character> iterator() {
				return new Iterator<Character>() {
					@Override
					public boolean hasNext() {
						return (loc + 1) != str.length();
					}

					@Override
					public Character next() {
						return str.charAt(loc++);
					}
				};
			}
		};
	}

	public static <T> Iterable<T> Iterate(T[] arr) {
		return new Iterable<T>() {
			private int loc = -1;
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					@Override
					public boolean hasNext() {
						return (loc + 1) != arr.length;
					}

					@Override
					public T next() {
						return arr[loc++];
					}
				};
			}
		};
	}


}