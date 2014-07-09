/*
 * The MIT License
 *
 * Copyright 2014 Lars Eriksson (larsq.eriksson@gmail.com).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package local.laer.app.spring.embeddedamqp;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 *
 * @author Lars Eriksson (larsq.eriksson@gmail.com)
 */
public class ExceptionSuppliers {

    public final static ExceptionSuppliers Exception = new ExceptionSuppliers();

    private Supplier<IllegalStateException> alreadyDefined(String name) {
        return () -> new IllegalStateException(name);
    }

    private Supplier<NoSuchElementException> notDefined(String name) {
        return () -> new NoSuchElementException(name);
    }

    /**
     * Supplier for IOException when exchange is not defined
     *
     * @param name the name of the non-existent exchange
     * @return a supplier
     */
    public Supplier<IOException> exchangeNotFound(String name) {
        return () -> new IOException("exchange is not defined", notDefined(name).get());
    }

    public Supplier<IOException> routerNotFound(String name) {
        return () -> new IOException("router of specified kind is not found" + name, notDefined(name).get());
    }

    /**
     * Supplier for IOException when queue is not defined
     *
     * @param name the name of the non-existent queue
     * @return a supplier
     */
    public Supplier<IOException> queueNotFound(String name) {
        return () -> new IOException("queue is not defined", notDefined(name).get());
    }

    public Supplier<IOException> exchangeAlreadyExists(String name) {
        return () -> new IOException("exchange already defined", alreadyDefined(name).get());
    }

    public Supplier<IOException> tagAlreadyExists(String name) {
        return () -> new IOException("tag already defined", alreadyDefined(name).get());
    }
}
