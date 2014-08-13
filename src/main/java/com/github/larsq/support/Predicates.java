/*
 * The MIT License
 *
 * Copyright 2014 lars.
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
package com.github.larsq.support;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

/**
 *
 * @author lars
 */
public interface Predicates {
    static <T,S> Predicate<S> casted(Predicate<T> predicate) {
        return s-> predicate.test((T) s);
    }
    
    static <T, S> Predicate<T> compose(Predicate<S> predicate, Function<T, S> fn) {
        return t -> predicate.test(fn.apply(t));
    }

    static <T> Predicate<T> isNotNull() {
        return t -> t != null;
    }

    static <T> Predicate<Optional<T>> optional(Predicate<T> predicate, boolean ifEmpty) {
        return t -> (t.isPresent()) ? predicate.test(t.get()) : ifEmpty;
    }
    
    

    interface Iterables {

        static <T extends Iterable<?>> Predicate<T> isNonEmpty() {
            return t -> !com.google.common.collect.Iterables.isEmpty(t);
        }

        static <T extends Iterable<?>> Predicate<T> collectionOfSize(Predicate<Integer> predicate) {
            return t -> predicate.test(com.google.common.collect.Iterables.size(t));
        }
        
        static <T extends Iterable<?>> Predicate<T> allInstanceOf(Class<?> clz) {
            return iter->StreamSupport.stream(iter.spliterator(), false)
                    .allMatch(item->clz.isInstance(item));
        }
    }

    interface Entry {

        static <K, V> Predicate<Map.Entry<K, V>> key(Predicate<K> predicate) {
            return e -> predicate.test(e.getKey());
        }

        static <K, V> Predicate<Map.Entry<K, V>> value(Predicate<V> predicate) {
            return e -> predicate.test(e.getValue());
        }
    }

    interface String {

        static <STR extends CharSequence> Predicate<STR> matches(Pattern pattern) {
            return str -> pattern.matcher(str).matches();
        }
    }

    interface Object {
        static <T> Predicate<T> untyped(Predicate<Object> predicate) {
            return t-> predicate.test((Object) t);
        }
        
        static <T> Predicate<java.lang.Object> typed(Predicate<T> predicate) {
            return o -> predicate.test((T) o);
        }
        
        static <T> Predicate<Object> instanceOf(Class<?> target) {
            return target::isInstance;
        }
    }
}
