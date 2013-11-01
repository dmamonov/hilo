package org.hilo.core;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Optional.fromNullable;

/**
 * @author dmitry.mamonov
 *         Created: 11/1/13 1:34 PM
 */
public class GuiceDemo {
    public static void main(final String[] args) {
        final AtomicReference<String> dummy = new AtomicReference<>(null);
        final Injector injector = Guice.createInjector(new Module() {
            @Override
            public void configure(final Binder binder) {
                binder.bind(String.class).toProvider(new Provider<String>() {
                    @Override
                    public String get() {
                        return dummy.get();
                    }
                });
            }
        });
        System.out.println(injector.getInstance(DemoBean.class));
        dummy.set("Real Value");
        System.out.println(injector.getInstance(DemoBean.class));
    }

    public static class DemoBean {
        @Inject
        @Nullable
        protected String title = "Default title";

        protected String description;

        @Inject
        protected void setDescription(final @Nullable String description) {
            this.description = fromNullable(description).or("blank");
        }

        @Override
        public String toString() {
            return "DemoBean{" +
                    "title='" + title + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        }
    }
}
