# decorice
helper library to bind chain of decorators in guice

You have a chain of decorators to bind with `Guice`. `Guice` does not offer much syntactic sugar to help with that. `decorice` can help.

    interface Foo {
        String bar();
    }

    class D1 implements Foo {
        @Inject
        D1(@DecoratedBy(D1.class) Foo decorated /* ... */) {
            /* ... */
        }

        /* ... */
    }

    class D2 implements Foo {
        @Inject
        D2(@DecoratedBy(D2.class) Foo decorated /* ... */) {
            /* ... */
        }

        /* ... */
    }

    class FooImpl implements Foo {
        /* ... */
    }


With `decorice`, you bind the chain as:

    install(new DecoratorModule() {{
        bind(Foo.class).to(
                D2.class,
                D1.class,
                FooImpl.class);
    }});
    
The order of the classes on the call to the `to` method defines the order of decorations.

Binding annotations and scopes are supported:
    
    install(new DecoratorModule() {{
        bind(Foo.class)
                .annotatedWith(SomeAnnotation.class)
                .to(
                    D2.class,
                    D1.class,
                    FooImpl.class)
                .in(Singleton.class);
    }});
    
Compatible with `Guice` 3.0
