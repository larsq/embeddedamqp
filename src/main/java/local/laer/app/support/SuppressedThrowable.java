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

package local.laer.app.support;

import java.util.Map;
import java.util.function.Consumer;

/**
 *
 * @author Lars Eriksson (larsq.eriksson@gmail.com)
 * @param <X>
 */
public class SuppressedThrowable<X extends Throwable> implements Consumer<Throwable> {
    private final Class<X> checked;
    private X throwable;
    
    public SuppressedThrowable(Class<X> checked) {
        this.checked = checked;
    }

    @Override
    public void accept(Throwable t) {
        if(checked.isAssignableFrom(t.getClass())) {
            this.throwable = checked.cast(t);
        }
    }
    
    public void check() throws X {
        if(throwable == null) {
            return;
        }
        
        throw throwable;
    }

    public X getThrowable() {
        return throwable;
    }
    
    public static <X extends Throwable> SuppressedThrowable<X> wrap(Class<X> clz) {
        return new SuppressedThrowable<>(clz);
    }
}
