package toremove;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import decorice.DecoratedBy;
import decorice.DecoratorModule;
import org.junit.Ignore;
import org.junit.Test;

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static toremove.OtherAttempts.DecoratorModule_SS2.implement;

public class OtherAttempts {

    @BindingAnnotation
    @Target({ FIELD, PARAMETER, METHOD }) @Retention(RUNTIME)
    private @interface SomeAnnotation {
    }

    private static interface Foo {
        String bar();
    }

    public static class DecoratorModule_SS2 {
        public static interface DecoratorAwareLinkedBindingBuilder<T> {
            ScopedBindingBuilder with(Class<? extends T>... cs);
        }

        public static interface DecoratorAwareAnnotateBindingBuilder<T>
                extends DecoratorAwareLinkedBindingBuilder<T> {
            DecoratorAwareLinkedBindingBuilder<T> annotatedWith(Class<? extends Annotation> annotationType);
        }

        public static <T> DecoratorAwareAnnotateBindingBuilder<T> implement(Class<T> clazz) {
            return null;
        }

        public static com.google.inject.Module of(final ScopedBindingBuilder to) {
            return null;
        }

    }

    public static class DecoratorModuleBuilder {

        public static interface ScopedBindingBuilder {
            com.google.inject.Module build();
        }

        public static interface LinkedBindingBuilder<T> {
            ScopedBindingBuilder to(Class<? extends T>... classes);
        }

        public static interface AnnotatedBindingBuilder<T>
                extends LinkedBindingBuilder<T> {
            LinkedBindingBuilder<T> annotatedWith(Class<? extends Annotation> annotationType);
        }

        public <T> AnnotatedBindingBuilder<T> bind(final Class<T> clazz) {
            return null;
        }

    }

    static class DecoratorBindingUtils_SyntacticSugar_1 {
        public static interface DecoratorBindingBuilder<T> extends AnnotatedBindingBuilder<T> {
            ScopedBindingBuilder to(Class<? extends T>... cs);
        }

        public static class DecoratorBindingModule {
            private final AbstractModule abstractModule;

            public DecoratorBindingModule(final AbstractModule abstractModule) {
                this.abstractModule = abstractModule;
            }

            public <T> DecoratorBindingBuilder<T> bind(final Class<T> clazz) {
                return null;
            }
        }

        public static DecoratorBindingModule ex(AbstractModule m) {
            return new DecoratorBindingModule(m);
        }
    }

    private static class Decorated implements DecoratedBy {
        private final Class<?> value;

        private Decorated(final Class<?> value) {
            this.value = value;
        }

        public static DecoratedBy by(final Class<?> class__) {
            return new Decorated(class__);
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return DecoratedBy.class;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) return true;
            if (obj == null || !(obj instanceof DecoratedBy)) {
                return false;
            }
            final DecoratedBy rhs = (DecoratedBy) obj;
            return value().equals(rhs.value());
        }

        @Override
        public int hashCode() {
            return 127 * "value".hashCode() ^ value.hashCode();
        }

        @Override
        public Class<?> value() {
            return value;
        }
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

    private static class Module extends AbstractModule {
        @Override
        protected void configure() {
            bind(Foo.class)
                    .annotatedWith(Decorated.by(D1.class))
                    .to(FooImpl.class);
            bind(Foo.class)
                    .annotatedWith(Decorated.by(D2.class))
                    .to(D1.class);
            bind(Foo.class).to(D2.class);
        }
    }

    @Test
    public void appliesDecorators() {
        Foo foo = Guice.createInjector(new Module())
                .getInstance(Foo.class);
        assertThat(foo.bar(), equalTo("D2:D1:FooImpl"));
    }

    @Ignore
    @Test
    public void syntacticSugar_1() {

        /*
        could not get this style to work because I found no way to create an AnnotatedBindingBuilder
        to forward the existing methods to.
         */

        Foo foo = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        DecoratorBindingUtils_SyntacticSugar_1.ex(this).bind(Foo.class).to(
                                D2.class,
                                D1.class,
                                FooImpl.class);
                    }
                })
                .getInstance(Foo.class);
        assertThat(foo.bar(), equalTo("D2:D1:FooImpl"));
    }

    @Test
    public void syntacticSugar_2() {
        Foo foo = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        install(DecoratorModule_SS2.of(
                                implement(Foo.class)
                                        .annotatedWith(SomeAnnotation.class)
                                        .with(
                                                D2.class,
                                                D1.class,
                                                FooImpl.class)));
                        install(DecoratorModule_SS2.of(
                                implement(Foo.class)
                                        .with(
                                                D2.class,
                                                D1.class,
                                                FooImpl.class)));
                    }
                }).getInstance(Foo.class);
        foo.bar();
    }

    @Test
    public void syntacticSugar_3() {
        final Injector injector = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        install(new DecoratorModuleBuilder()
                                .bind(Foo.class).to(
                                        D2.class,
                                        D1.class,
                                        FooImpl.class)
                                .build());
                        install(new DecoratorModuleBuilder()
                                .bind(Foo.class)
                                .annotatedWith(SomeAnnotation.class)
                                .to(
                                        D2.class,
                                        D1.class,
                                        FooImpl.class)
                                .build());
                    }
                });
        assertThat(
                injector.getInstance(Foo.class).bar(),
                equalTo("D2:D1:FooImpl"));
        assertThat(
                injector.getInstance(Key.get(Foo.class, SomeAnnotation.class)).bar(),
                equalTo("D2:D1:FooImpl"));
    }

}
