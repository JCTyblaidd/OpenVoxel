package net.openvoxel.api.logger;

import io.netty.util.internal.logging.AbstractInternalLogger;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Created by James on 01/08/2016.
 *
 * Wrapper to convert Netty Logger To Use MyLogging Framework
 */
public class NettyLogWrapper extends InternalLoggerFactory{

	private static Logger nettyLogger = Logger.getLogger("netty");

	public static void Load() {
		InternalLoggerFactory.setDefaultFactory(new NettyLogWrapper());
	}

	@Override
	protected InternalLogger newInstance(String name) {
		return new NettyLogInternalLogger();
	}

	private static class NettyLogInternalLogger extends AbstractInternalLogger {

		private String _format(String str,Object... args) {
			String v = str;
			for(Object arg : args) {
				int index = v.indexOf("{}");
				if(index != -1) {
					String pre = v.substring(0,index);
					String post = v.substring(index + 2,v.length());
					v = pre + arg.toString() + post;
				}else {
					v = v + " " + arg.toString();
				}
			}
			return v;
		}

		/**
		 * Creates a new instance.
		 **/
		protected NettyLogInternalLogger() {
			super("netty internal");
		}

		@Override
		public boolean isTraceEnabled() {
			return false;
		}


		@Override
		public void trace(String msg) {
			nettyLogger.Trace(msg);
		}

		@Override
		public void trace(String format, Object arg) {
			trace(_format(format,arg));
		}

		@Override
		public void trace(String format, Object argA, Object argB) {
			trace(_format(format,argA,argB));
		}

		@Override
		public void trace(String format, Object... arguments) {
			trace(_format(format,(Object[])arguments));
		}

		@Override
		public void trace(String msg, Throwable t) {
			trace(msg);
			nettyLogger.StackTrace(t);
		}

		@Override
		public boolean isDebugEnabled() {
			return nettyLogger.isEnabled(Logger.LogLevel.DEBUG);
		}

		@Override
		public void debug(String msg) {
			nettyLogger.Debug(msg);
		}

		@Override
		public void debug(String format, Object arg) {
			debug(_format(format,arg));
		}

		@Override
		public void debug(String format, Object argA, Object argB) {
			debug(_format(format,argA,argB));
		}

		@Override
		public void debug(String format, Object... arguments) {
			debug(_format(format,(Object[])arguments));
		}

		@Override
		public void debug(String msg, Throwable t) {
			debug(msg);
			nettyLogger.StackTrace(t);
		}

		@Override
		public boolean isInfoEnabled() {
			return nettyLogger.isEnabled(Logger.LogLevel.INFO);
		}

		@Override
		public void info(String msg) {
			nettyLogger.Info(msg);
		}

		@Override
		public void info(String format, Object arg) {
			info(_format(format,arg));
		}

		@Override
		public void info(String format, Object argA, Object argB) {
			info(_format(format,argA,argB));
		}

		@Override
		public void info(String format, Object... arguments) {
			info(_format(format,(Object[])arguments));
		}

		@Override
		public void info(String msg, Throwable t) {
			info(msg);
			nettyLogger.StackTrace(t);
		}

		@Override
		public boolean isWarnEnabled() {
			return nettyLogger.isEnabled(Logger.LogLevel.WARNING);
		}

		@Override
		public void warn(String msg) {
			nettyLogger.Warning(msg);
		}

		@Override
		public void warn(String format, Object arg) {
			warn(_format(format,arg));
		}

		@Override
		public void warn(String format, Object... arguments) {
			warn(_format(format,(Object[])arguments));
		}

		@Override
		public void warn(String format, Object argA, Object argB) {
			warn(_format(format,argA,argB));
		}

		@Override
		public void warn(String msg, Throwable t) {
			warn(msg);
			nettyLogger.StackTrace(t);
		}

		@Override
		public boolean isErrorEnabled() {
			return nettyLogger.isEnabled(Logger.LogLevel.SEVERE);
		}

		@Override
		public void error(String msg) {
			nettyLogger.Severe(msg);
		}

		@Override
		public void error(String format, Object arg) {
			error(_format(format,arg));
		}

		@Override
		public void error(String format, Object argA, Object argB) {
			error(_format(format,argA,argB));
		}

		@Override
		public void error(String format, Object... arguments) {
			error(_format(format,(Object[])arguments));
		}

		@Override
		public void error(String msg, Throwable t) {
			error(msg);
			nettyLogger.StackTrace(t);
		}
	}
}
