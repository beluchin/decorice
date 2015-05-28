package decorice;

import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.ScopeAnnotation;
import org.junit.Test;

import javax.inject.Inject;
import java.lang.annotation.Retention;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;
import static com.google.inject.Scopes.SINGLETON;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

@SuppressWarnings("unchecked")
public class DecoriceTest {

    @ScopeAnnotation @Retention(RUNTIME)
    public @interface CustomScope {}

    @BindingAnnotation @Retention(RUNTIME)
    private static @interface SomeAnnotation {}

    private static interface Foo {
        String bar();
    }

    private static class D1 implements Foo {
        private final Foo decorated;

        @Inject
        public D1(@DecoratedBy(D1.class) final Foo decorated) {
            this.decorated = decorated;
        }

        @Override
        public String bar() {
            return "D1:" + decorated.bar();
        }
    }

    private static class D2 implements Foo {
        private final Foo decorated;

        @Inject
        public D2(@DecoratedBy(D2.class) final Foo decorated) {
            this.decorated = decorated;
        }

        @Override
        public String bar() {
            return "D2:" + decorated.bar();
        }
    }

    private static class FooImpl implements Foo {
        @Override
        public String bar() {
            return "FooImpl";
        }
    }

    private static class SimpleScope implements Scope {

        private final ThreadLocal<Map<Key<?>, Object>> values
                = new ThreadLocal<>();

        public void enter() {
            checkState(values.get() == null, "A scoping block is already in progress");
            values.set(Maps.<Key<?>, Object>newHashMap());
        }

        public void exit() {
            checkState(values.get() != null, "No scoping block in progress");
            values.remove();
        }

        public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
            return new Provider<T>() {
                public T get() {
                    Map<Key<?>, Object> scopedObjects = getScopedObjectMap(key);

                    @SuppressWarnings("unchecked")
                    T current = (T) scopedObjects.get(key);
                    if (current == null && !scopedObjects.containsKey(key)) {
                        current = unscoped.get();

                        scopedObjects.put(key, current);
                    }
                    return current;
                }
            };
        }

        private <T> Map<Key<?>, Object> getScopedObjectMap(Key<T> key) {
            Map<Key<?>, Object> scopedObjects = values.get();
            if (scopedObjects == null) {
                throw new OutOfScopeException("Cannot access " + key
                        + " outside of a scoping block");
            }
            return scopedObjects;
        }

    }

    @Test
    public void bindToClass() {
        final Injector injector = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        install(new DecoratorModule() {{
                            bind(Foo.class)
                                    .to(FooImpl.class)
                                    .decoratedBy(
                                            D2.class,
                                            D1.class);
                        }});
                    }
                });
        assertThat(
                injector.getInstance(Foo.class).bar(),
                equalTo("D2:D1:FooImpl"));
    }

    @Test
    public void annotations() {
        final Injector injector = Guice.createInjector(
                new AbstractModule() {
                    @SuppressWarnings("unchecked")
                    @Override
                    protected void configure() {
                        install(new DecoratorModule() {{
                            bind(Foo.class)
                                    .annotatedWith(SomeAnnotation.class)
                                    .to(FooImpl.class)
                                    .decoratedBy(
                                            D2.class,
                                            D1.class);
                        }});

                    }
                });
        assertThat(
                injector.getInstance(Key.get(Foo.class, SomeAnnotation.class)).bar(),
                equalTo("D2:D1:FooImpl"));
    }

    @Test
    public void bindToKey() {
        final Injector injector = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Foo.class)
                                .annotatedWith(SomeAnnotation.class)
                                .to(FooImpl.class);
                        install(new DecoratorModule() {{
                            bind(Foo.class)
                                    .to(Key.get(Foo.class, SomeAnnotation.class))
                                    .decoratedBy(
                                            D2.class,
                                            D1.class);
                        }});
                    }
                });
        assertThat(
                injector.getInstance(Foo.class).bar(),
                equalTo("D2:D1:FooImpl"));
    }

    @Test
    public void asEagerSingleton() {
        final Injector injector = Guice.createInjector(
                new AbstractModule() {
                    @SuppressWarnings("unchecked")
                    @Override
                    protected void configure() {
                        install(new DecoratorModule() {{
                            bind(Foo.class)
                                    .to(FooImpl.class)
                                    .decoratedBy(
                                            D2.class,
                                            D1.class)
                                    .asEagerSingleton();
                        }});
                    }
                });

        final Foo instance1 = injector.getInstance(Foo.class);
        final Foo instance2 = injector.getInstance(Foo.class);

        assertThat(instance1, is(sameInstance(instance2)));
        assertThat(
                instance1.bar(),
                equalTo("D2:D1:FooImpl"));
    }

    @Test
    public void inScope() {
        final Injector injector = Guice.createInjector(
                new AbstractModule() {
                    @SuppressWarnings("unchecked")
                    @Override
                    protected void configure() {
                        install(new DecoratorModule() {{
                            bind(Foo.class)
                                    .to(FooImpl.class)
                                    .decoratedBy(
                                            D2.class,
                                            D1.class)
                                    .in(SINGLETON);
                        }});
                    }
                });

        final Foo instance1 = injector.getInstance(Foo.class);
        final Foo instance2 = injector.getInstance(Foo.class);

        assertThat(instance1, is(sameInstance(instance2)));
        assertThat(
                instance1.bar(),
                equalTo("D2:D1:FooImpl"));
    }

    @Test
    public void customScope() {
        final Injector injector = Guice.createInjector(
                new AbstractModule() {
                    @SuppressWarnings("unchecked")
                    @Override
                    protected void configure() {
                        SimpleScope simpleScope = new SimpleScope();

                        bindScope(CustomScope.class, simpleScope);
                        bind(SimpleScope.class).toInstance(simpleScope);

                        install(new DecoratorModule() {{
                            bind(Foo.class)
                                    .to(FooImpl.class)
                                    .decoratedBy(
                                            D2.class,
                                            D1.class)
                                    .in(CustomScope.class);
                        }});
                    }
                });

        SimpleScope scope = injector.getInstance(SimpleScope.class);
        scope.enter();
        final Foo instance1 = injector.getInstance(Foo.class);
        final Foo instance2 = injector.getInstance(Foo.class);
        scope.exit();

        assertThat(instance1, is(sameInstance(instance2)));
        assertThat(
                instance1.bar(),
                equalTo("D2:D1:FooImpl"));
    }

}
