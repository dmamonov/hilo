package org.hilo.core.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author dmitry.mamonov
 *         Created: 11/2/13 1:17 AM
 */
public class ListProxy {
    public static <T> T allVoid(final Class<T> clazz, final Iterable<?> list) {
        return clazz.cast(Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new InvocationHandler() {
            @Override
            public Object invoke(final Object ignoreSelf, final Method method, final Object[] args) throws Throwable {
                checkState(method.getReturnType() == Void.TYPE);
                for (final Object entry : list) {
                    if (clazz.isInstance(entry)) {
                        method.invoke(entry, args);
                    }
                }
                return null;
            }
        }));
    }

    public static <T> T firstSuccess(final Class<T> clazz, final Iterable<?> list) {
        return clazz.cast(Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new InvocationHandler() {
            @Override
            public Object invoke(final Object ignoreSelf, final Method method, final Object[] args) throws Throwable {
                checkState(method.getReturnType()==Boolean.TYPE);
                for (final Object entry : list) {
                    if (clazz.isInstance(entry)) {
                        final Object result = method.invoke(entry, args);
                        if (Boolean.TRUE.equals(result)) {
                            return result;
                        }
                    }
                }
                return Boolean.FALSE;
            }
        }));
    }

    public static <T> T lastSuccess(final Class<T> clazz, final Iterable<?> list) {
        return clazz.cast(Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new InvocationHandler() {
            @Override
            public Object invoke(final Object ignoreSelf, final Method method, final Object[] args) throws Throwable {
                checkState(method.getReturnType()==Boolean.TYPE);
                Boolean lastSuccess = Boolean.FALSE;
                for (final Object entry : list) {
                    if (clazz.isInstance(entry)) {
                        final Object result = method.invoke(entry, args);
                        if (Boolean.TRUE.equals(result)) {
                            lastSuccess = Boolean.TRUE;
                        }
                    }
                }
                return lastSuccess;
            }
        }));
    }
}
