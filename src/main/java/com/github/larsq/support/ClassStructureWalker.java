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

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import java.util.*;
import java.util.function.Function;

/**
 * @author lars
 */
public class ClassStructureWalker {
	private final Class<?> startAt;
	private final boolean includeInterfaces;
	private final boolean includeSuperClasses;

	public ClassStructureWalker(Class<?> startAt, boolean includeInterfaces, boolean includeSuperClasses) {
		this.startAt = startAt;
		this.includeInterfaces = includeInterfaces;
		this.includeSuperClasses = includeSuperClasses;
	}

	public <T> Iterable<T> traverseClassStructure(Function<Class<?>, Iterable<T>> fn) {
		List<Iterable<T>> listOfIterables = new ArrayList<>();

		Set<Class<?>> set = buildClassSet(startAt);

		set.stream()
				.map(fn)
				.filter(Predicates.Iterables.isNonEmpty())
				.forEach(listOfIterables::add);

		return Iterables.concat(listOfIterables);
	}

	private Set<Class<?>> buildClassSet(Class<?> startAt) {
		Set<Class<?>> setOfClasses = new HashSet<>();

		Stack<Class<?>> unvisited = new Stack<>();
		unvisited.add(startAt);
		while (!unvisited.isEmpty()) {
			Class<?> current = unvisited.pop();

			setOfClasses.add(current);

			if (includeInterfaces) {
				addInterfaces(current).forEach(clz -> unvisited.push(clz));
			}

			if (includeSuperClasses) {
				Optional.ofNullable(current.getSuperclass())
						.ifPresent(unvisited::push);
			}
		}

		return setOfClasses;
	}

	private Set<Class<?>> addInterfaces(Class<?> clz) {
		return Sets.newHashSet(clz.getInterfaces());
	}
}
