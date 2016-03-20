package com.bhupendra.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.bhupendra.aop.anno.After;
import com.bhupendra.aop.anno.Before;
import com.bhupendra.aop.anno.Error;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

public class Factory {

	public static Person createPerson() throws Exception {
		ProxyFactory factory = getProxyFactory(Person.class);
		Person person = (Person) factory.create(new Class[0], new Object[0], getDefaultHandler());
		return person;
	}

	private static MethodHandler getDefaultHandler() {
		return new MethodHandler() {
			public Object invoke(Object self, Method method, Method proceed, Object[] args) throws Throwable {
				try {
					handleBeforPointCut(self, method, args);
					Object object = proceed.invoke(self, args);
					handleAfterPointCut(self, method, args);
					return object;
				} catch (InvocationTargetException e) {
					handleErrorPointCut(self, method, args);
					throw e.getTargetException();
				}
			}
		};
	}

	private static void handleBeforPointCut(Object self, Method method, Object[] args) throws Exception {
		Before before = method.getAnnotation(Before.class);
		if (before != null) {
			String methodName = before.method();
			Class<?> type = before.type();
			int[] seq = before.argSequence();
			executePointCut(self, args, methodName, type, seq);
		}
	}

	private static void handleAfterPointCut(Object self, Method method, Object[] args) throws Exception {
		After before = method.getAnnotation(After.class);
		if (before != null) {
			String methodName = before.method();
			Class<?> type = before.type();
			int[] seq = before.argSequence();
			executePointCut(self, args, methodName, type, seq);
		}
	}

	private static void handleErrorPointCut(Object self, Method method, Object[] args) throws Exception {
		Error before = method.getAnnotation(Error.class);
		if (before != null) {
			String methodName = before.method();
			Class<?> type = before.type();
			int[] seq = before.argSequence();
			executePointCut(self, args, methodName, type, seq);
		}
	}

	private static void executePointCut(Object self, Object[] args, String methodName, Class<?> type, int[] seq)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Object[] methodArgs = null;
		Method callThis = null;
		if (seq.length > 0) {
			Class<?>[] methodArgTypes = new Class[seq.length + 1];
			methodArgs = new Object[seq.length + 1];
			methodArgTypes[0] = getClassType(self);
			methodArgs[0] = self;
			for (int i = 1; i <= seq.length; i++) {
				methodArgs[i] = args[seq[i - 1]];
				methodArgTypes[i] = getClassType(args[seq[i - 1]]);
			}
			callThis = type.getDeclaredMethod(methodName, methodArgTypes);
		} else {
			callThis = type.getDeclaredMethod(methodName, getClassType(self));
			methodArgs = new Object[1];
			methodArgs[0] = self;
		}
		callThis.setAccessible(true);
		callThis.invoke(null, methodArgs);
	}

	private static Class<?> getClassType(Object object) {
		return ProxyFactory.isProxyClass(object.getClass()) ? object.getClass().getSuperclass() : object.getClass();
	}

	private static ProxyFactory getProxyFactory(Class<?> type) {

		ProxyFactory factory = new ProxyFactory();
		factory.setSuperclass(type);
		factory.setFilter(new MethodFilter() {
			public boolean isHandled(Method method) {
				Annotation[] annotations = method.getDeclaredAnnotations();
				for (Annotation annotation : annotations) {
					if (annotation instanceof Before || annotation instanceof After || annotation instanceof Error) {
						return true;
					}
				}
				return false;
			}
		});
		return factory;
	}
}
