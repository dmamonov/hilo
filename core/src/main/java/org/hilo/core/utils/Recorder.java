package org.hilo.core.utils;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.*;

/**
 * @author dmitry.mamonov
 *         Created: 11/3/13 6:38 PM
 */
public class Recorder {
    public static <R> R recorder(final Class<R> clazz, final DataOutput tape) {
        return clazz.cast(Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new InvocationHandler() {
            private final TreeMap<String, Integer> methods = new TreeMap<>();

            {
                int index = 0;
                for (final Method method : clazz.getMethods()) {
                    if (method.getReturnType() == Void.TYPE) {
                        checkState(!methods.containsKey(method.getName()));
                        methods.put(method.getName(), index++);
                    }
                }
                try {
                    tape.writeInt(methods.size());
                    for (final Map.Entry<String, Integer> entry : methods.entrySet()) {
                        tape.writeUTF(entry.getKey());
                        tape.writeInt(entry.getValue());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }

            @Override
            public Object invoke(final Object o, final Method method, final Object[] objects) throws Throwable {
                checkArgument(method.getReturnType() == Void.TYPE);
                final String methodName = method.getName();
                tape.writeInt(checkNotNull(methods.get(methodName), methodName));
                if (objects != null) {
                    for (final Object param : objects) {
                        if (param instanceof Integer) {
                            tape.writeInt((Integer) param);
                        } else if (param instanceof Float) {
                            tape.writeFloat((Float) param);
                        } else if (param instanceof Double) {
                            tape.writeDouble((Double) param);
                        } else if (param instanceof Boolean) {
                            tape.writeBoolean((Boolean) param);
                        } else if (param instanceof String) {
                            tape.writeUTF((String) param);
                        } else {
                            throw new IllegalArgumentException("Wrong argument: " + param + " of type " + (param != null ? param.getClass() : null));
                        }
                    }
                }
                return null;
            }
        }));
    }

    public static <R> void play(final Class<R> clazz, final R face, final DataInput tape) {
        try {
            final Map<String, Method> nameToMethodMap = Maps.uniqueIndex(ImmutableList.copyOf(clazz.getMethods()), new Function<Method, String>() {
                @Override
                public String apply(final Method method) {
                    return method.getName();
                }
            });
            final int methodsCount = tape.readInt();
            final Map<Integer, Method> keyToMethodMap = new HashMap<>();
            for (int i = 0; i < methodsCount; i++) {
                final String methodName = tape.readUTF();
                final int methodKey = tape.readInt();
                final Method method = checkNotNull(nameToMethodMap.get(methodName), "Method not found: %s", methodName);
                keyToMethodMap.put(methodKey, method);
            }
            while (true) {
                final Method method;
                try {
                    final int key = tape.readInt();
                    method = checkNotNull(keyToMethodMap.get(key), "Can't find method by key %s", key);
                } catch (EOFException e) {
                    return;
                }
                final Class<?>[] parameterTypes = method.getParameterTypes();
                final Object[] params = new Object[parameterTypes.length];
                int index = 0;
                for (final Class<?> paramType : parameterTypes) {
                    if (paramType == Integer.TYPE) {
                        params[index] = tape.readInt();
                    } else if (paramType == Float.TYPE) {
                        params[index] = tape.readFloat();
                    } else if (paramType == Double.TYPE) {
                        params[index] = tape.readDouble();
                    } else if (paramType == Boolean.TYPE) {
                        params[index] = tape.readBoolean();
                    } else if (paramType == String.class) {
                        params[index] = tape.readUTF();
                    } else {
                        throw new IllegalArgumentException("Wrong argument type: " + paramType + " of method " + method);
                    }
                    index++;
                }
                method.invoke(face, params);
            }
        } catch (InvocationTargetException | IllegalAccessException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    private interface TestFace {
        void newLine();

        void print(String value);

        void printPosition(int x, int y);
    }

    public static void main(final String[] args) {
        final ByteArrayOutputStream tapeBuffer = new ByteArrayOutputStream();
        final TestFace recorder = recorder(TestFace.class, new DataOutputStream(tapeBuffer));
        recorder.print("Hello at position: ");
        recorder.printPosition(100, 100);
        recorder.newLine();
        recorder.print("That's it");
        play(TestFace.class, new TestFace() {
            @Override
            public void newLine() {
                System.out.println();
            }

            @Override
            public void print(final String value) {
                System.out.print(value);
            }

            @Override
            public void printPosition(final int x, final int y) {
                System.out.print(String.format("(%d,%d)", x, y));
            }
        }, new DataInputStream(new ByteArrayInputStream(tapeBuffer.toByteArray())));
    }
}
